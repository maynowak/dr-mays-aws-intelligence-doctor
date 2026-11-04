package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.ai.EvidenceReference;
import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.Recommendation;
import io.github.maynowak.drmays.ai.expertise.ExpertiseReport;
import io.github.maynowak.drmays.ai.expertise.PrivacyFirstExpertiseEngine;
import io.github.maynowak.drmays.ai.privacy.ApprovalToken;
import io.github.maynowak.drmays.ai.privacy.PrivacyPreview;
import org.springframework.stereotype.Service;

/** Additive, mock-only REST orchestration for privacy previews and expertise. */
@Service
public class ExpertiseService {

  private final IntelligenceInputFactory inputFactory;
  private final PrivacyFirstExpertiseEngine engine;

  public ExpertiseService(
      IntelligenceInputFactory inputFactory, PrivacyFirstExpertiseEngine engine) {
    this.inputFactory = inputFactory;
    this.engine = engine;
  }

  /**
   * Creates a sanitized preview without calling a provider.
   *
   * @param request preview request
   * @return safe preview
   */
  public ExpertisePreviewResponse preview(ExpertisePreviewRequest request) {
    var source = inputFactory.build(request.resources(), request.partialDiscovery());
    var preview =
        engine.preview(source, request.providerId(), request.modelId(), request.expertiseId());
    return toResponse(preview);
  }

  /**
   * Approves a preview and returns a single-use token.
   *
   * @param request approval request
   * @return token response
   */
  public ExpertiseApprovalResponse approve(ExpertiseApprovalRequest request) {
    var token =
        engine
            .approve(request.previewId())
            .orElseThrow(() -> new IllegalArgumentException("unknown or expired preview"));
    return new ExpertiseApprovalResponse(token.value());
  }

  /**
   * Executes a previously previewed request.
   *
   * @param request execution request
   * @return structured expertise
   */
  public ExpertiseExplainResponse explain(ExpertiseExplainRequest request) {
    var token =
        request.approvalToken() == null || request.approvalToken().isBlank()
            ? null
            : new ApprovalToken(request.approvalToken());
    return toResponse(engine.explain(request.previewId(), token));
  }

  private ExpertisePreviewResponse toResponse(PrivacyPreview preview) {
    return new ExpertisePreviewResponse(
        preview.previewId(),
        preview.payloadHash(),
        preview.policyVersion(),
        preview.providerId(),
        preview.modelId(),
        preview.expertiseId(),
        preview.approvalRequired(),
        preview.expiresAt(),
        preview.payloadJson(),
        preview.decisions().stream()
            .map(decision ->
                new ExpertiseFieldDecisionResponse(
                    decision.field().name(),
                    decision.classification().name(),
                    decision.included(),
                    decision.reason()))
            .toList(),
        new ExpertisePrivacyValidationResponse(
            preview.validation().valid(),
            preview.validation().violations().stream()
                .map(violation ->
                    new PrivacyViolationResponse(
                        violation.code(), violation.field(), violation.message()))
                .toList()));
  }

  private ExpertiseExplainResponse toResponse(ExpertiseReport report) {
    return new ExpertiseExplainResponse(
        report.status().name(),
        new IntelligenceMetadataResponse(
            report.metadata().provider(), report.metadata().model(), report.metadata().version()),
        report.summary(),
        report.facts(),
        report.explanations().stream().map(this::toExplanation).toList(),
        report.recommendations().stream().map(this::toRecommendation).toList(),
        report.assumptions(),
        report.missingInformation(),
        report.evidenceReferences().stream().map(this::toEvidenceReference).toList(),
        report.verifiedRelationships().stream().map(GraphDtoMapper::edge).toList(),
        report.uncertainties(),
        new ExpertisePrivacyReceiptResponse(
            report.privacyReceipt().policyVersion(),
            report.privacyReceipt().payloadHash(),
            report.privacyReceipt().approvalStatus().name(),
            report.privacyReceipt().providerId(),
            report.privacyReceipt().modelId(),
            report.privacyReceipt().expertiseId()));
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
