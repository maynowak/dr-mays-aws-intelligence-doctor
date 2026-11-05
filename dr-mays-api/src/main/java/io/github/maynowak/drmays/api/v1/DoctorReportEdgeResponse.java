package io.github.maynowak.drmays.api.v1;

/**
 * Wire graph edge for a Doctor Report.
 *
 * @param from source resource
 * @param to target resource
 * @param type verified relationship type
 * @param evidence relationship provenance
 */
public record DoctorReportEdgeResponse(
    ResourceResponse from,
    ResourceResponse to,
    String type,
    DoctorReportEvidenceResponse evidence) {}
