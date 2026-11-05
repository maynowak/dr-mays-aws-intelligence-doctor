package io.github.maynowak.drmays.api.v1;

/**
 * Wire finding for a Doctor Report.
 *
 * @param ruleId rule identifier
 * @param ruleVersion rule version
 * @param severity finding severity
 * @param resource affected resource
 * @param title finding title
 * @param description factual description
 * @param evidence traceable evidence
 * @param recommendation existing rule recommendation
 */
public record DoctorReportFindingResponse(
    String ruleId,
    String ruleVersion,
    String severity,
    ResourceResponse resource,
    String title,
    String description,
    DoctorReportEvidenceResponse evidence,
    String recommendation) {}
