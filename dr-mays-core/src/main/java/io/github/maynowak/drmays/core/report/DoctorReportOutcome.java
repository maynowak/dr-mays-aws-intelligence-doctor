package io.github.maynowak.drmays.core.report;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * A rule outcome rendered in the report, including the rule version used for evaluation.
 *
 * @param ruleId evaluated rule identifier
 * @param ruleVersion version of the evaluated rule
 * @param resource evaluated resource
 * @param type FINDING, NO_FINDING or NOT_APPLICABLE
 * @param reason non-finding reason; null for findings
 */
public record DoctorReportOutcome(
    String ruleId, String ruleVersion, ResourceIdentity resource, String type, String reason) {

  public DoctorReportOutcome {
    ruleId = DomainChecks.requireText(ruleId, "ruleId");
    ruleVersion = DomainChecks.requireText(ruleVersion, "ruleVersion");
    resource = Objects.requireNonNull(resource, "resource");
    type = DomainChecks.requireText(type, "type");
    if (!type.equals("FINDING") && !type.equals("NO_FINDING") && !type.equals("NOT_APPLICABLE")) {
      throw new IllegalArgumentException("type must be FINDING, NO_FINDING or NOT_APPLICABLE");
    }
    if (type.equals("FINDING")) {
      reason = null;
    } else {
      reason = DomainChecks.requireText(reason, "reason");
    }
  }
}
