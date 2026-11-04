package io.github.maynowak.drmays.ai.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PrivacyValidatorTest {

  private final PrivacyValidator validator = new PrivacyValidator();
  private final PrivacyPolicy policy = PrivacyPolicy.defaultPolicy();

  @Test
  void acceptsSanitizedPayload() {
    var payload = PrivacyTestFixtures.processed().payload();

    assertThat(validator.validate(payload, policy).valid()).isTrue();
  }

  @Test
  void rejectsRawAccountNumber() {
    var result = validator.validate(payload(withUniqueTitle("account 123456789012")), policy);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.RAW_ACCOUNT_ID));
  }

  @Test
  void rejectsRawSecurityGroupAndVpcIdentifiers() {
    var result = validator.validate(payload(withObserved("sg-0abc1234 vpc-0def5678")), policy);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.RAW_RESOURCE_ID));
  }

  @Test
  void rejectsEmailAddress() {
    var result = validator.validate(payload(withObserved("owner=alice@corp.example")), policy);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.EMAIL_ADDRESS));
  }

  @Test
  void rejectsIpAddressAndHostname() {
    var result =
        validator.validate(
            payload(withObserved("host=db.internal.example.com ip=203.0.113.7")), policy);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.IP_ADDRESS));
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.HOSTNAME));
  }

  @Test
  void rejectsSecretPattern() {
    var result = validator.validate(payload(withObserved("AKIAABCDEFGHIJKLMNOP")), policy);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.SECRET_PATTERN));
  }

  @Test
  void rejectsControlCharacters() {
    var result = validator.validate(payload(withObserved("line\nbreak")), policy);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.CONTROL_CHARACTER));
  }

  @Test
  void rejectsPolicyVersionMismatch() {
    var other =
        new PrivacyPolicy("privacy-other", policy.allowedFields(), policy.maxPayloadChars());

    var result = validator.validate(PrivacyTestFixtures.processed().payload(), other);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.INVALID_FIELD));
  }

  @Test
  void rejectsOversizedPayload() {
    var small = new PrivacyPolicy("small", policy.allowedFields(), 1);

    var result = validator.validateSize("{}", small);

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.PAYLOAD_TOO_LARGE));
  }

  @Test
  void detectsSecretInOriginalSource() {
    var result =
        validator.scanOriginal(
            PrivacyTestFixtures.requestWithObserved("token=AKIAABCDEFGHIJKLMNOP"));

    assertThat(result.valid()).isFalse();
    assertThat(result.violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.SECRET_PATTERN));
  }

  @Test
  void unsafeTextDetectionCatchesIdentifiers() {
    assertThat(validator.containsUnsafeText("account 123456789012")).isTrue();
    assertThat(validator.containsUnsafeText("sg-0abc1234")).isTrue();
    assertThat(validator.containsUnsafeText("alice@corp.example")).isTrue();
    assertThat(validator.containsUnsafeText("203.0.113.7")).isTrue();
    assertThat(validator.containsUnsafeText("AKIAABCDEFGHIJKLMNOP")).isTrue();
    assertThat(validator.containsUnsafeText("The finding is HIGH.")).isFalse();
  }

  private static SanitizedFinding withUniqueTitle(String title) {
    return new SanitizedFinding(
        "NET-PUBLIC-001",
        "1.0",
        "HIGH",
        "res_safe",
        "security-group",
        "REGION",
        title,
        "description",
        "resource-attribute",
        "ingress.public=true",
        "ingress.public=false");
  }

  private static SanitizedFinding withObserved(String observed) {
    return new SanitizedFinding(
        "NET-PUBLIC-001",
        "1.0",
        "HIGH",
        "res_safe",
        "security-group",
        "REGION",
        "Public ingress",
        "description",
        "resource-attribute",
        observed,
        "ingress.public=false");
  }

  private static SanitizedExpertiseRequest payload(SanitizedFinding finding) {
    return new SanitizedExpertiseRequest(
        SanitizedExpertiseRequest.CURRENT_PAYLOAD_VERSION,
        "privacy-v1",
        List.of(finding),
        List.of(),
        "COMPLETE",
        List.of());
  }
}
