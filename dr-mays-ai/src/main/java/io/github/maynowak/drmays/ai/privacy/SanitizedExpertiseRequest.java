package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.List;
import java.util.Objects;

/**
 * The only request shape a remote model provider may receive.
 *
 * @param payloadVersion payload schema version
 * @param policyVersion privacy policy version
 * @param findings sanitized findings
 * @param relationships sanitized verified relationships
 * @param completeness discovery completeness
 * @param uncertainties sanitized uncertainty statements
 */
public record SanitizedExpertiseRequest(
    String payloadVersion,
    String policyVersion,
    List<SanitizedFinding> findings,
    List<SanitizedRelationship> relationships,
    String completeness,
    List<String> uncertainties) {

  public static final String CURRENT_PAYLOAD_VERSION = "privacy-payload/v1";

  public SanitizedExpertiseRequest {
    payloadVersion = DomainChecks.requireText(payloadVersion, "payloadVersion");
    policyVersion = DomainChecks.requireText(policyVersion, "policyVersion");
    findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
    relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships"));
    completeness = DomainChecks.requireText(completeness, "completeness");
    uncertainties = List.copyOf(Objects.requireNonNull(uncertainties, "uncertainties"));
  }
}
