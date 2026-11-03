package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Versioned graph response.
 *
 * @param nodes graph nodes
 * @param edges directed evidence-backed edges
 * @param issues graph construction issues
 * @param stats summary statistics
 * @param completeness COMPLETE, PARTIAL or UNKNOWN
 */
public record GraphResponse(
    List<GraphNodeResponse> nodes,
    List<GraphEdgeResponse> edges,
    List<GraphIssueResponse> issues,
    GraphStatsResponse stats,
    String completeness) {

  public GraphResponse {
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
    issues = List.copyOf(issues);
  }
}
