package io.github.maynowak.drmays.ai.provider;

import java.util.Objects;

/**
 * Structural credential-reference validation without secret resolution.
 *
 * @param status validation result
 * @param reason safe explanation without secret material
 */
public record CredentialValidationResult(CredentialValidationStatus status, String reason) {

  public CredentialValidationResult {
    status = Objects.requireNonNull(status, "status");
    reason = Objects.requireNonNull(reason, "reason");
  }

  /**
   * Returns whether the reference can be used for this phase.
   *
   * @return true for valid or not-required references
   */
  public boolean allowed() {
    return status == CredentialValidationStatus.VALID
        || status == CredentialValidationStatus.NOT_REQUIRED;
  }
}
