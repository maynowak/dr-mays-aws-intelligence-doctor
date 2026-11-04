package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Safe server-side preview of the exact provider payload.
 *
 * @param previewId opaque server-side handle
 * @param payloadHash exact serialized payload hash
 * @param payloadJson exact payload shown to the approver
 * @param policyVersion privacy policy version
 * @param providerId selected provider
 * @param modelId selected model
 * @param expertiseId selected expertise profile
 * @param approvalRequired whether execution needs an approval token
 * @param expiresAt preview expiry
 * @param validation privacy validation outcome
 * @param decisions field decisions
 */
public record PrivacyPreview(
    String previewId,
    String payloadHash,
    String payloadJson,
    String policyVersion,
    String providerId,
    String modelId,
    String expertiseId,
    boolean approvalRequired,
    Instant expiresAt,
    PrivacyValidationResult validation,
    List<PrivacyFieldDecision> decisions) {

  public PrivacyPreview {
    previewId = DomainChecks.requireText(previewId, "previewId");
    payloadHash = DomainChecks.requireText(payloadHash, "payloadHash");
    payloadJson = DomainChecks.requireText(payloadJson, "payloadJson");
    policyVersion = DomainChecks.requireText(policyVersion, "policyVersion");
    providerId = DomainChecks.requireText(providerId, "providerId");
    modelId = DomainChecks.requireText(modelId, "modelId");
    expertiseId = DomainChecks.requireText(expertiseId, "expertiseId");
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    validation = Objects.requireNonNull(validation, "validation");
    decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions"));
  }
}
