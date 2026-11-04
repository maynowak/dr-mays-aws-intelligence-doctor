package io.github.maynowak.drmays.ai.expertise;

import io.github.maynowak.drmays.ai.EvidenceReference;
import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.ModelMetadata;
import io.github.maynowak.drmays.ai.Recommendation;
import io.github.maynowak.drmays.ai.provider.ProviderExecutionStatus;
import io.github.maynowak.drmays.core.dna.ResourceEdge;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.List;
import java.util.Objects;

/**
 * Structured, evidence-backed expertise report.
 *
 * @param status execution status
 * @param metadata provider/model metadata
 * @param summary model-generated summary
 * @param facts deterministic facts derived from the source request
 * @param explanations model explanations for existing findings
 * @param recommendations recommendations
 * @param assumptions unverified model assumptions
 * @param missingInformation explicitly missing information
 * @param evidenceReferences existing evidence references
 * @param verifiedRelationships original verified relationships
 * @param uncertainties uncertainty statements
 * @param privacyReceipt safe privacy/approval metadata
 */
public record ExpertiseReport(
    ProviderExecutionStatus status,
    ModelMetadata metadata,
    String summary,
    List<String> facts,
    List<FindingExplanation> explanations,
    List<Recommendation> recommendations,
    List<String> assumptions,
    List<String> missingInformation,
    List<EvidenceReference> evidenceReferences,
    List<ResourceEdge> verifiedRelationships,
    List<String> uncertainties,
    PrivacyReceipt privacyReceipt) {

  public ExpertiseReport {
    status = Objects.requireNonNull(status, "status");
    metadata = Objects.requireNonNull(metadata, "metadata");
    summary = DomainChecks.requireText(summary, "summary");
    facts = List.copyOf(Objects.requireNonNull(facts, "facts"));
    explanations = List.copyOf(Objects.requireNonNull(explanations, "explanations"));
    recommendations = List.copyOf(Objects.requireNonNull(recommendations, "recommendations"));
    assumptions = List.copyOf(Objects.requireNonNull(assumptions, "assumptions"));
    missingInformation =
        List.copyOf(Objects.requireNonNull(missingInformation, "missingInformation"));
    evidenceReferences =
        List.copyOf(Objects.requireNonNull(evidenceReferences, "evidenceReferences"));
    verifiedRelationships =
        List.copyOf(Objects.requireNonNull(verifiedRelationships, "verifiedRelationships"));
    uncertainties = List.copyOf(Objects.requireNonNull(uncertainties, "uncertainties"));
    privacyReceipt = Objects.requireNonNull(privacyReceipt, "privacyReceipt");
  }

  /**
   * Creates a successful report.
   *
   * @param metadata metadata
   * @param summary summary
   * @param facts deterministic facts
   * @param explanations explanations
   * @param recommendations recommendations
   * @param assumptions assumptions
   * @param missingInformation missing information
   * @param evidenceReferences evidence references
   * @param verifiedRelationships verified relationships
   * @param uncertainties uncertainties
   * @param receipt privacy receipt
   * @return success report
   */
  public static ExpertiseReport success(
      ModelMetadata metadata,
      String summary,
      List<String> facts,
      List<FindingExplanation> explanations,
      List<Recommendation> recommendations,
      List<String> assumptions,
      List<String> missingInformation,
      List<EvidenceReference> evidenceReferences,
      List<ResourceEdge> verifiedRelationships,
      List<String> uncertainties,
      PrivacyReceipt receipt) {
    return new ExpertiseReport(
        ProviderExecutionStatus.SUCCESS,
        metadata,
        summary,
        facts,
        explanations,
        recommendations,
        assumptions,
        missingInformation,
        evidenceReferences,
        verifiedRelationships,
        uncertainties,
        receipt);
  }

  /**
   * Creates a failure report without explanations or recommendations.
   *
   * @param status failure status
   * @param metadata metadata
   * @param facts deterministic facts
   * @param receipt privacy receipt
   * @return failure report
   */
  public static ExpertiseReport failure(
      ProviderExecutionStatus status,
      ModelMetadata metadata,
      List<String> facts,
      PrivacyReceipt receipt) {
    if (status == ProviderExecutionStatus.SUCCESS) {
      throw new IllegalArgumentException("failure status must not be SUCCESS");
    }
    return new ExpertiseReport(
        status,
        metadata,
        "Expertise request was not completed: " + status.name() + ".",
        facts,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        receipt);
  }
}
