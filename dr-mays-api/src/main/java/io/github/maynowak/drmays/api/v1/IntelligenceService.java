package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.ai.EvidenceReference;
import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.IntelligenceAnalyzer;
import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.ai.IntelligenceResult;
import io.github.maynowak.drmays.ai.Recommendation;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.dna.InfrastructureGraphBuilder;
import org.springframework.stereotype.Service;

/** Builds verified intelligence input server-side and calls the configured analyzer. */
@Service
public class IntelligenceService {

  private final DiagnosisService diagnosisService;
  private final IntelligenceAnalyzer analyzer;

  public IntelligenceService(DiagnosisService diagnosisService, IntelligenceAnalyzer analyzer) {
    this.diagnosisService = diagnosisService;
    this.analyzer = analyzer;
  }

  /**
   * Runs deterministic diagnosis/DNA extraction and explains the result.
   *
   * @param request synthetic resource request
   * @return structured AI response
   */
  public IntelligenceExplainResponse explain(IntelligenceExplainRequest request) {
    var diagnosisRequest = new DiagnosisRequest(request.resources());
    var resources = diagnosisService.toResources(diagnosisRequest);
    var diagnosis = diagnosisService.diagnoseDomain(diagnosisRequest);
    var graph =
        new InfrastructureGraphBuilder()
            .build(resources, Boolean.TRUE.equals(request.partialDiscovery()))
            .graph();
    var intelligenceRequest =
        new IntelligenceRequest(
            diagnosis.findings(),
            graph.edges(),
            Boolean.TRUE.equals(request.partialDiscovery())
                ? GraphCompleteness.PARTIAL
                : GraphCompleteness.COMPLETE);
    var result = analyzer.analyze(intelligenceRequest);
    return toResponse(result);
  }

  private IntelligenceExplainResponse toResponse(IntelligenceResult result) {
    return new IntelligenceExplainResponse(
        result.status().name(),
        new IntelligenceMetadataResponse(
            result.metadata().provider(), result.metadata().model(), result.metadata().version()),
        result.summary(),
        result.explanations().stream().map(this::toExplanation).toList(),
        result.recommendations().stream().map(this::toRecommendation).toList(),
        result.evidenceReferences().stream().map(this::toEvidenceReference).toList(),
        result.verifiedRelationships().stream().map(GraphDtoMapper::edge).toList(),
        result.uncertainties());
  }

  private IntelligenceExplanationResponse toExplanation(FindingExplanation explanation) {
    return new IntelligenceExplanationResponse(
        explanation.ruleId().value(),
        GraphDtoMapper.resource(explanation.resource()),
        explanation.explanation(),
        explanation.evidence().stream().map(this::toEvidenceReference).toList());
  }

  private EvidenceReferenceResponse toEvidenceReference(EvidenceReference reference) {
    return new EvidenceReferenceResponse(
        reference.ruleId().value(),
        GraphDtoMapper.resource(reference.resource()),
        reference.source());
  }

  private RecommendationResponse toRecommendation(Recommendation recommendation) {
    return new RecommendationResponse(
        recommendation.title(), recommendation.action(), recommendation.severity().name());
  }
}
