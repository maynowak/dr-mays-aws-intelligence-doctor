package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ProviderRouterTest {

  private static final ModelProviderDescriptor FAKE_DESCRIPTOR =
      new ModelProviderDescriptor(
          "fake",
          "Fake provider",
          ProviderKind.EXTERNAL_MODEL,
          new ProviderCapabilities(true, false, false, 1_000, 100),
          false,
          true,
          false);

  @Test
  void onlyOneAuthorizedRouteExists() {
    var routes =
        Arrays.stream(ProviderRouter.class.getMethods())
            .filter(method -> method.getName().equals("route"))
            .toList();

    assertThat(routes).hasSize(1);
    assertThat(routes.getFirst().getParameterTypes())
        .containsExactly(
            ModelInvocationRequest.class,
            io.github.maynowak.drmays.ai.privacy.ProviderInvocationAuthorization.class);
  }

  @Test
  void routesAuthorizedMockProvider() {
    var router =
        new ProviderRouter(new ProviderRegistry(List.of(new MockModelProvider())), Set.of("mock"));
    var authorized = ProviderTestFixtures.authorizedRequest("mock", "deterministic-v1");

    var result = router.route(authorized.request(), authorized.authorization());

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(result.content()).contains("No deterministic findings").contains("summary");
  }

  @Test
  void rejectsMissingAuthorizationWithoutCallingProvider() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");

    var result = router.route(authorized.request(), null);

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsExpiredAuthorization() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var policy = ProviderModelPolicy.of(Map.of("fake", Set.of("m")));
    var router =
        new ProviderRouter(
            new ProviderRegistry(List.of(fake)),
            Set.of("fake"),
            policy,
            CredentialResolutionPolicy.disabled(),
            Clock.offset(Clock.systemUTC(), Duration.ofMinutes(1)));
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsReplayedAuthorization() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isEqualTo(1);
  }

  @Test
  void rejectsModelNotAllowlisted() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router =
        new ProviderRouter(
            new ProviderRegistry(List.of(fake)),
            Set.of("fake"),
            ProviderModelPolicy.mockOnly(),
            CredentialResolutionPolicy.disabled(),
            Clock.systemUTC());
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.MODEL_NOT_ALLOWED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsToolsCapability() {
    var toolsDescriptor =
        new ModelProviderDescriptor(
            "fake",
            "Fake provider",
            ProviderKind.EXTERNAL_MODEL,
            new ProviderCapabilities(true, false, true, 1_000, 100),
            false,
            true,
            false);
    var fake =
        new FakeProvider(toolsDescriptor, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.PRIVACY_REJECTED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsCredentialProviderWhileResolutionIsDisabled() {
    var credentialDescriptor =
        new ModelProviderDescriptor(
            "fake",
            "Fake provider",
            ProviderKind.EXTERNAL_MODEL,
            new ProviderCapabilities(true, false, false, 1_000, 100),
            true,
            true,
            false);
    var fake =
        new FakeProvider(credentialDescriptor, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsPayloadHashMismatch() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");
    var request = authorized.request();
    var tampered =
        new ModelInvocationRequest(
            request.providerId(),
            request.modelId(),
            request.expertiseId(),
            request.payload(),
            request.payloadJson(),
            "wrong-hash",
            request.credentialReference(),
            request.timeout(),
            request.maxOutputChars());

    assertThat(router.route(tampered, authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsPayloadJsonSubstitutionEvenWithMatchingHashField() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");
    var request = authorized.request();
    var tampered =
        new ModelInvocationRequest(
            request.providerId(),
            request.modelId(),
            request.expertiseId(),
            request.payload(),
            "{\"tampered\":true}",
            request.payloadHash(),
            request.credentialReference(),
            request.timeout(),
            request.maxOutputChars());

    assertThat(router.route(tampered, authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsUnknownProvider() {
    var router =
        new ProviderRouter(
            new ProviderRegistry(),
            Set.of("missing"),
            ProviderModelPolicy.of(Map.of("missing", Set.of("m"))),
            CredentialResolutionPolicy.disabled(),
            Clock.systemUTC());
    var authorized = ProviderTestFixtures.authorizedRequest("missing", "m");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.NOT_ALLOWED);
  }

  @Test
  void concurrentAuthorizationAllowsExactlyOneProviderCall() throws Exception {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var first =
          pool.submit(() -> {
            start.await();
            return router.route(authorized.request(), authorized.authorization()).status();
          });
      var second =
          pool.submit(() -> {
            start.await();
            return router.route(authorized.request(), authorized.authorization()).status();
          });
      start.countDown();

      assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(
              ProviderExecutionStatus.SUCCESS, ProviderExecutionStatus.APPROVAL_REQUIRED);
      assertThat(fake.calls()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void rejectsWrongModelBinding() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router =
        new ProviderRouter(
            new ProviderRegistry(List.of(fake)),
            Set.of("fake"),
            ProviderModelPolicy.of(Map.of("fake", Set.of("m", "other-model"))),
            CredentialResolutionPolicy.disabled(),
            Clock.systemUTC());
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");
    var request = authorized.request();
    var wrongModel =
        new ModelInvocationRequest(
            request.providerId(),
            "other-model",
            request.expertiseId(),
            request.payload(),
            request.payloadJson(),
            request.payloadHash(),
            request.credentialReference(),
            request.timeout(),
            request.maxOutputChars());

    assertThat(router.route(wrongModel, authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsWrongExpertiseBinding() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");
    var request = authorized.request();
    var wrongExpertise =
        new ModelInvocationRequest(
            request.providerId(),
            request.modelId(),
            "other-expertise",
            request.payload(),
            request.payloadJson(),
            request.payloadHash(),
            request.credentialReference(),
            request.timeout(),
            request.maxOutputChars());

    assertThat(router.route(wrongExpertise, authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsWrongProviderBinding() {
    var fake =
        new FakeProvider(FAKE_DESCRIPTOR, ModelInvocationResult.success("fake", "m", "ok", 1));
    var mock = new MockModelProvider();
    var policy =
        ProviderModelPolicy.of(Map.of("fake", Set.of("m"), "mock", Set.of("deterministic-v1")));
    var router =
        new ProviderRouter(
            new ProviderRegistry(List.of(fake, mock)),
            Set.of("fake", "mock"),
            policy,
            CredentialResolutionPolicy.disabled(),
            Clock.systemUTC());
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");
    var request = authorized.request();
    var wrongProvider =
        new ModelInvocationRequest(
            "mock",
            "deterministic-v1",
            request.expertiseId(),
            request.payload(),
            request.payloadJson(),
            request.payloadHash(),
            ProviderCredentialReference.none("mock"),
            request.timeout(),
            request.maxOutputChars());

    assertThat(router.route(wrongProvider, authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.APPROVAL_REQUIRED);
    assertThat(fake.calls()).isZero();
  }

  @Test
  void rejectsDisabledProvider() {
    var router =
        new ProviderRouter(
            new ProviderRegistry(List.of(new MockModelProvider())),
            Set.of(),
            ProviderModelPolicy.mockOnly(),
            CredentialResolutionPolicy.disabled(),
            Clock.systemUTC());
    var authorized = ProviderTestFixtures.authorizedRequest("mock", "deterministic-v1");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.DISABLED);
  }

  @Test
  void mapsProviderExceptionWithoutFallback() {
    var fake = new FakeProvider(FAKE_DESCRIPTOR, null);
    fake.throwOnInvoke = true;
    var router = router(fake);
    var authorized = ProviderTestFixtures.authorizedRequest("fake", "m");

    assertThat(router.route(authorized.request(), authorized.authorization()).status())
        .isEqualTo(ProviderExecutionStatus.UNAVAILABLE);
    assertThat(fake.calls()).isEqualTo(1);
  }

  private static ProviderRouter router(FakeProvider fake) {
    return new ProviderRouter(
        new ProviderRegistry(List.of(fake)),
        Set.of("fake"),
        ProviderModelPolicy.of(Map.of("fake", Set.of("m"))),
        CredentialResolutionPolicy.disabled(),
        Clock.systemUTC());
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
