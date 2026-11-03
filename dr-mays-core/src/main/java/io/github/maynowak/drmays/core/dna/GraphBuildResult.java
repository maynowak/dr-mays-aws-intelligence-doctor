package io.github.maynowak.drmays.core.dna;

import java.util.List;
import java.util.Objects;

/**
 * Result of building a graph, including completeness and validation issues.
 *
 * @param graph built graph
 * @param issues non-fatal issues encountered while building
 * @param completeness completeness of the underlying resource data
 */
public record GraphBuildResult(
    InfrastructureGraph graph, List<GraphValidationIssue> issues, GraphCompleteness completeness) {

  public GraphBuildResult {
    graph = Objects.requireNonNull(graph, "graph");
    issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    completeness = Objects.requireNonNull(completeness, "completeness");
  }
}
