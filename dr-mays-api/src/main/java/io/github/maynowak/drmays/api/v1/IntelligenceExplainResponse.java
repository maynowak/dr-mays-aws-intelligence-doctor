package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Structured AI response with explicit execution status.
 *
 * @param status SUCCESS, DISABLED, APPROVAL_REQUIRED, UNAVAILABLE, FAILED or INVALID_RESPONSE
 * @param metadata provider/model metadata
 * @param summary explanation summary
 * @param explanations explanations of existing findings
 * @param recommendations suggested actions
 * @param evidenceReferences existing evidence references
 * @param verifiedRelationships verified DNA relationships
 * @param uncertainties explicitly unknown impacts
 */
public record IntelligenceExplainResponse(
    String status,
    IntelligenceMetadataResponse metadata,
    String summary,
    List<IntelligenceExplanationResponse> explanations,
    List<RecommendationResponse> recommendations,
    List<EvidenceReferenceResponse> evidenceReferences,
    List<GraphEdgeResponse> verifiedRelationships,
    List<String> uncertainties) {

  public IntelligenceExplainResponse {
    explanations = List.copyOf(explanations);
    recommendations = List.copyOf(recommendations);
    evidenceReferences = List.copyOf(evidenceReferences);
    verifiedRelationships = List.copyOf(verifiedRelationships);
    uncertainties = List.copyOf(uncertainties);
  }
}
