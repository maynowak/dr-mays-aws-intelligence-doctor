package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Validates that provider-facing payloads contain no raw identifiers or secrets. */
public final class PrivacyValidator {

  private static final Pattern ACCOUNT_ID = Pattern.compile("\\b\\d{12}\\b");
  private static final Pattern RAW_RESOURCE_ID =
      Pattern.compile("\\b(?:sg|vpc|subnet|i|vol)-[A-Za-z0-9]{8,}\\b");
  private static final Pattern ARN = Pattern.compile("\\barn:(?:aws|aws-us-gov|aws-cn):[^\\s]+");
  private static final Pattern EMAIL =
      Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
  private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
  private static final Pattern HOSTNAME =
      Pattern.compile("\\b(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}\\b");
  private static final Pattern SECRET =
      Pattern.compile(
          "(?i)(?:AKIA|ASIA)[0-9A-Z]{16}"
              + "|ghp_[A-Za-z0-9]{20,}"
              + "|sk-[A-Za-z0-9]{20,}"
              + "|xox[baprs]-[A-Za-z0-9-]{10,}"
              + "|-----BEGIN [A-Z ]+PRIVATE KEY-----"
              + "|Bearer\\s+[A-Za-z0-9._-]{12,}"
              + "|SECRET_REDACTED");
  private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

  /**
   * Validates a sanitized payload against the policy.
   *
   * @param payload sanitized payload
   * @param policy applied policy
   * @return validation outcome
   */
  public PrivacyValidationResult validate(SanitizedExpertiseRequest payload, PrivacyPolicy policy) {
    var violations = new ArrayList<PrivacyViolation>();
    if (!payload.policyVersion().equals(policy.version())) {
      violations.add(
          new PrivacyViolation(
              PrivacyViolation.INVALID_FIELD, "policyVersion", "policy version mismatch"));
    }
    for (var finding : payload.findings()) {
      check("ruleId", finding.ruleId(), violations, false);
      check("ruleVersion", finding.ruleVersion(), violations, false);
      check("severity", finding.severity(), violations, false);
      check("resourceAlias", finding.resourceAlias(), violations, false);
      check("resourceType", finding.resourceType(), violations, false);
      check("regionClass", finding.regionClass(), violations, false);
      check("title", finding.title(), violations, true);
      check("description", finding.description(), violations, true);
      check("evidenceSource", finding.evidenceSource(), violations, true);
      check("observed", finding.observed(), violations, true);
      check("expected", finding.expected(), violations, true);
    }
    for (var relationship : payload.relationships()) {
      check("type", relationship.type(), violations, false);
      check("fromAlias", relationship.fromAlias(), violations, false);
      check("toAlias", relationship.toAlias(), violations, false);
      check("evidenceSource", relationship.evidenceSource(), violations, true);
      check("observed", relationship.observed(), violations, true);
    }
    check("completeness", payload.completeness(), violations, false);
    for (var uncertainty : payload.uncertainties()) {
      check("uncertainties", uncertainty, violations, true);
    }
    return new PrivacyValidationResult(true, violations);
  }

  /**
   * Validates the serialized size of the exact payload.
   *
   * @param payloadJson serialized payload
   * @param policy applied policy
   * @return validation outcome
   */
  public PrivacyValidationResult validateSize(String payloadJson, PrivacyPolicy policy) {
    if (payloadJson.length() > policy.maxPayloadChars()) {
      return PrivacyValidationResult.invalid(
          List.of(
              new PrivacyViolation(
                  PrivacyViolation.PAYLOAD_TOO_LARGE,
                  "payload",
                  "payload exceeds the policy limit")));
    }
    return PrivacyValidationResult.ok();
  }

  /**
   * Detects raw secrets in source text. Raw account IDs are expected at this stage and are
   * pseudonymized, not rejected.
   *
   * @param request source intelligence request
   * @return validation outcome
   */
  public PrivacyValidationResult scanOriginal(IntelligenceRequest request) {
    var violations = new ArrayList<PrivacyViolation>();
    for (var finding : request.findings()) {
      checkSecret("title", finding.title(), violations);
      checkSecret("description", finding.description(), violations);
      checkSecret("observed", finding.evidence().observed(), violations);
      checkSecret("expected", finding.evidence().expected(), violations);
      for (var detail : finding.evidence().details().values()) {
        checkSecret("details", detail, violations);
      }
    }
    return new PrivacyValidationResult(true, violations);
  }

  /**
   * Returns whether untrusted model free text contains an obvious raw identifier or secret.
   *
   * @param value model text
   * @return true when unsafe
   */
  public boolean containsUnsafeText(String value) {
    if (value == null) {
      return false;
    }
    return SECRET.matcher(value).find()
        || ACCOUNT_ID.matcher(value).find()
        || RAW_RESOURCE_ID.matcher(value).find()
        || ARN.matcher(value).find()
        || EMAIL.matcher(value).find()
        || IPV4.matcher(value).find();
  }

  private void check(
      String field, String value, List<PrivacyViolation> violations, boolean checkHostname) {
    if (CONTROL.matcher(value).find()) {
      violations.add(
          new PrivacyViolation(
              PrivacyViolation.CONTROL_CHARACTER, field, "control characters are not allowed"));
    }
    if (SECRET.matcher(value).find()) {
      violations.add(
          new PrivacyViolation(PrivacyViolation.SECRET_PATTERN, field, "secret-like value found"));
    }
    if (ACCOUNT_ID.matcher(value).find()) {
      violations.add(
          new PrivacyViolation(
              PrivacyViolation.RAW_ACCOUNT_ID, field, "raw account identifier found"));
    }
    if (RAW_RESOURCE_ID.matcher(value).find()) {
      violations.add(
          new PrivacyViolation(
              PrivacyViolation.RAW_RESOURCE_ID, field, "raw resource identifier found"));
    }
    if (ARN.matcher(value).find()) {
      violations.add(
          new PrivacyViolation(PrivacyViolation.RAW_RESOURCE_ID, field, "raw ARN found"));
    }
    if (EMAIL.matcher(value).find()) {
      violations.add(
          new PrivacyViolation(PrivacyViolation.EMAIL_ADDRESS, field, "email address found"));
    }
    if (IPV4.matcher(value).find()) {
      violations.add(new PrivacyViolation(PrivacyViolation.IP_ADDRESS, field, "IP address found"));
    }
    if (checkHostname && HOSTNAME.matcher(value).find()) {
      violations.add(new PrivacyViolation(PrivacyViolation.HOSTNAME, field, "hostname found"));
    }
  }

  private void checkSecret(String field, String value, List<PrivacyViolation> violations) {
    if (SECRET.matcher(value).find()) {
      violations.add(
          new PrivacyViolation(
              PrivacyViolation.SECRET_PATTERN,
              field,
              "secret-like source value found; payload rejected"));
    }
  }
}
