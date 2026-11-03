package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.dna.ResourceEdge;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import java.util.List;
import java.util.Objects;

/**
 * Validated input for an AI explanation.
 *
 * @param findings deterministic findings; never invented by the model
 * @param relationships verified DNA edges
 * @param completeness discovery completeness
 */
public record IntelligenceRequest(
    List<DiagnosticFinding> findings,
    List<ResourceEdge> relationships,
    GraphCompleteness completeness) {

  public IntelligenceRequest {
    Objects.requireNonNull(findings, "findings");
    Objects.requireNonNull(relationships, "relationships");
    completeness = Objects.requireNonNull(completeness, "completeness");
    var copiedFindings =
        findings.stream().map(value -> Objects.requireNonNull(value, "finding")).toList();
    var copiedRelationships =
        relationships.stream().map(value -> Objects.requireNonNull(value, "relationship")).toList();
    findings = List.copyOf(copiedFindings);
    relationships = List.copyOf(copiedRelationships);
  }
}
