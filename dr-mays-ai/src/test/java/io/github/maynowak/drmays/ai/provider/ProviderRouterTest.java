package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProviderRouterTest {

  private final ModelProviderDescriptor fakeDescriptor =
      new ModelProviderDescriptor(
          "fake",
          "Fake provider",
          ProviderKind.EXTERNAL_MODEL,
          new ProviderCapabilities(true, false, false, 1_000, 100),
          true,
          true,
          false);

  @Test
  void routesMockProvider() {
    var mock = new MockModelProvider();
    var router = new ProviderRouter(new ProviderRegistry(List.of(mock)), Set.of("mock"));

    var result =
        router.route(
            ProviderTestFixtures.request(
                "mock", "deterministic-v1", ProviderCredentialReference.none("mock")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(result.content()).contains("NET-PUBLIC-001").contains("res_safe");
  }

  @Test
  void rejectsUnknownProvider() {
    var router = new ProviderRouter(new ProviderRegistry(), Set.of());

    var result =
        router.route(
            ProviderTestFixtures.request(
                "missing", "m", ProviderCredentialReference.none("missing")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.NOT_ALLOWED);
  }

  @Test
  void rejectsDisabledProvider() {
    var router =
        new ProviderRouter(new ProviderRegistry(List.of(new MockModelProvider())), Set.of());

    var result =
        router.route(
            ProviderTestFixtures.request(
                "mock", "deterministic-v1", ProviderCredentialReference.none("mock")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.DISABLED);
  }

  @Test
  void rejectsMissingCredentialReference() {
    var fake =
        new FakeProvider(fakeDescriptor, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = new ProviderRouter(new ProviderRegistry(List.of(fake)), Set.of("fake"), true);

    var result =
        router.route(
            ProviderTestFixtures.request("fake", "m", ProviderCredentialReference.none("fake")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsCredentialResolutionWhenDisabled() {
    var fake =
        new FakeProvider(fakeDescriptor, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = new ProviderRouter(new ProviderRegistry(List.of(fake)), Set.of("fake"), false);

    var result =
        router.route(
            ProviderTestFixtures.request(
                "fake", "m", ProviderCredentialReference.environment("fake", "DR_MAYS_FAKE_KEY")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void routesExplicitCredentialWhenResolutionIsEnabled() {
    var fake =
        new FakeProvider(fakeDescriptor, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = new ProviderRouter(new ProviderRegistry(List.of(fake)), Set.of("fake"), true);

    var result =
        router.route(
            ProviderTestFixtures.request(
                "fake", "m", ProviderCredentialReference.environment("fake", "DR_MAYS_FAKE_KEY")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(fake.calls()).isEqualTo(1);
  }

  @Test
  void rejectsOversizedPayloadBeforeProviderCall() {
    var fake =
        new FakeProvider(fakeDescriptor, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = new ProviderRouter(new ProviderRegistry(List.of(fake)), Set.of("fake"));
    var request =
        ProviderTestFixtures.request(
            "fake", "m", "x".repeat(2_000), ProviderCredentialReference.none("fake"), 100);

    assertThat(router.route(request).status()).isEqualTo(ProviderExecutionStatus.PRIVACY_REJECTED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsOversizedProviderOutput() {
    var fake =
        new FakeProvider(
            fakeDescriptor, ModelInvocationResult.success("fake", "m", "x".repeat(200), 1));
    var router = new ProviderRouter(new ProviderRegistry(List.of(fake)), Set.of("fake"), true);

    var result =
        router.route(
            ProviderTestFixtures.request(
                "fake", "m", ProviderCredentialReference.environment("fake", "DR_MAYS_FAKE_KEY")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void mapsProviderExceptionToUnavailableWithoutFallback() {
    var fake = new FakeProvider(fakeDescriptor, null);
    fake.throwOnInvoke = true;
    var router = new ProviderRouter(new ProviderRegistry(List.of(fake)), Set.of("fake"), true);

    var result =
        router.route(
            ProviderTestFixtures.request(
                "fake", "m", ProviderCredentialReference.environment("fake", "DR_MAYS_FAKE_KEY")));

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.UNAVAILABLE);
    assertThat(fake.calls()).isEqualTo(1);
  }

  private static final class FakeProvider implements ModelProvider {

    private final ModelProviderDescriptor descriptor;
    private final ModelInvocationResult result;
    private int calls;
    private boolean throwOnInvoke;

    private FakeProvider(ModelProviderDescriptor descriptor, ModelInvocationResult result) {
      this.descriptor = descriptor;
      this.result = result;
    }

    @Override
    public ModelProviderDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
      calls++;
      if (throwOnInvoke) {
        throw new IllegalStateException("simulated failure");
      }
      return result;
    }

    private int calls() {
      return calls;
    }
  }
}
