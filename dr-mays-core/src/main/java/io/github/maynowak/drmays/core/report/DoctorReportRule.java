package io.github.maynowak.drmays.core.report;

import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * One rule version represented in the report appendix.
 *
 * @param ruleId stable rule identifier
 * @param version evaluated rule version
 * @param description rule description
 */
public record DoctorReportRule(String ruleId, String version, String description) {

  public DoctorReportRule {
    ruleId = DomainChecks.requireText(ruleId, "ruleId");
    version = DomainChecks.requireText(version, "version");
    description = DomainChecks.requireText(description, "description");
  }
}
