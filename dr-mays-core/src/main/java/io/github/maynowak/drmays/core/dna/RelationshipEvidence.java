package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.evidence.Evidence;
import java.util.Objects;

/**
 * Provenance of one verified resource relationship.
 *
 * @param evidence existing domain evidence for the source attribute
 */
public record RelationshipEvidence(Evidence evidence) {

  public RelationshipEvidence {
    evidence = Objects.requireNonNull(evidence, "evidence");
  }
}
