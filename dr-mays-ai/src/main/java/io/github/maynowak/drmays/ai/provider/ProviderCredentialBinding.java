package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Immutable credential binding metadata. It never contains a secret and is not a live credential
 * resolution result.
 *
 * @param bindingId server-assigned binding identifier
 * @param providerId provider identifier
 * @param reference logical reference label
 * @param version monotonic credential version
 * @param trustedFingerprint synthetic/trusted fingerprint placeholder; blank for none
 */
public record ProviderCredentialBinding(
    String bindingId,
    String providerId,
    String reference,
    long version,
    String trustedFingerprint) {

  public ProviderCredentialBinding {
    bindingId = DomainChecks.requireText(bindingId, "bindingId");
    providerId = DomainChecks.requireText(providerId, "providerId");
    if (!providerId.matches("[a-z][a-z0-9-]{1,31}")) {
      throw new IllegalArgumentException("providerId is invalid");
    }
    reference = DomainChecks.requireText(reference, "reference");
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
    trustedFingerprint = Objects.requireNonNull(trustedFingerprint, "trustedFingerprint");
    if (!trustedFingerprint.isBlank() && !trustedFingerprint.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException(
          "trustedFingerprint must be a lowercase SHA-256 hex value");
    }
  }

  /**
   * Returns the no-credential binding used by the mock-only phase.
   *
   * @param providerId provider identifier
   * @return synthetic none binding
   */
  public static ProviderCredentialBinding none(String providerId) {
    return new ProviderCredentialBinding("credential-none", providerId, "none", 0, "");
  }

  /**
   * Returns whether the binding matches a logical reference.
   *
   * @param reference logical reference
   * @return true when provider and reference match
   */
  public boolean matches(ProviderCredentialReference reference) {
    return providerId.equals(reference.providerId())
        && this.reference.equals(reference.reference());
  }

  @Override
  public String toString() {
    return "ProviderCredentialBinding[bindingId="
        + bindingId
        + ", providerId="
        + providerId
        + ", reference=redacted, version="
        + version
        + ", fingerprint=redacted]";
  }
}
