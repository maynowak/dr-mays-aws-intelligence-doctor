package io.github.maynowak.drmays.ai.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrivacyPolicyTest {

  @Test
  void defaultPolicyAllowsEverySupportedField() {
    var policy = PrivacyPolicy.defaultPolicy();

    assertThat(policy.version()).isEqualTo("privacy-v1");
    assertThat(policy.allowedFields()).containsExactlyInAnyOrder(PrivacyField.values());
    assertThat(policy.maxPayloadChars()).isEqualTo(32_000);
  }

  @Test
  void rejectsBlankVersion() {
    assertThatThrownBy(() ->
            new PrivacyPolicy(
                " ", EnumSet.allOf(PrivacyField.class), PrivacyPolicy.DEFAULT_MAX_PAYLOAD_CHARS))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsEmptyAllowlist() {
    assertThatThrownBy(() -> new PrivacyPolicy("p", Set.of(), 100))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositivePayloadLimit() {
    assertThatThrownBy(() -> new PrivacyPolicy("p", EnumSet.allOf(PrivacyField.class), 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reportsFieldExclusion() {
    var policy =
        new PrivacyPolicy(
            "privacy-test",
            EnumSet.complementOf(EnumSet.of(PrivacyField.TITLE)),
            PrivacyPolicy.DEFAULT_MAX_PAYLOAD_CHARS);

    assertThat(policy.allows(PrivacyField.TITLE)).isFalse();
    assertThat(policy.allows(PrivacyField.DESCRIPTION)).isTrue();
  }
}
