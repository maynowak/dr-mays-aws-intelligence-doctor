package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Logical pointer to a credential. It never contains the secret itself.
 *
 * @param providerId provider the reference belongs to
 * @param kind reference kind
 * @param reference environment variable name, file label or keyring label
 */
public record ProviderCredentialReference(
    String providerId, CredentialKind kind, String reference) {

  public ProviderCredentialReference {
    providerId = DomainChecks.requireText(providerId, "providerId");
    if (!providerId.matches("[a-z][a-z0-9-]{1,31}")) {
      throw new IllegalArgumentException("providerId is invalid");
    }
    kind = Objects.requireNonNull(kind, "kind");
    reference = DomainChecks.requireText(reference, "reference");
    if (kind == CredentialKind.ENV && !reference.matches("DR_MAYS_[A-Z0-9_]{2,64}")) {
      throw new IllegalArgumentException("environment references must use the DR_MAYS_ prefix");
    }
  }

  /**
   * Creates a no-credential reference.
   *
   * @param providerId provider identifier
   * @return no-credential reference
   */
  public static ProviderCredentialReference none(String providerId) {
    return new ProviderCredentialReference(providerId, CredentialKind.NONE, "none");
  }

  /**
   * Creates an explicit environment reference without reading the environment.
   *
   * @param providerId provider identifier
   * @param name allowlisted variable name
   * @return logical reference
   */
  public static ProviderCredentialReference environment(String providerId, String name) {
    return new ProviderCredentialReference(providerId, CredentialKind.ENV, name);
  }
}
