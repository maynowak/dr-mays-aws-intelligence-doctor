package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Versioned Doctor Report response.
 *
 * @param schemaVersion report schema version
 * @param metadata report provenance
 * @param scope reviewed snapshot scope
 * @param summary deterministic summary counts
 * @param findings existing deterministic findings
 * @param outcomes all rule/resource outcomes
 * @param graph verified Infrastructure DNA graph
 * @param rules evaluated rule catalog
 * @param limitations explicit limitations
 */
public record DoctorReportResponse(
    String schemaVersion,
    DoctorReportMetadataResponse metadata,
    DoctorReportScopeResponse scope,
    DoctorReportSummaryResponse summary,
    List<DoctorReportFindingResponse> findings,
    List<DoctorReportOutcomeResponse> outcomes,
    DoctorReportGraphResponse graph,
    List<DoctorReportRuleResponse> rules,
    List<String> limitations) {

  public DoctorReportResponse {
    findings = List.copyOf(findings);
    outcomes = List.copyOf(outcomes);
    rules = List.copyOf(rules);
    limitations = List.copyOf(limitations);
  }
}
