package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProviderModelPolicyTest {

  @Test
  void mockOnlyAllowsExactMockModel() {
    var policy = ProviderModelPolicy.mockOnly();

    assertThat(policy.allows("mock", "deterministic-v1")).isTrue();
    assertThat(policy.allows("mock", "other")).isFalse();
    assertThat(policy.allows("other", "deterministic-v1")).isFalse();
  }

  @Test
  void noWildcardOrPrefixMatching() {
    var policy = ProviderModelPolicy.of(Map.of("mock", Set.of("deterministic-v1")));

    assertThat(policy.allows("mock", "deterministic-v10")).isFalse();
    assertThat(policy.allows("mock", "*")).isFalse();
  }

  @Test
  void allowlistIsImmutable() {
    var policy = ProviderModelPolicy.mockOnly();

    assertThatThrownBy(() -> policy.allowedModels().put("other", Set.of("m")))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void emptyPolicyDeniesEverything() {
    assertThat(ProviderModelPolicy.of(Map.of()).allows("mock", "deterministic-v1")).isFalse();
  }

  @Test
  void nullModelsAreRejected() {
    assertThatThrownBy(() -> ProviderModelPolicy.of(Map.of("mock", null)))
        .isInstanceOf(NullPointerException.class);
  }
}
