package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Privacy validation result in a preview response.
 *
 * @param valid whether the payload may be approved
 * @param violations safe violations
 */
public record ExpertisePrivacyValidationResponse(
    boolean valid, List<PrivacyViolationResponse> violations) {

  public ExpertisePrivacyValidationResponse {
    violations = List.copyOf(violations);
  }
}
