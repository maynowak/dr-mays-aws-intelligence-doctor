package io.github.maynowak.drmays.ai.provider;

/** Explicit provider invocation status. */
public enum ProviderExecutionStatus {
  SUCCESS,
  DISABLED,
  UNAVAILABLE,
  FAILED,
  INVALID_RESPONSE,
  APPROVAL_REQUIRED,
  PRIVACY_REJECTED,
  NOT_ALLOWED,
  CREDENTIAL_UNAVAILABLE,
  TIMEOUT
}
