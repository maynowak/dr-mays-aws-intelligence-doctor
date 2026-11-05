package io.github.maynowak.drmays.api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wire rule outcome for a Doctor Report.
 *
 * @param ruleId evaluated rule
 * @param ruleVersion rule version used
 * @param resource evaluated resource
 * @param type FINDING, NO_FINDING or NOT_APPLICABLE
 * @param reason non-finding reason; omitted for findings
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoctorReportOutcomeResponse(
    String ruleId, String ruleVersion, ResourceResponse resource, String type, String reason) {}
