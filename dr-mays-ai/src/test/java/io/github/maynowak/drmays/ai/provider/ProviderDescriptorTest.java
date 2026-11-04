package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProviderDescriptorTest {

  @Test
  void mockDescriptorIsEnabledAndRequiresNoCredential() {
    var descriptor = ModelProviderDescriptor.mock();

    assertThat(descriptor.id()).isEqualTo("mock");
    assertThat(descriptor.kind()).isEqualTo(ProviderKind.DETERMINISTIC_MOCK);
    assertThat(descriptor.capabilities().localOnly()).isTrue();
    assertThat(descriptor.capabilities().toolsAllowed()).isFalse();
    assertThat(descriptor.requiresCredential()).isFalse();
    assertThat(descriptor.requiresApproval()).isFalse();
    assertThat(descriptor.enabledByDefault()).isTrue();
  }

  @Test
  void ollamaDescriptorIsDisabledAndRequiresApproval() {
    var descriptor = ModelProviderDescriptor.ollama();

    assertThat(descriptor.id()).isEqualTo("ollama");
    assertThat(descriptor.kind()).isEqualTo(ProviderKind.LOCAL_MODEL);
    assertThat(descriptor.requiresApproval()).isTrue();
    assertThat(descriptor.enabledByDefault()).isFalse();
  }

  @Test
  void openCodeDescriptorIsDisabledAndRequiresCredential() {
    var descriptor = ModelProviderDescriptor.openCode();

    assertThat(descriptor.id()).isEqualTo("opencode");
    assertThat(descriptor.kind()).isEqualTo(ProviderKind.CLI_AGENT);
    assertThat(descriptor.requiresCredential()).isTrue();
    assertThat(descriptor.requiresApproval()).isTrue();
    assertThat(descriptor.enabledByDefault()).isFalse();
  }

  @Test
  void rejectsInvalidProviderId() {
    assertThatThrownBy(() ->
            new ModelProviderDescriptor(
                "Invalid ID",
                "name",
                ProviderKind.EXTERNAL_MODEL,
                new ProviderCapabilities(true, false, false, 1, 1),
                true,
                true,
                false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositiveCapabilities() {
    assertThatThrownBy(() -> new ProviderCapabilities(true, true, false, 0, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
