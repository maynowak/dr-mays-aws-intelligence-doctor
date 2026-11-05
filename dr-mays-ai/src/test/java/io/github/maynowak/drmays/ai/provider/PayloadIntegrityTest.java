package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.ai.privacy.SanitizedExpertiseRequest;
import org.junit.jupiter.api.Test;

class PayloadIntegrityTest {

  private final SanitizedExpertiseRequest payload = ProviderTestFixtures.payload();

  @Test
  void canonicalJsonMatchesProviderFixture() {
    assertThat(PayloadIntegrity.canonicalJson(payload))
        .isEqualTo(ProviderTestFixtures.PAYLOAD_JSON);
  }

  @Test
  void hashIsStableAndLowercaseHex() {
    var hash = PayloadIntegrity.hash(ProviderTestFixtures.PAYLOAD_JSON);
    assertThat(hash).hasSize(64).isEqualTo(hash.toLowerCase());
    assertThat(PayloadIntegrity.hash(ProviderTestFixtures.PAYLOAD_JSON)).isEqualTo(hash);
  }

  @Test
  void detectsJsonSubstitution() {
    assertThat(PayloadIntegrity.matches(payload, ProviderTestFixtures.PAYLOAD_JSON)).isTrue();
    assertThat(PayloadIntegrity.matches(payload, "{}")).isFalse();
  }
}
