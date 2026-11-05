package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProviderCredentialBindingTest {

  @Test
  void noneBindingIsSyntheticAndRedacted() {
    var binding = ProviderCredentialBinding.none("mock");

    assertThat(binding.version()).isZero();
    assertThat(binding.trustedFingerprint()).isEmpty();
    assertThat(binding.toString()).doesNotContain("reference=none").contains("reference=redacted");
  }

  @Test
  void versionAndProviderAreBound() {
    var binding =
        new ProviderCredentialBinding("binding-1", "fake", "DR_MAYS_FAKE_KEY", 2, "a".repeat(64));

    assertThat(binding.version()).isEqualTo(2);
    assertThat(binding.providerId()).isEqualTo("fake");
  }

  @Test
  void rejectsNegativeVersionAndInvalidFingerprint() {
    assertThatThrownBy(() -> new ProviderCredentialBinding("b", "fake", "ref", -1, ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ProviderCredentialBinding("b", "fake", "ref", 1, "client-claimed"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rotationChangesVersion() {
    var v1 = new ProviderCredentialBinding("b", "fake", "ref", 1, "");
    var v2 = new ProviderCredentialBinding("b", "fake", "ref", 2, "");

    assertThat(v1.version()).isNotEqualTo(v2.version());
  }
}
