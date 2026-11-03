package io.github.maynowak.drmays.core.finding;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.RuleId;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * A deterministic diagnostic finding with traceable evidence.
 *
 * @param ruleId the rule that produced the finding
 * @param ruleVersion the rule version
 * @param severity the finding severity
 * @param resource the affected resource
 * @param title a short human-readable title
 * @param description a factual description of the finding
 * @param evidence traceable technical evidence
 * @param recommendation the recommended remediation
 */
public record DiagnosticFinding(
    RuleId ruleId,
    String ruleVersion,
    Severity severity,
    ResourceIdentity resource,
    String title,
    String description,
    Evidence evidence,
    String recommendation) {

  public DiagnosticFinding {
    ruleId = Objects.requireNonNull(ruleId, "ruleId");
    ruleVersion = DomainChecks.requireText(ruleVersion, "ruleVersion");
    severity = Objects.requireNonNull(severity, "severity");
    resource = Objects.requireNonNull(resource, "resource");
    title = DomainChecks.requireText(title, "title");
    description = DomainChecks.requireText(description, "description");
    evidence = Objects.requireNonNull(evidence, "evidence");
    recommendation = DomainChecks.requireText(recommendation, "recommendation");

    if (!evidence.resource().equals(resource)) {
      throw new IllegalArgumentException("evidence resource must match finding resource");
    }
  }
}
