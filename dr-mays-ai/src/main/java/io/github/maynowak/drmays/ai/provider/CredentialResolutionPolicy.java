package io.github.maynowak.drmays.ai.provider;

import java.util.Objects;
import java.util.Set;

/**
 * Credential-resolution policy. The current default is disabled and no resolver reads secrets.
 *
 * @param enabled whether a future trusted resolver may be used
 * @param allowedKinds credential kinds permitted by that future resolver
 */
public record CredentialResolutionPolicy(boolean enabled, Set<CredentialKind> allowedKinds) {

  public CredentialResolutionPolicy {
    allowedKinds = Set.copyOf(Objects.requireNonNull(allowedKinds, "allowedKinds"));
    if (!enabled && !allowedKinds.isEmpty()) {
      throw new IllegalArgumentException("disabled policy must not allow credential kinds");
    }
  }

  /** Returns the fail-closed default. */
  public static CredentialResolutionPolicy disabled() {
    return new CredentialResolutionPolicy(false, Set.of());
  }

  /**
   * Returns whether a kind is allowed by this policy.
   *
   * @param kind credential kind
   * @return true only when resolution is enabled and the kind is explicitly allowed
   */
  public boolean allows(CredentialKind kind) {
    return enabled && allowedKinds.contains(Objects.requireNonNull(kind, "kind"));
  }
}
