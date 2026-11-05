package io.github.maynowak.drmays.api.v1;

/**
 * Wire rule catalog entry for a Doctor Report.
 *
 * @param ruleId rule identifier
 * @param version rule version
 * @param description rule description
 */
public record DoctorReportRuleResponse(String ruleId, String version, String description) {}
