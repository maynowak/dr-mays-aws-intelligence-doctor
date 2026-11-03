package io.github.maynowak.drmays.core.evidence;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Map;
import java.util.Objects;

/**
 * Traceable technical evidence supporting a diagnostic finding.
 *
 * @param resource the resource the evidence belongs to
 * @param source the technical source of the evidence
 * @param observed the value actually observed
 * @param expected the value expected by the rule
 * @param details additional structured evidence attributes
 */
public record Evidence(
    ResourceIdentity resource,
    String source,
    String observed,
    String expected,
    Map<String, String> details) {

  public Evidence {
    resource = Objects.requireNonNull(resource, "resource");
    source = DomainChecks.requireText(source, "source");
    observed = DomainChecks.requireText(observed, "observed");
    expected = DomainChecks.requireText(expected, "expected");
    details = DomainChecks.immutableTextMap(details, "details");
  }
}
