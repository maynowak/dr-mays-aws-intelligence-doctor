package io.github.maynowak.drmays.ai.privacy;

import static io.github.maynowak.drmays.ai.privacy.PrivacyTestFixtures.SG;
import static io.github.maynowak.drmays.ai.privacy.PrivacyTestFixtures.VPC;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class PrivacyProcessorTest {

  @Test
  void removesRawAccountAndResourceIdentifiers() {
    var processed = PrivacyTestFixtures.processed();

    assertThat(processed.payloadJson())
        .doesNotContain("123456789012")
        .doesNotContain("vpc-0def5678")
        .doesNotContain("sg-0abc1234")
        .doesNotContain("alice@corp.example")
        .doesNotContain("db.internal.example.com")
        .doesNotContain("203.0.113.7");
  }

  @Test
  void keepsResourceAliasesConsistentAcrossFindingsAndRelationships() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyTestFixtures.relationshipPolicy())
            .process(PrivacyTestFixtures.request());
    var finding = processed.payload().findings().getFirst();
    var relationship = processed.payload().relationships().getFirst();

    assertThat(finding.resourceAlias()).isEqualTo(relationship.fromAlias());
    assertThat(processed.aliasMapping().resolveResource(finding.resourceAlias())).contains(SG);
    assertThat(processed.aliasMapping().resolveResource(relationship.toAlias())).contains(VPC);
  }

  @Test
  void removesPlaceholderStyleResourceIdentifiers() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(PrivacyTestFixtures.requestWithObserved("vpc.id=vpc-unknown sg-unknown"));

    assertThat(processed.payloadJson()).doesNotContain("vpc-unknown").doesNotContain("sg-unknown");
    assertThat(processed.validation().valid()).isTrue();
  }

  @Test
  void aliasesArbitraryRelationshipEvidenceValues() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyTestFixtures.relationshipPolicy())
            .process(
                PrivacyTestFixtures.requestWithRelationshipObserved(
                    "ingress.referenced.securityGroupIds=prod-app-sg"));

    assertThat(processed.payloadJson())
        .doesNotContain("prod-app-sg")
        .contains("ingress.referenced.securityGroupIds=sg_");
    assertThat(processed.validation().valid()).isTrue();
  }

  @Test
  void aliasesNonVpcRelationshipIdentifiers() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyTestFixtures.relationshipPolicy())
            .process(PrivacyTestFixtures.requestWithRelationshipObserved("vpc.id=igw-0abc1234"));

    assertThat(processed.payloadJson()).doesNotContain("igw-0abc1234");
  }

  @Test
  void aliasesNonStandardArnPartitions() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(
                PrivacyTestFixtures.requestWithObserved(
                    "role=arn:aws-iso:iam::123456789012:role/example"));

    assertThat(processed.payloadJson()).doesNotContain("aws-iso").doesNotContain("123456789012");
    assertThat(processed.validation().valid()).isTrue();
  }

  @Test
  void rejectsExpandedSecretFamilies() {
    for (var secret :
        java.util.List.of(
            "aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
            "token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abc",
            "password=Sup3rS3cret!",
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC",
            "github_pat_11ABCDEFG0123456789_abcdefghijklmnopqrstuvwxyz",
            "glpat-abcdefghijklmnopqrst",
            "AIzaSyA1234567890abcdefghijklmnopqrstu",
            "sk-proj-abcdefghijklmnopqrstuvwx")) {
      var processed =
          PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
              .process(PrivacyTestFixtures.requestWithObserved(secret));
      assertThat(processed.validation().valid()).as(secret).isFalse();
      assertThat(processed.validation().violations())
          .as(secret)
          .anyMatch(violation -> violation.code().equals(PrivacyViolation.SECRET_PATTERN));
    }
  }

  @Test
  void rejectsSecretInRelationshipEvidence() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(
                PrivacyTestFixtures.requestWithRelationshipObserved(
                    "vpc.id=token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abc"));

    assertThat(processed.validation().valid()).isFalse();
    assertThat(processed.validation().violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.SECRET_PATTERN));
  }

  @Test
  void redactsUrls() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(
                PrivacyTestFixtures.requestWithObserved(
                    "endpoint=https://localhost:8080/admin/customer-123"));

    assertThat(processed.payloadJson()).doesNotContain("https://").doesNotContain("customer-123");
  }

  @Test
  void redactsBareHostnameEvidence() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(PrivacyTestFixtures.requestWithObserved("db.internal.example.com"));

    assertThat(processed.payloadJson()).doesNotContain("db.internal.example.com");
  }

  @Test
  void preservesSchemaKeyEvidence() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyTestFixtures.relationshipPolicy())
            .process(PrivacyTestFixtures.request());

    assertThat(processed.payload().findings().getFirst().expected())
        .isEqualTo("ingress.public=false");
    assertThat(processed.payload().relationships().getFirst().observed()).startsWith("vpc.id=");
  }

  @Test
  void redactsHostnameWithPort() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(PrivacyTestFixtures.requestWithObserved("host=db.internal.example.com:5432"));

    assertThat(processed.payloadJson()).doesNotContain("db.internal.example.com:5432");
  }

  @Test
  void preservesAttributeNamesWhileRedactingHostnames() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(
                PrivacyTestFixtures.requestWithObserved(
                    "ingress.public=true vpc.id=vpc-0def5678 host=db.internal.example.com"));
    var observed = processed.payload().findings().getFirst().observed();

    assertThat(observed)
        .contains("ingress.public=true")
        .contains("vpc.id=")
        .doesNotContain("db.internal.example.com");
  }

  @Test
  void excludesRegionByDefault() {
    var processed = PrivacyTestFixtures.processed();

    assertThat(processed.payloadJson()).doesNotContain("eu-west-1");
    assertThat(processed.payload().findings().getFirst().regionClass()).isEqualTo("[excluded]");
  }

  @Test
  void marksPartialDiscoveryAsUncertainty() {
    var processed = PrivacyTestFixtures.partialProcessed();

    assertThat(processed.payload().completeness()).isEqualTo("PARTIAL");
    assertThat(processed.payload().uncertainties())
        .contains("Discovery data is partial; unknown resources are not covered.");
  }

  @Test
  void defaultPolicyOmitsRelationshipsAndFreeText() {
    var processed = PrivacyTestFixtures.processed();
    var finding = processed.payload().findings().getFirst();

    assertThat(processed.payload().relationships()).isEmpty();
    assertThat(finding.title()).isEqualTo("[excluded]");
    assertThat(finding.description()).isEqualTo("[excluded]");
    assertThat(finding.resourceType()).isEqualTo("[excluded]");
    assertThat(finding.regionClass()).isEqualTo("[excluded]");
  }

  @Test
  void decisionsCoverEveryFieldExactlyOnce() {
    var processed = PrivacyTestFixtures.processed();

    assertThat(processed.decisions())
        .extracting(PrivacyFieldDecision::field)
        .containsExactlyInAnyOrder(PrivacyField.values());
  }

  @Test
  void policyVersionIsPartOfTheHash() {
    var fields = java.util.EnumSet.copyOf(PrivacyPolicy.DEFAULT_ALLOWED_FIELDS);
    var other = new PrivacyPolicy("privacy-other", fields, PrivacyPolicy.DEFAULT_MAX_PAYLOAD_CHARS);
    var first =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(PrivacyTestFixtures.request());
    var second = PrivacyTestFixtures.processor(other).process(PrivacyTestFixtures.request());

    assertThat(first.payloadHash()).isNotEqualTo(second.payloadHash());
  }

  @Test
  void relationshipsAreOmittedNotPlaceholderEncoded() {
    var processed = PrivacyTestFixtures.processed();

    assertThat(processed.payload().relationships()).isEmpty();
    assertThat(processed.payloadJson()).doesNotContain("fromAlias").doesNotContain("toAlias");
  }

  @Test
  void decisionsAreVisibleForExcludedFields() {
    var processed = PrivacyTestFixtures.processed();

    assertThat(processed.decisions())
        .anyMatch(decision ->
            decision.field() == PrivacyField.TITLE
                && !decision.included()
                && decision.reason().contains("privacy-v2"));
  }

  @Test
  void producesValidDefaultPayload() {
    var processed = PrivacyTestFixtures.processed();

    assertThat(processed.validation().valid()).isTrue();
    assertThat(processed.decisions()).hasSize(PrivacyField.values().length);
    assertThat(processed.payloadHash()).hasSize(64);
  }

  @Test
  void detectsSecretInOriginalSourceAndRejectsPayload() {
    var processed =
        PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
            .process(PrivacyTestFixtures.requestWithObserved("token=AKIAABCDEFGHIJKLMNOP"));

    assertThat(processed.validation().valid()).isFalse();
    assertThat(processed.validation().violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.SECRET_PATTERN));
  }

  @Test
  void rejectsPayloadAbovePolicyLimit() {
    var policy = new PrivacyPolicy("small", EnumSet.allOf(PrivacyField.class), 20);
    var processed =
        new PrivacyProcessor(
                policy,
                new tools.jackson.databind.ObjectMapper(),
                () -> AliasMapping.withKey(PrivacyTestFixtures.KEY))
            .process(PrivacyTestFixtures.request());

    assertThat(processed.validation().valid()).isFalse();
    assertThat(processed.validation().violations())
        .anyMatch(violation -> violation.code().equals(PrivacyViolation.PAYLOAD_TOO_LARGE));
  }

  @Test
  void excludesFieldsNotAllowedByPolicy() {
    var policy =
        new PrivacyPolicy(
            "no-title",
            EnumSet.complementOf(EnumSet.of(PrivacyField.TITLE)),
            PrivacyPolicy.DEFAULT_MAX_PAYLOAD_CHARS);
    var processed = PrivacyTestFixtures.processor(policy).process(PrivacyTestFixtures.request());

    assertThat(processed.payload().findings().getFirst().title()).isEqualTo("[excluded]");
    assertThat(processed.decisions())
        .anyMatch(decision -> decision.field() == PrivacyField.TITLE && !decision.included());
  }

  @Test
  void hashesSamePayloadDeterministically() {
    var first = PrivacyTestFixtures.processed();
    var second = PrivacyTestFixtures.processed();

    assertThat(first.payloadJson()).isEqualTo(second.payloadJson());
    assertThat(first.payloadHash()).isEqualTo(second.payloadHash());
  }
}
