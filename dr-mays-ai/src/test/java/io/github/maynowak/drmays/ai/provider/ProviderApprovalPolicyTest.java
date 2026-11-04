package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.ai.privacy.PrivacyGate;
import io.github.maynowak.drmays.ai.privacy.PrivacyPolicy;
import io.github.maynowak.drmays.ai.privacy.PrivacyProcessor;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderApprovalPolicyTest {

  @Test
  void exactMockIsExempt() {
    var registry = new ProviderRegistry(List.of(new MockModelProvider()));

    assertThat(ProviderApprovalPolicy.forRegistry(registry).requiresApproval("mock")).isFalse();
  }

  @Test
  void unknownProviderRequiresApproval() {
    assertThat(ProviderApprovalPolicy.strict().requiresApproval("unknown")).isTrue();
    assertThat(ProviderApprovalPolicy.forRegistry(new ProviderRegistry()).requiresApproval("mock"))
        .isTrue();
  }

  @Test
  void descriptorFlagsCannotCreateMockExemption() {
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
    var registry = new ProviderRegistry();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.register(impostor))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void realMockStillGetsAuthorizationAtRouter() {
    var source = new IntelligenceRequest(List.of(), List.of(), GraphCompleteness.COMPLETE);
    var processed = new PrivacyProcessor(PrivacyPolicy.defaultPolicy()).process(source);
    var registry = new ProviderRegistry(List.of(new MockModelProvider()));
    var gate =
        new PrivacyGate(
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(5),
            java.time.Clock.systemUTC(),
            ProviderApprovalPolicy.forRegistry(registry));
    var preview =
        gate.register(
            source,
            processed,
            "mock",
            "deterministic-v1",
            "standard",
            ProviderCredentialBinding.none("mock"));
    var invocation = gate.consume(preview.previewId(), null).invocation().orElseThrow();

    assertThat(invocation.authorization()).isNotNull();
    assertThat(invocation.authorization().providerId()).isEqualTo("mock");
  }
}
