package io.github.maynowak.drmays.ai.privacy;

import java.util.List;
import java.util.Objects;

/**
 * Result of validating a sanitized provider payload.
 *
 * @param valid true only when no violations exist
 * @param violations safe validation issues
 */
public record PrivacyValidationResult(boolean valid, List<PrivacyViolation> violations) {

  public PrivacyValidationResult {
    violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
    valid = valid && violations.isEmpty();
  }

  /**
   * Returns a passing result.
   *
   * @return valid result
   */
  public static PrivacyValidationResult ok() {
    return new PrivacyValidationResult(true, List.of());
  }

  /**
   * Returns a failing result.
   *
   * @param violations validation issues
   * @return invalid result
   */
  public static PrivacyValidationResult invalid(List<PrivacyViolation> violations) {
    return new PrivacyValidationResult(false, violations);
  }

  /**
   * Combines this result with another, preserving all violations.
   *
   * @param other additional result
   * @return combined result
   */
  public PrivacyValidationResult merge(PrivacyValidationResult other) {
    Objects.requireNonNull(other, "other");
    if (other.valid()) {
      return this;
    }
    if (valid()) {
      return other;
    }
    var combined = new java.util.ArrayList<>(violations);
    combined.addAll(other.violations());
    return invalid(combined);
  }
}
