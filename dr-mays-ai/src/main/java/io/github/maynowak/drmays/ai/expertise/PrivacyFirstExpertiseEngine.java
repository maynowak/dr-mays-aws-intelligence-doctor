package io.github.maynowak.drmays.ai.expertise;

import io.github.maynowak.drmays.ai.EvidenceReference;
import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.ai.ModelMetadata;
import io.github.maynowak.drmays.ai.Recommendation;
import io.github.maynowak.drmays.ai.privacy.ApprovalStatus;
import io.github.maynowak.drmays.ai.privacy.ApprovedInvocation;
import io.github.maynowak.drmays.ai.privacy.PrivacyGate;
import io.github.maynowak.drmays.ai.privacy.PrivacyPolicy;
import io.github.maynowak.drmays.ai.privacy.PrivacyPreview;
import io.github.maynowak.drmays.ai.privacy.PrivacyProcessor;
import io.github.maynowak.drmays.ai.provider.MockModelProvider;
import io.github.maynowak.drmays.ai.provider.ModelInvocationRequest;
import io.github.maynowak.drmays.ai.provider.ProviderCredentialReference;
import io.github.maynowak.drmays.ai.provider.ProviderExecutionStatus;
import io.github.maynowak.drmays.ai.provider.ProviderRegistry;
import io.github.maynowak.drmays.ai.provider.ProviderRouter;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.rule.RuleId;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mock-first orchestration of privacy projection, preview/approval, provider routing and expertise
 * validation. It never builds a second diagnosis engine.
 */
public final class PrivacyFirstExpertiseEngine {

  private static final Duration INVOCATION_TIMEOUT = Duration.ofSeconds(30);
  private static final int MAX_OUTPUT_CHARS = 64_000;

  private final ProviderRegistry registry;
  private final ProviderRouter router;
  private final ExpertiseCatalog catalog;
  private final PrivacyProcessor privacyProcessor;
  private final PrivacyGate gate;
  private final ExpertiseParser parser;
  private final ExpertiseValidator validator;

  /** Creates the default mock-only engine. */
  public PrivacyFirstExpertiseEngine() {
    this(
        defaultRegistry(),
        new ExpertiseCatalog(),
        new PrivacyProcessor(PrivacyPolicy.defaultPolicy()),
        new PrivacyGate(),
        new ExpertiseParser(),
        new ExpertiseValidator());
  }

  /**
   * Creates an engine with explicit dependencies for tests and integration.
   *
   * @param registry provider registry
   * @param catalog expertise catalog
   * @param privacyProcessor privacy projector
   * @param gate approval gate
   * @param parser model response parser
   * @param validator expertise validator
   */
  public PrivacyFirstExpertiseEngine(
      ProviderRegistry registry,
      ExpertiseCatalog catalog,
      PrivacyProcessor privacyProcessor,
      PrivacyGate gate,
      ExpertiseParser parser,
      ExpertiseValidator validator) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.privacyProcessor = Objects.requireNonNull(privacyProcessor, "privacyProcessor");
    this.gate = Objects.requireNonNull(gate, "gate");
    this.parser = Objects.requireNonNull(parser, "parser");
    this.validator = Objects.requireNonNull(validator, "validator");
    this.router = new ProviderRouter(registry, Set.of("mock"));
  }

  /**
   * Creates a preview for the exact sanitized payload. No provider is called.
   *
   * @param source verified intelligence request
   * @param providerId provider identifier
   * @param modelId model identifier
   * @param expertiseId expertise profile
   * @return safe preview
   */
  public PrivacyPreview preview(
      IntelligenceRequest source, String providerId, String modelId, String expertiseId) {
    Objects.requireNonNull(source, "source");
    var provider =
        registry
            .find(DomainChecks.requireText(providerId, "providerId"))
            .orElseThrow(() -> new IllegalArgumentException("unknown provider"));
    var model = DomainChecks.requireText(modelId, "modelId");
    if (!model.matches("[A-Za-z0-9._:/-]{1,128}")) {
      throw new IllegalArgumentException("modelId is invalid");
    }
    var expertise = catalog.require(DomainChecks.requireText(expertiseId, "expertiseId"));
    var processed = privacyProcessor.process(source);
    return gate.register(
        source,
        processed,
        providerId,
        model,
        expertise.id(),
        provider.descriptor().requiresApproval());
  }

  /**
   * Approves a preview.
   *
   * @param previewId preview identifier
   * @return opaque token when the preview exists
   */
  public Optional<io.github.maynowak.drmays.ai.privacy.ApprovalToken> approve(String previewId) {
    return gate.approve(previewId);
  }

  /**
   * Executes a previously previewed request. The source payload cannot be substituted by callers.
   *
   * @param previewId preview identifier
   * @param token approval token, or null for a non-approval provider
   * @return structured expertise or explicit failure
   */
  public ExpertiseReport explain(
      String previewId, io.github.maynowak.drmays.ai.privacy.ApprovalToken token) {
    var decision = gate.consume(previewId, token);
    if (decision.invocation().isEmpty()) {
      return approvalFailure(decision.status());
    }
    return execute(decision.invocation().orElseThrow());
  }

  private ExpertiseReport execute(ApprovedInvocation invocation) {
    var source = invocation.source();
    var processed = invocation.processed();
    var facts = facts(source);
    var metadata = new ModelMetadata(invocation.providerId(), invocation.modelId(), "1.0");
    var provider = registry.find(invocation.providerId()).orElse(null);
    var approvalStatus =
        provider != null && provider.descriptor().requiresApproval()
            ? ApprovalStatus.APPROVED
            : ApprovalStatus.NOT_REQUIRED;
    var receipt =
        new PrivacyReceipt(
            processed.policy().version(),
            processed.payloadHash(),
            approvalStatus,
            invocation.providerId(),
            invocation.modelId(),
            invocation.expertiseId());
    if (!processed.validation().valid()) {
      return ExpertiseReport.failure(
          ProviderExecutionStatus.PRIVACY_REJECTED, metadata, facts, receipt);
    }
    var profile = catalog.find(invocation.expertiseId()).orElse(null);
    if (profile == null) {
      return ExpertiseReport.failure(ProviderExecutionStatus.NOT_ALLOWED, metadata, facts, receipt);
    }
    var request =
        new ModelInvocationRequest(
            invocation.providerId(),
            invocation.modelId(),
            processed.payload(),
            processed.payloadJson(),
            ProviderCredentialReference.none(invocation.providerId()),
            INVOCATION_TIMEOUT,
            MAX_OUTPUT_CHARS);
    var result = router.route(request);
    if (result.status() != ProviderExecutionStatus.SUCCESS) {
      return ExpertiseReport.failure(result.status(), metadata, facts, receipt);
    }
    try {
      var response = parser.parse(result.content());
      var explanations = new ArrayList<FindingExplanation>();
      var evidenceReferences = new ArrayList<EvidenceReference>();
      for (var explanation : response.explanations()) {
        var ruleId = new RuleId(explanation.ruleId());
        var resource =
            processed
                .aliasMapping()
                .resolveResource(explanation.resourceAlias())
                .orElseThrow(() -> new IllegalArgumentException("unknown resource alias"));
        if (explanation.evidence().isEmpty()) {
          throw new IllegalArgumentException("evidence is required");
        }
        var references =
            explanation.evidence().stream()
                .map(evidence -> new EvidenceReference(ruleId, resource, evidence.source()))
                .toList();
        evidenceReferences.addAll(references);
        explanations.add(
            new FindingExplanation(ruleId, resource, explanation.explanation(), references));
      }
      var recommendations = new ArrayList<Recommendation>();
      for (var recommendation : response.recommendations()) {
        recommendations.add(
            new Recommendation(
                recommendation.title(),
                recommendation.action(),
                Severity.valueOf(recommendation.severity())));
      }
      var missingInformation = new ArrayList<>(response.missingInformation());
      var uncertainties = new ArrayList<>(response.uncertainties());
      if (source.completeness() == GraphCompleteness.PARTIAL
          && !uncertainties.contains(
              "Discovery data is partial; unknown resources are not covered.")) {
        uncertainties.add("Discovery data is partial; unknown resources are not covered.");
      }
      var report =
          ExpertiseReport.success(
              metadata,
              response.summary(),
              facts,
              explanations,
              recommendations,
              response.assumptions(),
              missingInformation,
              evidenceReferences,
              source.relationships(),
              uncertainties,
              receipt);
      return validator.validate(source, report, profile);
    } catch (RuntimeException exception) {
      return ExpertiseReport.failure(
          ProviderExecutionStatus.INVALID_RESPONSE, metadata, facts, receipt);
    }
  }

  private ExpertiseReport approvalFailure(ApprovalStatus status) {
    var mapped =
        status == ApprovalStatus.PRIVACY_REJECTED
            ? ProviderExecutionStatus.PRIVACY_REJECTED
            : ProviderExecutionStatus.APPROVAL_REQUIRED;
    return ExpertiseReport.failure(
        mapped, new ModelMetadata("unknown", "unknown", "1.0"), List.of(), PrivacyReceipt.none());
  }

  private static List<String> facts(IntelligenceRequest source) {
    return source.findings().stream()
        .sorted(
            Comparator.comparing(
                    (io.github.maynowak.drmays.core.finding.DiagnosticFinding finding) ->
                        finding.resource().canonicalId())
                .thenComparing(finding -> finding.ruleId().value()))
        .map(finding ->
            finding.ruleId().value()
                + " "
                + finding.severity().name()
                + " on "
                + finding.resource().canonicalId()
                + "; evidence="
                + finding.evidence().source())
        .toList();
  }

  private static ProviderRegistry defaultRegistry() {
    var registry = new ProviderRegistry();
    registry.register(new MockModelProvider());
    return registry;
  }
}
