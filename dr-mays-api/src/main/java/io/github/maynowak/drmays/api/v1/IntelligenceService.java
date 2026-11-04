package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.ai.EvidenceReference;
import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.IntelligenceAnalyzer;
import io.github.maynowak.drmays.ai.IntelligenceResult;
import io.github.maynowak.drmays.ai.Recommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Builds verified intelligence input server-side and calls the configured analyzer. */
@Service
public class IntelligenceService {

  private final IntelligenceInputFactory inputFactory;
  private final IntelligenceAnalyzer analyzer;

  /**
   * Creates the service from existing dependencies.
   *
   * @param diagnosisService deterministic diagnosis service
   * @param analyzer configured intelligence analyzer
   */
  public IntelligenceService(DiagnosisService diagnosisService, IntelligenceAnalyzer analyzer) {
    this(new IntelligenceInputFactory(diagnosisService), analyzer);
  }

  /**
   * Creates the service with an explicit input factory.
   *
   * @param inputFactory verified input factory
   * @param analyzer configured intelligence analyzer
   */
  @Autowired
  public IntelligenceService(IntelligenceInputFactory inputFactory, IntelligenceAnalyzer analyzer) {
    this.inputFactory = inputFactory;
    this.analyzer = analyzer;
  }

  /**
   * Runs deterministic diagnosis/DNA extraction and explains the result.
   *
   * @param request synthetic resource request
   * @return structured AI response
   */
  public IntelligenceExplainResponse explain(IntelligenceExplainRequest request) {
    var result =
        analyzer.analyze(
            inputFactory.build(
                request.resources(), Boolean.TRUE.equals(request.partialDiscovery())));
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
