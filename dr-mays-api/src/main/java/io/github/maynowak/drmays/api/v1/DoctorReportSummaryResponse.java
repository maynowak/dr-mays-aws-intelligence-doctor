package io.github.maynowak.drmays.api.v1;

/**
 * Wire summary for a Doctor Report.
 *
 * @param resourceCount reviewed resources
 * @param findingCount technical findings
 * @param outcomeCount rule/resource outcomes
 * @param info INFO findings
 * @param low LOW findings
 * @param medium MEDIUM findings
 * @param high HIGH findings
 * @param critical CRITICAL findings
 * @param highestSeverity highest observed severity, or NONE
 */
public record DoctorReportSummaryResponse(
    int resourceCount,
    int findingCount,
    int outcomeCount,
    int info,
    int low,
    int medium,
    int high,
    int critical,
    String highestSeverity) {}
