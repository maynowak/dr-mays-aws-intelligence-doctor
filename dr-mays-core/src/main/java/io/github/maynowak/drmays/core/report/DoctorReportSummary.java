package io.github.maynowak.drmays.core.report;

/**
 * Deterministic counts derived only from the existing diagnosis result.
 *
 * @param resourceCount number of inventoried resources
 * @param findingCount number of technical findings
 * @param outcomeCount number of rule/resource outcomes
 * @param info INFO findings
 * @param low LOW findings
 * @param medium MEDIUM findings
 * @param high HIGH findings
 * @param critical CRITICAL findings
 * @param highestSeverity highest observed severity, or NONE
 */
public record DoctorReportSummary(
    int resourceCount,
    int findingCount,
    int outcomeCount,
    int info,
    int low,
    int medium,
    int high,
    int critical,
    String highestSeverity) {

  public DoctorReportSummary {
    if (resourceCount < 0
        || findingCount < 0
        || outcomeCount < 0
        || info < 0
        || low < 0
        || medium < 0
        || high < 0
        || critical < 0) {
      throw new IllegalArgumentException("summary counts must not be negative");
    }
    if (findingCount != info + low + medium + high + critical) {
      throw new IllegalArgumentException("findingCount must equal the severity count sum");
    }
    if (highestSeverity == null || highestSeverity.isBlank()) {
      throw new IllegalArgumentException("highestSeverity must not be blank");
    }
  }
}
