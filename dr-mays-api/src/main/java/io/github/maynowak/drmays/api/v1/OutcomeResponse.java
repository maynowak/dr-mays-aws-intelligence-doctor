package io.github.maynowak.drmays.api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wire representation of a rule evaluation outcome.
 *
 * @param ruleId evaluated rule
 * @param resource evaluated resource
 * @param type {@code FINDING}, {@code NO_FINDING} or {@code NOT_APPLICABLE}
 * @param reason reason for a non-finding outcome, omitted for findings
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OutcomeResponse(
    String ruleId, ResourceResponse resource, String type, String reason) {}
