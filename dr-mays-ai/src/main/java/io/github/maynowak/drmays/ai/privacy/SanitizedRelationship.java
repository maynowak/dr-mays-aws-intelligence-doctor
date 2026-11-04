package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * Provider-facing verified relationship using resource aliases only.
 *
 * @param type verified relationship type
 * @param fromAlias source resource alias
 * @param toAlias target resource alias
 * @param evidenceSource relationship evidence source
 * @param observed sanitized relationship evidence
 */
public record SanitizedRelationship(
    String type, String fromAlias, String toAlias, String evidenceSource, String observed) {

  public SanitizedRelationship {
    type = DomainChecks.requireText(type, "type");
    fromAlias = DomainChecks.requireText(fromAlias, "fromAlias");
    toAlias = DomainChecks.requireText(toAlias, "toAlias");
    evidenceSource = DomainChecks.requireText(evidenceSource, "evidenceSource");
    observed = DomainChecks.requireText(observed, "observed");
  }
}
