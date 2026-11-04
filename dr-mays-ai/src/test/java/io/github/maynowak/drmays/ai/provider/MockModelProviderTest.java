package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.ai.privacy.SanitizedExpertiseRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockModelProviderTest {

  private final MockModelProvider provider = new MockModelProvider();

  @Test
  void returnsDeterministicStructuredContent() {
    var request =
        ProviderTestFixtures.request(
            "mock", "deterministic-v1", ProviderCredentialReference.none("mock"));

    var result = provider.invoke(request);

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(result.content())
        .contains("\"summary\"")
        .contains("NET-PUBLIC-001")
        .contains("res_safe")
        .doesNotContain("123456789012");
    assertThat(result.inputChars()).isEqualTo(ProviderTestFixtures.PAYLOAD_JSON.length());
  }

  @Test
  void handlesEmptyFindingSet() {
    var payload =
        new SanitizedExpertiseRequest(
            "privacy-payload/v1", "privacy-v2", List.of(), List.of(), "COMPLETE", List.of());
    var request =
        new ModelInvocationRequest(
            "mock",
            "deterministic-v1",
            "standard",
            payload,
            ProviderTestFixtures.PAYLOAD_JSON,
            ProviderTestFixtures.PAYLOAD_HASH,
            ProviderCredentialReference.none("mock"),
            java.time.Duration.ofSeconds(1),
            64_000);

    var result = provider.invoke(request);

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(result.content()).contains("No deterministic findings");
  }

  @Test
  void rejectsOutputAboveConfiguredLimit() {
    var request =
        ProviderTestFixtures.request(
            "mock",
            "deterministic-v1",
            ProviderTestFixtures.PAYLOAD_JSON,
            ProviderCredentialReference.none("mock"),
            1);

    var result = provider.invoke(request);

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }
}
