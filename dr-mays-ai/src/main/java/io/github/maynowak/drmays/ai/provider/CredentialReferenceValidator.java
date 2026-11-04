package io.github.maynowak.drmays.ai.provider;

import java.util.Objects;

/** Validates credential-reference shape; it never reads environment or files. */
public final class CredentialReferenceValidator {

  /**
   * Validates a reference against provider requirements.
   *
   * @param reference logical reference, or null
   * @param descriptor provider descriptor
   * @return structural validation result
   */
  public CredentialValidationResult validate(
      ProviderCredentialReference reference, ModelProviderDescriptor descriptor) {
    Objects.requireNonNull(descriptor, "descriptor");
    if (!descriptor.requiresCredential()) {
      if (reference == null || reference.kind() == CredentialKind.NONE) {
        return new CredentialValidationResult(
            CredentialValidationStatus.NOT_REQUIRED, "provider does not require a credential");
      }
      return new CredentialValidationResult(
          CredentialValidationStatus.INVALID,
          "credential reference is not allowed for this provider");
    }
    if (reference == null) {
      return new CredentialValidationResult(
          CredentialValidationStatus.INVALID, "credential reference is required");
    }
    if (!reference.providerId().equals(descriptor.id())) {
      return new CredentialValidationResult(
          CredentialValidationStatus.INVALID, "credential reference belongs to another provider");
    }
    if (reference.kind() == CredentialKind.NONE) {
      return new CredentialValidationResult(
          CredentialValidationStatus.INVALID, "provider requires a non-empty credential reference");
    }
    return new CredentialValidationResult(
        CredentialValidationStatus.VALID, "credential reference is structurally valid");
  }
}
