package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Wire graph section for a Doctor Report.
 *
 * @param nodes graph nodes
 * @param edges verified relationships
 * @param issues non-fatal graph issues
 * @param stats deterministic graph statistics
 * @param completeness COMPLETE, PARTIAL or UNKNOWN
 */
public record DoctorReportGraphResponse(
    List<DoctorReportNodeResponse> nodes,
    List<DoctorReportEdgeResponse> edges,
    List<GraphIssueResponse> issues,
    GraphStatsResponse stats,
    String completeness) {

  public DoctorReportGraphResponse {
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
    issues = List.copyOf(issues);
  }
}
