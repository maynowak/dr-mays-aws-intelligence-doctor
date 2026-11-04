package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Projects verified intelligence into the only provider-facing, allowlisted payload shape. */
public final class PrivacyProcessor {

  private static final String EXCLUDED = "[excluded]";
  private static final Pattern ACCOUNT_ID = Pattern.compile("\\b\\d{12}\\b");
  private static final Pattern ARN = Pattern.compile("arn:(?:aws|aws-us-gov|aws-cn):[^\\s]+");
  private static final Pattern AWS_ID =
      Pattern.compile("\\b(sg|vpc|subnet|i|vol)-[0-9a-fA-F]{8,}\\b");
  private static final Pattern EMAIL =
      Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
  private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
  private static final Pattern IPV6 =
      Pattern.compile("\\b(?:[0-9A-Fa-f]{0,4}:){2,}[0-9A-Fa-f]{0,4}\\b");
  private static final Pattern HOSTNAME =
      Pattern.compile("\\b(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}\\b");
  private static final Pattern SECRET =
      Pattern.compile(
          "(?i)(?:AKIA|ASIA)[0-9A-Z]{16}"
              + "|ghp_[A-Za-z0-9]{20,}"
              + "|sk-[A-Za-z0-9]{20,}"
              + "|xox[baprs]-[A-Za-z0-9-]{10,}"
              + "|-----BEGIN [A-Z ]+PRIVATE KEY-----"
              + "|Bearer\\s+[A-Za-z0-9._-]{12,}");

  private final PrivacyPolicy policy;
  private final PrivacyValidator validator;
  private final ObjectMapper objectMapper;
  private final Supplier<AliasMapping> aliasFactory;

  /**
   * Creates the default processor.
   *
   * @param policy privacy policy
   */
  public PrivacyProcessor(PrivacyPolicy policy) {
    this(policy, new ObjectMapper(), AliasMapping::random);
  }

  /**
   * Creates a processor with explicit serialization and alias dependencies for tests.
   *
   * @param policy privacy policy
   * @param objectMapper JSON mapper
   * @param aliasFactory per-preview alias-map factory
   */
  public PrivacyProcessor(
      PrivacyPolicy policy, ObjectMapper objectMapper, Supplier<AliasMapping> aliasFactory) {
    this.policy = Objects.requireNonNull(policy, "policy");
    this.validator = new PrivacyValidator();
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.aliasFactory = Objects.requireNonNull(aliasFactory, "aliasFactory");
  }

  /**
   * Projects a verified request into a sanitized payload and preview metadata.
   *
   * @param request verified intelligence request
   * @return local processing result including alias map
   */
  public PrivacyProcessingResult process(IntelligenceRequest request) {
    Objects.requireNonNull(request, "request");
    var aliases = aliasFactory.get();
    var findings =
        request.findings().stream()
            .sorted(
                Comparator.comparing(
                        (io.github.maynowak.drmays.core.finding.DiagnosticFinding finding) ->
                            finding.resource().canonicalId())
                    .thenComparing(finding -> finding.ruleId().value()))
            .map(finding -> sanitizeFinding(finding, aliases))
            .toList();
    var relationships =
        request.relationships().stream()
            .sorted(
                Comparator.comparing((io.github.maynowak.drmays.core.dna.ResourceEdge edge) ->
                        edge.from().canonicalId())
                    .thenComparing(edge -> edge.to().canonicalId())
                    .thenComparing(edge -> edge.type().name()))
            .map(edge -> sanitizeRelationship(edge, aliases))
            .toList();
    var uncertainties = new ArrayList<String>();
    if (request.completeness() == GraphCompleteness.PARTIAL
        && policy.allows(PrivacyField.UNCERTAINTIES)) {
      uncertainties.add("Discovery data is partial; unknown resources are not covered.");
    }
    var payload =
        new SanitizedExpertiseRequest(
            SanitizedExpertiseRequest.CURRENT_PAYLOAD_VERSION,
            policy.version(),
            findings,
            relationships,
            fieldText(PrivacyField.COMPLETENESS, request.completeness().name(), aliases),
            uncertainties.stream().map(value -> sanitize(value, aliases)).toList());
    var json = serialize(payload);
    var validation =
        validator
            .validate(payload, policy)
            .merge(validator.scanOriginal(request))
            .merge(validator.validateSize(json, policy));
    return new PrivacyProcessingResult(
        payload, aliases, policy, validation, json, hash(json), decisions());
  }

  private SanitizedFinding sanitizeFinding(
      io.github.maynowak.drmays.core.finding.DiagnosticFinding finding, AliasMapping aliases) {
    var alias =
        policy.allows(PrivacyField.RESOURCE_ALIAS)
            ? aliases.aliasFor(finding.resource())
            : EXCLUDED;
    return new SanitizedFinding(
        fieldText(PrivacyField.RULE_ID, finding.ruleId().value(), aliases),
        fieldText(PrivacyField.RULE_VERSION, finding.ruleVersion(), aliases),
        fieldText(PrivacyField.SEVERITY, finding.severity().name(), aliases),
        alias,
        fieldText(PrivacyField.RESOURCE_TYPE, finding.resource().resourceType(), aliases),
        policy.allows(PrivacyField.REGION_CLASS) ? "REGION" : EXCLUDED,
        fieldText(PrivacyField.TITLE, finding.title(), aliases),
        fieldText(PrivacyField.DESCRIPTION, finding.description(), aliases),
        fieldText(PrivacyField.EVIDENCE_SOURCE, finding.evidence().source(), aliases),
        fieldText(PrivacyField.EVIDENCE_OBSERVED, finding.evidence().observed(), aliases),
        fieldText(PrivacyField.EVIDENCE_EXPECTED, finding.evidence().expected(), aliases));
  }

  private SanitizedRelationship sanitizeRelationship(
      io.github.maynowak.drmays.core.dna.ResourceEdge edge, AliasMapping aliases) {
    return new SanitizedRelationship(
        fieldText(PrivacyField.RELATIONSHIP_TYPE, edge.type().name(), aliases),
        policy.allows(PrivacyField.RESOURCE_ALIAS) ? aliases.aliasFor(edge.from()) : EXCLUDED,
        policy.allows(PrivacyField.RESOURCE_ALIAS) ? aliases.aliasFor(edge.to()) : EXCLUDED,
        fieldText(PrivacyField.EVIDENCE_SOURCE, edge.evidence().evidence().source(), aliases),
        fieldText(PrivacyField.EVIDENCE_OBSERVED, edge.evidence().evidence().observed(), aliases));
  }

  private String fieldText(PrivacyField field, String raw, AliasMapping aliases) {
    if (!policy.allows(field)) {
      return EXCLUDED;
    }
    return sanitize(raw, aliases);
  }

  private static String sanitize(String raw, AliasMapping aliases) {
    var value = Objects.requireNonNull(raw, "raw");
    value = SECRET.matcher(value).replaceAll("[SECRET_REDACTED]");
    value = ACCOUNT_ID.matcher(value).replaceAll(match -> aliases.aliasFor("acct", match.group()));
    value = ARN.matcher(value).replaceAll(match -> aliases.aliasFor("arn", match.group()));
    value = AWS_ID.matcher(value).replaceAll(match -> aliasAwsId(match, aliases));
    value = EMAIL.matcher(value).replaceAll("[email]");
    value = IPV4.matcher(value).replaceAll("[ip]");
    value = IPV6.matcher(value).replaceAll("[ip]");
    value = HOSTNAME.matcher(value).replaceAll("[host]");
    return value.replaceAll("\\p{Cntrl}", " ").strip();
  }

  private static String aliasAwsId(MatchResult match, AliasMapping aliases) {
    var value = match.group();
    var prefix = value.substring(0, value.indexOf('-'));
    return aliases.aliasFor(prefix, value);
  }

  private String serialize(SanitizedExpertiseRequest payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JacksonException exception) {
      throw new IllegalStateException("cannot serialize sanitized payload", exception);
    }
  }

  private static String hash(String json) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private List<PrivacyFieldDecision> decisions() {
    return List.of(
        decision(PrivacyField.RULE_ID, PrivacyClassification.PUBLIC, "deterministic rule code"),
        decision(PrivacyField.RULE_VERSION, PrivacyClassification.PUBLIC, "rule version"),
        decision(PrivacyField.SEVERITY, PrivacyClassification.PUBLIC, "finding severity"),
        decision(
            PrivacyField.RESOURCE_TYPE, PrivacyClassification.INTERNAL, "coarse resource type"),
        decision(PrivacyField.REGION_CLASS, PrivacyClassification.QUASI_IDENTIFIER, "coarse only"),
        decision(PrivacyField.RESOURCE_ALIAS, PrivacyClassification.IDENTIFIER, "local pseudonym"),
        decision(PrivacyField.TITLE, PrivacyClassification.INTERNAL, "sanitized rule text"),
        decision(PrivacyField.DESCRIPTION, PrivacyClassification.INTERNAL, "sanitized rule text"),
        decision(
            PrivacyField.EVIDENCE_SOURCE, PrivacyClassification.INTERNAL, "evidence provenance"),
        decision(
            PrivacyField.EVIDENCE_OBSERVED, PrivacyClassification.SENSITIVE, "sanitized value"),
        decision(
            PrivacyField.EVIDENCE_EXPECTED, PrivacyClassification.SENSITIVE, "sanitized value"),
        decision(
            PrivacyField.RELATIONSHIP_TYPE, PrivacyClassification.PUBLIC, "verified edge type"),
        decision(PrivacyField.COMPLETENESS, PrivacyClassification.PUBLIC, "discovery completeness"),
        decision(
            PrivacyField.UNCERTAINTIES, PrivacyClassification.INTERNAL, "sanitized uncertainty"));
  }

  private PrivacyFieldDecision decision(
      PrivacyField field, PrivacyClassification classification, String reason) {
    var included = policy.allows(field);
    return new PrivacyFieldDecision(
        field, classification, included, included ? reason : "excluded by " + policy.version());
  }
}
