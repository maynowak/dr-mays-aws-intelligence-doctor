package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * One preview decision for a candidate provider field.
 *
 * @param field candidate field
 * @param classification sensitivity class
 * @param included whether the field is populated
 * @param reason safe explanation
 */
public record PrivacyFieldDecision(
    PrivacyField field, PrivacyClassification classification, boolean included, String reason) {

  public PrivacyFieldDecision {
    field = Objects.requireNonNull(field, "field");
    classification = Objects.requireNonNull(classification, "classification");
    reason = DomainChecks.requireText(reason, "reason");
  }
}
