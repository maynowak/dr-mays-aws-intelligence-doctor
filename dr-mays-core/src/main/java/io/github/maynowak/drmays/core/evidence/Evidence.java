package io.github.maynowak.drmays.core.evidence;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.validation.DomainChecks;

import java.util.Map;
import java.util.Objects;

/**
 * Traceable technical evidence supporting a diagnostic finding.
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
