package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.Objects;

/**
 * A directed, evidence-backed relationship between two resource identities.
 *
 * @param from source identity
 * @param to target identity
 * @param type verified relationship type
 * @param evidence provenance from the source resource
 */
public record ResourceEdge(
    ResourceIdentity from,
    ResourceIdentity to,
    RelationshipType type,
    RelationshipEvidence evidence) {

  public ResourceEdge {
    from = Objects.requireNonNull(from, "from");
    to = Objects.requireNonNull(to, "to");
    type = Objects.requireNonNull(type, "type");
    evidence = Objects.requireNonNull(evidence, "evidence");
    if (!evidence.evidence().resource().equals(from)) {
      throw new IllegalArgumentException("evidence resource must match edge source");
    }
  }
}
