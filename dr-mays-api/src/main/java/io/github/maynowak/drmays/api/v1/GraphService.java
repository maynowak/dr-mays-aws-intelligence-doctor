package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.core.dna.GraphAnalyzer;
import io.github.maynowak.drmays.core.dna.InfrastructureGraphBuilder;
import org.springframework.stereotype.Service;

/** Maps the offline resource contract into the core DNA graph and back to versioned wire DTOs. */
@Service
public class GraphService {

  private final DiagnosisService diagnosisService;

  public GraphService(DiagnosisService diagnosisService) {
    this.diagnosisService = diagnosisService;
  }

  /**
   * Builds and analyzes a graph without performing any AWS or model call.
   *
   * @param request validated graph request
   * @return versioned graph response
   */
  public GraphResponse graph(GraphRequest request) {
    var resources = diagnosisService.toResources(new DiagnosisRequest(request.resources()));
    var result = new InfrastructureGraphBuilder().build(resources, request.partialDiscovery());
    var analyzer = new GraphAnalyzer(result.graph());
    var nodes = result.graph().nodes().stream().map(GraphDtoMapper::node).toList();
    var edges = result.graph().edges().stream().map(GraphDtoMapper::edge).toList();
    var issues =
        result.issues().stream()
            .map(issue -> new GraphIssueResponse(issue.code().name(), issue.message()))
            .toList();
    var stats =
        new GraphStatsResponse(
            result.graph().nodes().size(),
            result.graph().edges().size(),
            result.graph().inventoriedNodes().size(),
            result.graph().referencedNodes().size(),
            analyzer.components().size(),
            analyzer.hasCycles());
    return new GraphResponse(nodes, edges, issues, stats, result.completeness().name());
  }
}
