package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProviderEnablementTest {

  @Test
  void mockOnlyIsTheDefault() {
    assertThat(ProviderEnablement.mockOnly().isEnabled("mock")).isTrue();
    assertThat(ProviderEnablement.mockOnly().isEnabled("fake")).isFalse();
  }

  @Test
  void registrationDoesNotEnableProvider() {
    var registry = new ProviderRegistry(List.of(new FakeProvider("fake")));
    var enablement = ProviderEnablement.mockOnly();

    assertThat(enablement.isEnabled("fake")).isFalse();
  }

  @Test
  void explicitEnablementRequiresRegisteredProvider() {
    var registry = new ProviderRegistry(List.of(new FakeProvider("fake")));

    assertThat(ProviderEnablement.of(registry, Set.of("fake")).isEnabled("fake")).isTrue();
    assertThatThrownBy(() -> ProviderEnablement.of(registry, Set.of("missing")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void wildcardAndMalformedIdsAreRejected() {
    var registry = new ProviderRegistry(List.of(new FakeProvider("fake")));

    assertThatThrownBy(() -> ProviderEnablement.of(registry, Set.of("*")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ProviderEnablement.of(registry, Set.of("f*")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ProviderEnablement.of(registry, Set.of("")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void enabledSetIsImmutable() {
    assertThatThrownBy(() -> ProviderEnablement.mockOnly().enabledProviderIds().add("fake"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static final class FakeProvider implements ModelProvider {
    private final ModelProviderDescriptor descriptor;

    private FakeProvider(String id) {
      descriptor =
          new ModelProviderDescriptor(
              id,
              "Fake",
              ProviderKind.EXTERNAL_MODEL,
              new ProviderCapabilities(true, false, false, 1000, 100),
              false,
              true,
              false);
    }

    @Override
    public ModelProviderDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
      return ModelInvocationResult.success("fake", "m", "{}", 1);
    }
  }
}
