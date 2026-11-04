package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.List;
import java.util.Objects;

/**
 * Complete local result of privacy projection.
 *
 * @param payload provider-facing sanitized payload
 * @param aliasMapping local-only alias map
 * @param policy applied policy
 * @param validation privacy validation result
 * @param payloadJson exact serialized payload shown in preview
 * @param payloadHash SHA-256 hash of the exact payload bytes
 * @param decisions field-level preview decisions
 */
public record PrivacyProcessingResult(
    SanitizedExpertiseRequest payload,
    AliasMapping aliasMapping,
    PrivacyPolicy policy,
    PrivacyValidationResult validation,
    String payloadJson,
    String payloadHash,
    List<PrivacyFieldDecision> decisions) {

  public PrivacyProcessingResult {
    payload = Objects.requireNonNull(payload, "payload");
    aliasMapping = Objects.requireNonNull(aliasMapping, "aliasMapping");
    policy = Objects.requireNonNull(policy, "policy");
    validation = Objects.requireNonNull(validation, "validation");
    payloadJson = DomainChecks.requireText(payloadJson, "payloadJson");
    payloadHash = DomainChecks.requireText(payloadHash, "payloadHash");
    decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions"));
  }
}
