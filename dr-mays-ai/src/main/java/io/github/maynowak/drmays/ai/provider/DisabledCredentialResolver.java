package io.github.maynowak.drmays.ai.provider;

import java.util.Objects;

/** Fail-closed resolver placeholder. It never reads environment, files, keychains or AWS state. */
public final class DisabledCredentialResolver {

  /**
   * Returns a safe decision without resolving a secret.
   *
   * @param reference logical reference
   * @return NOT_REQUIRED for NONE, otherwise DENIED
   */
  public CredentialAccessDecision resolve(ProviderCredentialReference reference) {
    var value = Objects.requireNonNull(reference, "reference");
    return value.kind() == CredentialKind.NONE
        ? CredentialAccessDecision.NOT_REQUIRED
        : CredentialAccessDecision.DENIED;
  }
}
