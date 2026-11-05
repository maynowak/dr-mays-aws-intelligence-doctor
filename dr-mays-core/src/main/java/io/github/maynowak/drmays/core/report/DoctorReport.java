package io.github.maynowak.drmays.core.report;

import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic, evidence-backed Doctor Report. It aggregates existing diagnosis and DNA data and
 * does not create new findings or relationships.
 *
 * @param schemaVersion report schema version
 * @param metadata report provenance metadata
 * @param scope reviewed snapshot scope
 * @param summary deterministic summary counts
 * @param findings findings produced by the existing diagnosis engine
 * @param outcomes all rule/resource outcomes
 * @param graph verified Infrastructure DNA graph
 * @param rules evaluated rule catalog
 * @param limitations explicit coverage and interpretation limits
 */
public record DoctorReport(
    String schemaVersion,
    DoctorReportMetadata metadata,
    DoctorReportScope scope,
    DoctorReportSummary summary,
    List<DiagnosticFinding> findings,
    List<DoctorReportOutcome> outcomes,
    DoctorReportGraph graph,
    List<DoctorReportRule> rules,
    List<String> limitations) {

  public static final String SCHEMA_VERSION = "doctor-report-v1";

  public DoctorReport {
    schemaVersion = DomainChecks.requireText(schemaVersion, "schemaVersion");
    metadata = Objects.requireNonNull(metadata, "metadata");
    scope = Objects.requireNonNull(scope, "scope");
    summary = Objects.requireNonNull(summary, "summary");
    findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
    outcomes = List.copyOf(Objects.requireNonNull(outcomes, "outcomes"));
    graph = Objects.requireNonNull(graph, "graph");
    rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
    limitations = List.copyOf(Objects.requireNonNull(limitations, "limitations"));
  }
}
