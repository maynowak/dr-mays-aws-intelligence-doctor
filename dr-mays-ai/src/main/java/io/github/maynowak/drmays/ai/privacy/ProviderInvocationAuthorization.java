package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.time.Instant;
import java.util.Objects;

/**
 * Server-minted proof that a specific sanitized invocation was approved. The constructor is
 * package-private so only the privacy gate can mint it; REST DTOs never carry it.
 */
public final class ProviderInvocationAuthorization {

  private final String nonce;
  private final String payloadHash;
  private final String providerId;
  private final String modelId;
  private final String expertiseId;
  private final String policyVersion;
  private final Instant expiresAt;

  ProviderInvocationAuthorization(
      String nonce,
      String payloadHash,
      String providerId,
      String modelId,
      String expertiseId,
      String policyVersion,
      Instant expiresAt) {
    this.nonce = DomainChecks.requireText(nonce, "nonce");
    this.payloadHash = DomainChecks.requireText(payloadHash, "payloadHash");
    this.providerId = DomainChecks.requireText(providerId, "providerId");
    this.modelId = DomainChecks.requireText(modelId, "modelId");
    this.expertiseId = DomainChecks.requireText(expertiseId, "expertiseId");
    this.policyVersion = DomainChecks.requireText(policyVersion, "policyVersion");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
  }

  public String nonce() {
    return nonce;
  }

  public String payloadHash() {
    return payloadHash;
  }

  public String providerId() {
    return providerId;
  }

  public String modelId() {
    return modelId;
  }

  public String expertiseId() {
    return expertiseId;
  }

  public String policyVersion() {
    return policyVersion;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  /**
   * Returns whether this authorization is expired at the supplied instant.
   *
   * @param now current instant
   * @return true when the authorization may no longer be used
   */
  public boolean expired(Instant now) {
    return !Objects.requireNonNull(now, "now").isBefore(expiresAt);
  }

  @Override
  public String toString() {
    return "ProviderInvocationAuthorization[redacted]";
  }
}
