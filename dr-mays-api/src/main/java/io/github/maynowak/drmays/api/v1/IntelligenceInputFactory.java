package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.dna.InfrastructureGraphBuilder;
import java.util.List;
import org.springframework.stereotype.Service;

/** Single server-side construction path for verified intelligence input. */
@Service
public class IntelligenceInputFactory {

  private final DiagnosisService diagnosisService;

  public IntelligenceInputFactory(DiagnosisService diagnosisService) {
    this.diagnosisService = diagnosisService;
  }

  /**
   * Builds verified findings, DNA edges and completeness from synthetic resources.
   *
   * @param resources validated wire resources
   * @param partialDiscovery whether discovery was incomplete
   * @return verified intelligence request
   */
  public IntelligenceRequest build(List<ResourceRequest> resources, boolean partialDiscovery) {
    var diagnosisRequest = new DiagnosisRequest(resources);
    var domainResources = diagnosisService.toResources(diagnosisRequest);
    var diagnosis = diagnosisService.diagnoseDomain(diagnosisRequest);
    var graph = new InfrastructureGraphBuilder().build(domainResources, partialDiscovery).graph();
    return new IntelligenceRequest(
        diagnosis.findings(),
        graph.edges(),
        partialDiscovery ? GraphCompleteness.PARTIAL : GraphCompleteness.COMPLETE);
  }
}
