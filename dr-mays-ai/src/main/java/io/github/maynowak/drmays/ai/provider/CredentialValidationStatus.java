package io.github.maynowak.drmays.ai.provider;

/** Result of structurally validating a credential reference. */
public enum CredentialValidationStatus {
  VALID,
  NOT_REQUIRED,
  INVALID,
  UNRESOLVED
}
