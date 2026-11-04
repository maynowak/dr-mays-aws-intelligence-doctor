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
  void unknownProviderIsEmpty() {
    assertThat(new ProviderRegistry().find("missing")).isEmpty();
  }
}
