package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Server-side preview and approval store. It accepts only server-generated previews, binds
 * approvals to the exact payload/provider/model/expertise tuple and consumes a successful
 * invocation once.
 */
public final class PrivacyGate {

  private static final int MAX_PENDING = 100;
  private static final int MAX_APPROVALS = 100;
  private static final int MAX_CONSUMED = 1_000;
  private static final int PREVIEW_ID_BYTES = 16;
  private static final int TOKEN_BYTES = 32;
  private static final int AUTHORIZATION_NONCE_BYTES = 16;
  private static final Duration AUTHORIZATION_TTL = Duration.ofSeconds(30);

  private final Duration previewTtl;
  private final Duration approvalTtl;
  private final Clock clock;
  private final ApprovalRequirement approvalRequirement;
  private final SecureRandom random;
  private final Map<String, Pending> pending = new HashMap<>();
  private final Map<String, ApprovalRecord> approvals = new HashMap<>();
  private final Set<String> consumed = new LinkedHashSet<>();

  /** Creates a strict gate with five-minute lifetimes. */
  public PrivacyGate() {
    this(Duration.ofMinutes(5), Duration.ofMinutes(5), Clock.systemUTC(), providerId -> true);
  }

  /**
   * Creates a gate with explicit lifetime and clock dependencies.
   *
   * @param previewTtl preview lifetime
   * @param approvalTtl approval lifetime
   * @param clock clock used for expiry
   */
  public PrivacyGate(Duration previewTtl, Duration approvalTtl, Clock clock) {
    this(previewTtl, approvalTtl, clock, providerId -> true);
  }

  /**
   * Creates a gate with a server-owned approval requirement.
   *
   * @param previewTtl preview lifetime
   * @param approvalTtl approval lifetime
   * @param clock clock used for expiry
   * @param approvalRequirement trusted approval policy
   */
  public PrivacyGate(
      Duration previewTtl,
      Duration approvalTtl,
      Clock clock,
      ApprovalRequirement approvalRequirement) {
    this.previewTtl = requirePositive(previewTtl, "previewTtl");
    this.approvalTtl = requirePositive(approvalTtl, "approvalTtl");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.approvalRequirement = Objects.requireNonNull(approvalRequirement, "approvalRequirement");
    this.random = new SecureRandom();
  }

  /**
   * Registers a sanitized preview. No provider call occurs.
   *
   * @param source original verified request
   * @param processed sanitized projection
   * @param providerId selected provider
   * @param modelId selected model
   * @param expertiseId selected expertise profile
   * @return safe preview
   */
  public synchronized PrivacyPreview register(
      IntelligenceRequest source,
      PrivacyProcessingResult processed,
      String providerId,
      String modelId,
      String expertiseId) {
    prune();
    if (pending.size() >= MAX_PENDING) {
      throw new IllegalStateException("approval store is full");
    }
    var previewId = randomToken(PREVIEW_ID_BYTES);
    var expiresAt = clock.instant().plus(previewTtl);
    var approvalRequired = approvalRequirement.requiresApproval(providerId);
    pending.put(
        previewId,
        new Pending(
            source,
            processed,
            DomainChecks.requireText(providerId, "providerId"),
            DomainChecks.requireText(modelId, "modelId"),
            DomainChecks.requireText(expertiseId, "expertiseId"),
            approvalRequired,
            expiresAt));
    return new PrivacyPreview(
        previewId,
        processed.payloadHash(),
        processed.payloadJson(),
        processed.policy().version(),
        providerId,
        modelId,
        expertiseId,
        approvalRequired,
        expiresAt,
        processed.validation(),
        processed.decisions());
  }

  /**
   * Approves an existing preview and returns an opaque single-use token.
   *
   * @param previewId preview handle
   * @return token when the preview exists and has not expired
   */
  public synchronized Optional<ApprovalToken> approve(String previewId) {
    prune();
    var preview = pending.get(DomainChecks.requireText(previewId, "previewId"));
    if (preview == null || approvals.size() >= MAX_APPROVALS) {
      return Optional.empty();
    }
    var now = clock.instant();
    if (!now.isBefore(preview.expiresAt())) {
      pending.remove(previewId);
      return Optional.empty();
    }
    var token = randomToken(TOKEN_BYTES);
    var expiresAt = earlier(now.plus(approvalTtl), preview.expiresAt());
    approvals.put(
        token,
        new ApprovalRecord(
            previewId,
            preview.processed().payloadHash(),
            preview.providerId(),
            preview.modelId(),
            preview.expertiseId(),
            preview.processed().policy().version(),
            expiresAt));
    return Optional.of(new ApprovalToken(token));
  }

  /**
   * Consumes an approval for the exact preview tuple.
   *
   * @param previewId preview handle
   * @param token approval token; null for providers that do not require approval
   * @return approval decision and sealed invocation material
   */
  public synchronized ApprovalDecision consume(String previewId, ApprovalToken token) {
    var id = DomainChecks.requireText(previewId, "previewId");
    if (consumed.contains(id)) {
      return ApprovalDecision.rejected(ApprovalStatus.CONSUMED);
    }
    var preview = pending.get(id);
    if (preview == null) {
      return ApprovalDecision.rejected(ApprovalStatus.NOT_FOUND);
    }
    var now = clock.instant();
    if (!now.isBefore(preview.expiresAt())) {
      pending.remove(id);
      return ApprovalDecision.rejected(ApprovalStatus.EXPIRED);
    }
    if (!preview.processed().validation().valid()) {
      pending.remove(id);
      return ApprovalDecision.rejected(ApprovalStatus.PRIVACY_REJECTED);
    }
    if (preview.approvalRequired()) {
      if (token == null) {
        return ApprovalDecision.rejected(ApprovalStatus.APPROVAL_REQUIRED);
      }
      var record = approvals.get(token.value());
      if (record == null || !matches(record, preview, id)) {
        return ApprovalDecision.rejected(ApprovalStatus.MISMATCH);
      }
      if (!now.isBefore(record.expiresAt())) {
        approvals.remove(token.value());
        pending.remove(id);
        return ApprovalDecision.rejected(ApprovalStatus.EXPIRED);
      }
      approvals.remove(token.value());
    }
    pending.remove(id);
    consumed.add(id);
    if (consumed.size() > MAX_CONSUMED) {
      consumed.remove(consumed.iterator().next());
    }
    var authorization =
        new ProviderInvocationAuthorization(
            randomToken(AUTHORIZATION_NONCE_BYTES),
            preview.processed().payloadHash(),
            preview.providerId(),
            preview.modelId(),
            preview.expertiseId(),
            preview.processed().policy().version(),
            preview.approvalRequired(),
            earlier(clock.instant().plus(AUTHORIZATION_TTL), preview.expiresAt()));
    return ApprovalDecision.approved(
        new ApprovedInvocation(
            preview.source(),
            preview.processed(),
            preview.providerId(),
            preview.modelId(),
            preview.expertiseId(),
            preview.approvalRequired(),
            authorization));
  }

  /** Removes expired state. */
  public synchronized void prune() {
    var now = clock.instant();
    pending.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
    approvals.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
  }

  /** Returns the number of pending previews. */
  public synchronized int pendingCount() {
    return pending.size();
  }

  private static boolean matches(ApprovalRecord record, Pending preview, String previewId) {
    return record.previewId().equals(previewId)
        && record.payloadHash().equals(preview.processed().payloadHash())
        && record.providerId().equals(preview.providerId())
        && record.modelId().equals(preview.modelId())
        && record.expertiseId().equals(preview.expertiseId())
        && record.policyVersion().equals(preview.processed().policy().version());
  }

  private String randomToken(int bytes) {
    var value = new byte[bytes];
    random.nextBytes(value);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private static Instant earlier(Instant left, Instant right) {
    return left.isBefore(right) ? left : right;
  }

  private static Duration requirePositive(Duration value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
  }

  private record Pending(
      IntelligenceRequest source,
      PrivacyProcessingResult processed,
      String providerId,
      String modelId,
      String expertiseId,
      boolean approvalRequired,
      Instant expiresAt) {}

  private record ApprovalRecord(
      String previewId,
      String payloadHash,
      String providerId,
      String modelId,
      String expertiseId,
      String policyVersion,
      Instant expiresAt) {}
}
