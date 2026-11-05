package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.report.DoctorReport;
import io.github.maynowak.drmays.core.report.DoctorReportGraph;
import io.github.maynowak.drmays.core.report.DoctorReportMetadata;
import io.github.maynowak.drmays.core.report.DoctorReportOutcome;
import io.github.maynowak.drmays.core.report.DoctorReportRule;
import io.github.maynowak.drmays.core.report.DoctorReportScope;
import io.github.maynowak.drmays.core.report.DoctorReportSummary;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;

/** Maps the core report model to versioned wire DTOs. */
final class DoctorReportDtoMapper {

  private DoctorReportDtoMapper() {}

  static DoctorReportResponse map(DoctorReport report) {
    return new DoctorReportResponse(
        report.schemaVersion(),
        metadata(report.metadata()),
        scope(report.scope()),
        summary(report.summary()),
        report.findings().stream().map(DoctorReportDtoMapper::finding).toList(),
        report.outcomes().stream().map(DoctorReportDtoMapper::outcome).toList(),
        graph(report.graph()),
        report.rules().stream().map(DoctorReportDtoMapper::rule).toList(),
        report.limitations());
  }

  private static DoctorReportMetadataResponse metadata(DoctorReportMetadata metadata) {
    return new DoctorReportMetadataResponse(
        metadata.generator(),
        metadata.mode(),
        metadata.diagnosisSource(),
        metadata.inputHash(),
        metadata.rulesFingerprint(),
        metadata.partialDiscovery(),
        metadata.resourceCount());
  }

  private static DoctorReportScopeResponse scope(DoctorReportScope scope) {
    return new DoctorReportScopeResponse(
        scope.providers(), scope.accounts(), scope.regions(), scope.resourceCount());
  }

  private static DoctorReportSummaryResponse summary(DoctorReportSummary summary) {
    return new DoctorReportSummaryResponse(
        summary.resourceCount(),
        summary.findingCount(),
        summary.outcomeCount(),
        summary.info(),
        summary.low(),
        summary.medium(),
        summary.high(),
        summary.critical(),
        summary.highestSeverity());
  }

  private static DoctorReportFindingResponse finding(DiagnosticFinding finding) {
    return new DoctorReportFindingResponse(
        finding.ruleId().value(),
        finding.ruleVersion(),
        finding.severity().name(),
        resource(finding.resource()),
        finding.title(),
        finding.description(),
        evidence(finding.evidence()),
        finding.recommendation());
  }

  private static DoctorReportOutcomeResponse outcome(DoctorReportOutcome outcome) {
    return new DoctorReportOutcomeResponse(
        outcome.ruleId(),
        outcome.ruleVersion(),
        resource(outcome.resource()),
        outcome.type(),
        outcome.reason());
  }

  private static DoctorReportGraphResponse graph(DoctorReportGraph graph) {
    var nodes =
        graph.graph().nodes().stream()
            .map(node ->
                new DoctorReportNodeResponse(
                    resource(node.identity()),
                    node.status().name(),
                    node.tags(),
                    node.attributes()))
            .toList();
    var edges =
        graph.graph().edges().stream()
            .map(edge ->
                new DoctorReportEdgeResponse(
                    resource(edge.from()),
                    resource(edge.to()),
                    edge.type().name(),
                    evidence(edge.evidence().evidence())))
            .toList();
    var issues =
        graph.issues().stream()
            .map(issue -> new GraphIssueResponse(issue.code().name(), issue.message()))
            .toList();
    var stats =
        new GraphStatsResponse(
            graph.nodeCount(),
            graph.edgeCount(),
            graph.inventoriedCount(),
            graph.referencedCount(),
            graph.componentCount(),
            graph.cyclic());
    return new DoctorReportGraphResponse(nodes, edges, issues, stats, graph.completeness().name());
  }

  private static DoctorReportRuleResponse rule(DoctorReportRule rule) {
    return new DoctorReportRuleResponse(rule.ruleId(), rule.version(), rule.description());
  }

  private static DoctorReportEvidenceResponse evidence(Evidence evidence) {
    return new DoctorReportEvidenceResponse(
        evidence.source(), evidence.observed(), evidence.expected(), evidence.details());
  }

  private static ResourceResponse resource(ResourceIdentity identity) {
    return new ResourceResponse(
        identity.provider(),
        identity.accountId(),
        identity.region(),
        identity.resourceType(),
        identity.resourceId());
  }
}
