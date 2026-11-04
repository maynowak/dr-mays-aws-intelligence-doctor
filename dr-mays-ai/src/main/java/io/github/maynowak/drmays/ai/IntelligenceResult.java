package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.dna.ResourceEdge;
import java.util.List;
import java.util.Objects;

/**
 * Structured AI response, validated against the original request.
 *
 * @param status execution status
 * @param metadata provider/model metadata
 * @param summary concise explanation summary
 * @param explanations explanations for existing findings
 * @param recommendations suggested actions
 * @param evidenceReferences evidence references used by the explanation
 * @param verifiedRelationships subset of verified request relationships
 * @param uncertainties explicitly unknown impacts
 */
public record IntelligenceResult(
    ModelExecutionStatus status,
    ModelMetadata metadata,
    String summary,
    List<FindingExplanation> explanations,
    List<Recommendation> recommendations,
    List<EvidenceReference> evidenceReferences,
    List<ResourceEdge> verifiedRelationships,
    List<String> uncertainties) {

  public IntelligenceResult {
    status = Objects.requireNonNull(status, "status");
    metadata = Objects.requireNonNull(metadata, "metadata");
    summary = Objects.requireNonNull(summary, "summary");
    explanations = List.copyOf(Objects.requireNonNull(explanations, "explanations"));
    recommendations = List.copyOf(Objects.requireNonNull(recommendations, "recommendations"));
    evidenceReferences =
        List.copyOf(Objects.requireNonNull(evidenceReferences, "evidenceReferences"));
    verifiedRelationships =
        List.copyOf(Objects.requireNonNull(verifiedRelationships, "verifiedRelationships"));
    uncertainties = List.copyOf(Objects.requireNonNull(uncertainties, "uncertainties"));
  }

  public static IntelligenceResult disabled(ModelMetadata metadata) {
    return new IntelligenceResult(
        ModelExecutionStatus.DISABLED,
        metadata,
        "AI explanation is disabled.",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  /**
   * Returns an approval-required result without calling a provider.
   *
   * @param metadata provider/model metadata
   * @return approval-required result
   */
  public static IntelligenceResult approvalRequired(ModelMetadata metadata) {
    return new IntelligenceResult(
        ModelExecutionStatus.APPROVAL_REQUIRED,
        metadata,
        "A privacy preview and approval are required before external model execution.",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  public static IntelligenceResult unavailable(ModelMetadata metadata) {
    return new IntelligenceResult(
        ModelExecutionStatus.UNAVAILABLE,
        metadata,
        "The AI model is unavailable.",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  public static IntelligenceResult failed(ModelMetadata metadata) {
    return new IntelligenceResult(
        ModelExecutionStatus.FAILED,
        metadata,
        "AI explanation failed.",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  public static IntelligenceResult invalidResponse(ModelMetadata metadata) {
    return new IntelligenceResult(
        ModelExecutionStatus.INVALID_RESPONSE,
        metadata,
        "AI response rejected by verification.",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }
}
