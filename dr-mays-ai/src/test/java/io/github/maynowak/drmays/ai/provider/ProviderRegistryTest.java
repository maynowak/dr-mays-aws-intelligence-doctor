package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderRegistryTest {

  private final MockModelProvider mock = new MockModelProvider();

  @Test
  void registersAndFindsProvider() {
    var registry = new ProviderRegistry(List.of(mock));

    assertThat(registry.find("mock")).contains(mock);
    assertThat(registry.ids()).containsExactly("mock");
    assertThat(registry.descriptors())
        .extracting(ModelProviderDescriptor::id)
        .containsExactly("mock");
  }

  @Test
  void rejectsDuplicateProvider() {
    var registry = new ProviderRegistry();
    registry.register(mock);

    assertThatThrownBy(() -> registry.register(mock)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void containsReportsRegistration() {
    var registry = new ProviderRegistry(List.of(mock));

    assertThat(registry.contains("mock")).isTrue();
    assertThat(registry.contains("missing")).isFalse();
  }

  @Test
  void unknownProviderIsEmpty() {
    assertThat(new ProviderRegistry().find("missing")).isEmpty();
  }

  @Test
  void rejectsMockIdFromNonMockImplementation() {
    var impostor =
        new ModelProvider() {
          @Override
          public ModelProviderDescriptor descriptor() {
            return ModelProviderDescriptor.mock();
          }

          @Override
          public ModelInvocationResult invoke(ModelInvocationRequest request) {
            return ModelInvocationResult.success("mock", "deterministic-v1", "{}", 1);
          }
        };

    assertThatThrownBy(() -> new ProviderRegistry().register(impostor))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
