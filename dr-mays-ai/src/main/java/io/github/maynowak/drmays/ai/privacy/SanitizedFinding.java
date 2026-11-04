package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Provider-facing finding with identifiers replaced by aliases.
 *
 * @param ruleId deterministic rule identifier
 * @param ruleVersion rule version
 * @param severity finding severity name
 * @param resourceAlias local pseudonym for the finding resource
 * @param resourceType coarse resource type
 * @param regionClass coarse region classification
 * @param title sanitized finding title
 * @param description sanitized finding description
 * @param evidenceSource evidence source
 * @param observed sanitized observed value
 * @param expected sanitized expected value
 */
public record SanitizedFinding(
    String ruleId,
    String ruleVersion,
    String severity,
    String resourceAlias,
    String resourceType,
    String regionClass,
    String title,
    String description,
    String evidenceSource,
    String observed,
    String expected) {

  public SanitizedFinding {
    ruleId = DomainChecks.requireText(ruleId, "ruleId");
    ruleVersion = DomainChecks.requireText(ruleVersion, "ruleVersion");
    severity = DomainChecks.requireText(severity, "severity");
    resourceAlias = DomainChecks.requireText(resourceAlias, "resourceAlias");
    resourceType = DomainChecks.requireText(resourceType, "resourceType");
    regionClass = DomainChecks.requireText(regionClass, "regionClass");
    title = DomainChecks.requireText(title, "title");
    description = DomainChecks.requireText(description, "description");
    evidenceSource = DomainChecks.requireText(evidenceSource, "evidenceSource");
    observed = DomainChecks.requireText(observed, "observed");
    expected = DomainChecks.requireText(expected, "expected");
  }

  /**
   * Replaces the sanitized title when the field is excluded by policy.
   *
   * @param value replacement text
   * @return copy with replacement title
   */
  public SanitizedFinding withTitle(String value) {
    return new SanitizedFinding(
        ruleId,
        ruleVersion,
        severity,
        resourceAlias,
        resourceType,
        regionClass,
        Objects.requireNonNull(value, "value"),
        description,
        evidenceSource,
        observed,
        expected);
  }
}
