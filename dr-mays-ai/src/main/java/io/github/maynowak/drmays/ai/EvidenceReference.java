package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.RuleId;
import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * Reference to evidence already present in the request.
 *
 * @param ruleId finding rule
 * @param resource finding resource
 * @param source evidence source
 */
public record EvidenceReference(RuleId ruleId, ResourceIdentity resource, String source) {

  public EvidenceReference {
    ruleId = java.util.Objects.requireNonNull(ruleId, "ruleId");
    resource = java.util.Objects.requireNonNull(resource, "resource");
    source = DomainChecks.requireText(source, "source");
  }
}
