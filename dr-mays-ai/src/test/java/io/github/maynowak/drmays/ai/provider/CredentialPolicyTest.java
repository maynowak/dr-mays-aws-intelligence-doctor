package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class CredentialPolicyTest {

  @Test
  void defaultPolicyDisablesResolution() {
    var policy = CredentialResolutionPolicy.disabled();

    assertThat(policy.enabled()).isFalse();
    assertThat(policy.allows(CredentialKind.ENV)).isFalse();
    assertThat(policy.allows(CredentialKind.NONE)).isFalse();
  }

  @Test
  void disabledPolicyRejectsAllowedKinds() {
    assertThatThrownBy(() -> new CredentialResolutionPolicy(false, Set.of(CredentialKind.ENV)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void disabledResolverNeverResolvesEnvironmentOrFile() {
    var resolver = new DisabledCredentialResolver();

    assertThat(resolver.resolve(ProviderCredentialReference.none("mock")))
        .isEqualTo(CredentialAccessDecision.NOT_REQUIRED);
    assertThat(
            resolver.resolve(
                ProviderCredentialReference.environment("opencode", "DR_MAYS_OPENCODE_KEY")))
        .isEqualTo(CredentialAccessDecision.DENIED);
  }

  @Test
  void futurePolicyRequiresExplicitKind() {
    var policy = new CredentialResolutionPolicy(true, Set.of(CredentialKind.ENV));

    assertThat(policy.allows(CredentialKind.ENV)).isTrue();
    assertThat(policy.allows(CredentialKind.FILE)).isFalse();
  }
}
