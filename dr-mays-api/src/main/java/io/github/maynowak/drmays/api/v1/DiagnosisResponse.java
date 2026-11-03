package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Deterministic result of one diagnosis run.
 *
 * @param findings technical findings in deterministic order
 * @param outcomes every rule/resource outcome in deterministic order
 * @param findingCount number of findings
 */
public record DiagnosisResponse(
    List<FindingResponse> findings, List<OutcomeResponse> outcomes, int findingCount) {

  public DiagnosisResponse {
    findings = List.copyOf(findings);
    outcomes = List.copyOf(outcomes);
  }
}
