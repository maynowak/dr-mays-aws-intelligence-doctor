package io.github.maynowak.drmays.ai.provider;

/** Result of a credential-resolution request; never contains secret material. */
public enum CredentialAccessDecision {
  NOT_REQUIRED,
  DENIED,
  RESOLVED
}
