package io.github.maynowak.drmays.core.report;

import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.dna.GraphValidationIssue;
import io.github.maynowak.drmays.core.dna.InfrastructureGraph;
import java.util.List;
import java.util.Objects;

/**
 * Verified graph projection and its deterministic summary statistics.
 *
 * @param graph verified infrastructure graph
 * @param issues non-fatal graph construction issues
 * @param completeness completeness declared by the source snapshot
 * @param componentCount undirected connected components
 * @param cyclic whether the graph contains a directed cycle
 */
public record DoctorReportGraph(
    InfrastructureGraph graph,
    List<GraphValidationIssue> issues,
    GraphCompleteness completeness,
    int componentCount,
    boolean cyclic) {

  public DoctorReportGraph {
    graph = Objects.requireNonNull(graph, "graph");
    issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    completeness = Objects.requireNonNull(completeness, "completeness");
    if (componentCount < 0) {
      throw new IllegalArgumentException("componentCount must not be negative");
    }
  }

  public int nodeCount() {
    return graph.nodes().size();
  }

  public int edgeCount() {
    return graph.edges().size();
  }

  public int inventoriedCount() {
    return graph.inventoriedNodes().size();
  }

  public int referencedCount() {
    return graph.referencedNodes().size();
  }
}
