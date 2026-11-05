package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenCodeSandboxPolicyTest {

  @Test
  void blockedPolicyIsNotReady() {
    assertThat(OpenCodeSandboxPolicy.blocked().ready()).isFalse();
  }

  @Test
  void allControlsAreRequired() {
    var ready = OpenCodeSandboxPolicy.allControlsSatisfiedForTest();
    assertThat(ready.ready()).isTrue();

    assertThat(
            new OpenCodeSandboxPolicy(
                    true, true, true, true, true, true, true, true, true, true, true, false)
                .ready())
        .isFalse();
  }

  @Test
  void disabledAdapterCannotRunEvenWithLiveFlagWhenSandboxBlocked() {
    var adapter =
        new OpenCodeCliAdapter(
            true,
            java.nio.file.Path.of("/usr/bin/opencode"),
            java.nio.file.Path.of("/tmp/opencode/home"),
            java.nio.file.Path.of("/tmp/opencode/work"),
            command -> new CliProcessResult(0, "{}", ""));
    var request =
        ProviderTestFixtures.request(
            "opencode",
            "openai/gpt",
            ProviderCredentialReference.environment("opencode", "DR_MAYS_OPENCODE_KEY"));

    assertThat(adapter.invoke(request).status()).isEqualTo(ProviderExecutionStatus.DISABLED);
  }
}
