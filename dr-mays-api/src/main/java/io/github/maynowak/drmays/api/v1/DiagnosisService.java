package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.diagnosis.DiagnosisResult;
import io.github.maynowak.drmays.core.dna.InfrastructureGraph;
import io.github.maynowak.drmays.core.dna.InfrastructureGraphBuilder;
import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.DiagnosticContext;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Translates wire DTOs to the existing domain engine and back without duplicating diagnosis logic.
 */
@Service
public class DiagnosisService {

  private final DiagnosisEngine engine;

  public DiagnosisService(DiagnosisEngine engine) {
    this.engine = engine;
  }

  /**
   * Runs the registered rules over the request snapshot.
   *
   * @param request validated synthetic snapshot
   * @return deterministic wire response
   */
  public DiagnosisResponse diagnose(DiagnosisRequest request) {
    var resources = toResources(request);
    var context = new DiagnosticContext(resources);
    var result = engine.diagnose(context);
    var graph = new InfrastructureGraphBuilder().build(resources).graph();
    return toResponse(result, graph);
  }

  /** Maps validated wire resources to the existing domain model. */
  List<InfrastructureResource> toResources(DiagnosisRequest request) {
    return request.resources().stream().map(this::toResource).toList();
  }

  /**
   * Returns the registered rules in deterministic order.
   *
   * @return wire representation of the registered rules
   */
  public RulesResponse rules() {
    var rules =
        engine.rules().stream()
            .map(rule -> new RuleResponse(rule.id().value(), rule.version(), rule.description()))
            .toList();
    return new RulesResponse(rules);
  }

  private InfrastructureResource toResource(ResourceRequest request) {
    var identity = request.identity();
    var resourceIdentity =
        new ResourceIdentity(
            identity.provider(),
            identity.accountId(),
            identity.region(),
            identity.resourceType(),
            identity.resourceId());
    return new InfrastructureResource(resourceIdentity, request.tags(), request.attributes());
  }

  private DiagnosisResponse toResponse(DiagnosisResult result, InfrastructureGraph graph) {
    var findings = result.findings().stream().map(finding -> toFinding(finding, graph)).toList();
    var outcomes = result.outcomes().stream().map(this::toOutcome).toList();
    return new DiagnosisResponse(findings, outcomes, result.findingCount());
  }

  private FindingResponse toFinding(DiagnosticFinding finding, InfrastructureGraph graph) {
    var relationships =
        graph.outgoing(finding.resource()).stream().map(GraphDtoMapper::edge).toList();
    return new FindingResponse(
        finding.ruleId().value(),
        finding.ruleVersion(),
        finding.severity().name(),
        toResource(finding.resource()),
        finding.title(),
        finding.description(),
        toEvidence(finding.evidence()),
        finding.recommendation(),
        relationships);
  }

  private EvidenceResponse toEvidence(Evidence evidence) {
    return new EvidenceResponse(
        evidence.source(), evidence.observed(), evidence.expected(), evidence.details());
  }

  private OutcomeResponse toOutcome(DiagnosticOutcome outcome) {
    return switch (outcome) {
      case DiagnosticOutcome.FindingDetected detected ->
          new OutcomeResponse(
              detected.ruleId().value(), toResource(detected.resource()), "FINDING", null);
      case DiagnosticOutcome.NoFinding noFinding ->
          new OutcomeResponse(
              noFinding.ruleId().value(),
              toResource(noFinding.resource()),
              "NO_FINDING",
              noFinding.reason());
      case DiagnosticOutcome.NotApplicable notApplicable ->
          new OutcomeResponse(
              notApplicable.ruleId().value(),
              toResource(notApplicable.resource()),
              "NOT_APPLICABLE",
              notApplicable.reason());
    };
  }

  private ResourceResponse toResource(ResourceIdentity identity) {
    return new ResourceResponse(
        identity.provider(),
        identity.accountId(),
        identity.region(),
        identity.resourceType(),
        identity.resourceId());
  }
}
