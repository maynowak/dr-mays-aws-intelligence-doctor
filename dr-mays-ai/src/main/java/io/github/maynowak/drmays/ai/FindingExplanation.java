package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.RuleId;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.List;
import java.util.Objects;

/**
 * Explanation of one existing deterministic finding.
 *
 * @param ruleId finding rule
 * @param resource finding resource
 * @param explanation human-readable explanation
 * @param evidence references to existing evidence
 */
public record FindingExplanation(
    RuleId ruleId,
    ResourceIdentity resource,
    String explanation,
    List<EvidenceReference> evidence) {

  public FindingExplanation {
    ruleId = Objects.requireNonNull(ruleId, "ruleId");
    resource = Objects.requireNonNull(resource, "resource");
    explanation = DomainChecks.requireText(explanation, "explanation");
    evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
    if (evidence.isEmpty()) {
      throw new IllegalArgumentException("evidence must not be empty");
    }
  }
}
