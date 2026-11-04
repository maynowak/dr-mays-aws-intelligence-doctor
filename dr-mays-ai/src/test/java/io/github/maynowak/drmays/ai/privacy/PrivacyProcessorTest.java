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
    var processed = PrivacyTestFixtures.processed();
    var finding = processed.payload().findings().getFirst();
    var relationship = processed.payload().relationships().getFirst();

    assertThat(finding.resourceAlias()).isEqualTo(relationship.fromAlias());
    assertThat(processed.aliasMapping().resolveResource(finding.resourceAlias())).contains(SG);
    assertThat(processed.aliasMapping().resolveResource(relationship.toAlias())).contains(VPC);
  }

  @Test
  void usesCoarseRegionInsteadOfRawRegion() {
    var processed = PrivacyTestFixtures.processed();

    assertThat(processed.payloadJson()).doesNotContain("eu-west-1");
    assertThat(processed.payload().findings().getFirst().regionClass()).isEqualTo("REGION");
  }

  @Test
  void marksPartialDiscoveryAsUncertainty() {
    var processed = PrivacyTestFixtures.partialProcessed();

    assertThat(processed.payload().completeness()).isEqualTo("PARTIAL");
    assertThat(processed.payload().uncertainties())
        .contains("Discovery data is partial; unknown resources are not covered.");
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
