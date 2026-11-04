package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Structured expertise response with explicit status and privacy receipt.
 *
 * @param status execution status
 * @param metadata provider/model metadata
 * @param summary summary
 * @param facts deterministic facts
 * @param explanations explanations
 * @param recommendations recommendations
 * @param assumptions unverified assumptions
 * @param missingInformation missing information
 * @param evidenceReferences evidence references
 * @param verifiedRelationships verified relationships
 * @param uncertainties uncertainties
 * @param privacyReceipt privacy/approval receipt
 */
public record ExpertiseExplainResponse(
    String status,
    IntelligenceMetadataResponse metadata,
    String summary,
    List<String> facts,
    List<IntelligenceExplanationResponse> explanations,
    List<RecommendationResponse> recommendations,
    List<String> assumptions,
    List<String> missingInformation,
    List<EvidenceReferenceResponse> evidenceReferences,
    List<GraphEdgeResponse> verifiedRelationships,
    List<String> uncertainties,
    ExpertisePrivacyReceiptResponse privacyReceipt) {

  public ExpertiseExplainResponse {
    facts = List.copyOf(facts);
    explanations = List.copyOf(explanations);
    recommendations = List.copyOf(recommendations);
    assumptions = List.copyOf(assumptions);
    missingInformation = List.copyOf(missingInformation);
    evidenceReferences = List.copyOf(evidenceReferences);
    verifiedRelationships = List.copyOf(verifiedRelationships);
    uncertainties = List.copyOf(uncertainties);
  }
}
