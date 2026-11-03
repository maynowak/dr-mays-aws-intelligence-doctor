package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Map;
import java.util.Objects;

/**
 * A graph node representing an inventoried or referenced resource.
 *
 * @param identity stable account/region-aware identity
 * @param status inventoried or referenced
 * @param tags immutable resource tags (empty for referenced nodes)
 * @param attributes immutable resource attributes (empty for referenced nodes)
 */
public record ResourceNode(
    ResourceIdentity identity,
    ResourceResolutionStatus status,
    Map<String, String> tags,
    Map<String, String> attributes) {

  public ResourceNode {
    identity = Objects.requireNonNull(identity, "identity");
    status = Objects.requireNonNull(status, "status");
    tags = DomainChecks.immutableTextMap(tags, "tags");
    attributes = DomainChecks.immutableTextMap(attributes, "attributes");
  }

  public boolean inventoried() {
    return status == ResourceResolutionStatus.INVENTORIED;
  }
}
