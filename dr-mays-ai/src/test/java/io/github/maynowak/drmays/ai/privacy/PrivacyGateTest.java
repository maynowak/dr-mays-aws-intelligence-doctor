package io.github.maynowak.drmays.ai.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.ai.provider.MockModelProvider;
import io.github.maynowak.drmays.ai.provider.ProviderApprovalPolicy;
import io.github.maynowak.drmays.ai.provider.ProviderRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PrivacyGateTest {

  private static final String PREVIEW = "model";

  private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
  private final PrivacyGate gate =
      new PrivacyGate(
          Duration.ofMinutes(5), Duration.ofMinutes(5), clock, id -> !"mock".equals(id));
  private final PrivacyProcessingResult processed = PrivacyTestFixtures.processed();
  private final PrivacyProcessingResult invalidProcessed =
      PrivacyTestFixtures.processor(PrivacyPolicy.defaultPolicy())
          .process(PrivacyTestFixtures.requestWithObserved("token=AKIAABCDEFGHIJKLMNOP"));

  @Test
  void registersExactPayloadWithoutCallingProvider() {
    var preview = register(false, PREVIEW, processed);

    assertThat(preview.payloadJson()).isEqualTo(processed.payloadJson());
    assertThat(preview.payloadHash()).isEqualTo(processed.payloadHash());
    assertThat(preview.approvalRequired()).isFalse();
    assertThat(gate.pendingCount()).isEqualTo(1);
  }

  @Test
  void localProviderNeedsNoToken() {
    var preview = register(false, PREVIEW, processed);

    var decision = gate.consume(preview.previewId(), null);

    assertThat(decision.status()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(decision.invocation()).isPresent();
    assertThat(decision.invocation().orElseThrow().source())
        .isEqualTo(PrivacyTestFixtures.request());
    assertThat(decision.invocation().orElseThrow().authorization().payloadHash())
        .isEqualTo(processed.payloadHash());
  }

  @Test
  void remoteProviderFailsClosedWithoutToken() {
    var preview = register(true, PREVIEW, processed);

    var decision = gate.consume(preview.previewId(), null);

    assertThat(decision.status()).isEqualTo(ApprovalStatus.APPROVAL_REQUIRED);
    assertThat(decision.invocation()).isEmpty();
    assertThat(gate.pendingCount()).isEqualTo(1);
  }

  @Test
  void approvedTokenWorksOnce() {
    var preview = register(true, PREVIEW, processed);
    var token = gate.approve(preview.previewId()).orElseThrow();

    var first = gate.consume(preview.previewId(), token);
    var second = gate.consume(preview.previewId(), token);

    assertThat(first.status()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(second.status()).isEqualTo(ApprovalStatus.CONSUMED);
  }

  @Test
  void approvingUnknownPreviewReturnsEmpty() {
    assertThat(gate.approve("unknown")).isEmpty();
  }

  @Test
  void unknownTokenIsRejected() {
    var preview = register(true, PREVIEW, processed);
    var wrong = new ApprovalToken("A".repeat(43));

    assertThat(gate.consume(preview.previewId(), wrong).status())
        .isEqualTo(ApprovalStatus.MISMATCH);
  }

  @Test
  void tokenFromAnotherPreviewIsRejected() {
    var first = register(true, PREVIEW, processed);
    var second = register(true, PREVIEW, processed);
    var token = gate.approve(first.previewId()).orElseThrow();

    assertThat(gate.consume(second.previewId(), token).status()).isEqualTo(ApprovalStatus.MISMATCH);
  }

  @Test
  void expiredPreviewCannotBeApprovedOrConsumed() {
    var preview = register(false, PREVIEW, processed);
    clock.advance(Duration.ofMinutes(6));

    assertThat(gate.consume(preview.previewId(), null).status()).isEqualTo(ApprovalStatus.EXPIRED);
    assertThat(gate.approve(preview.previewId())).isEmpty();
  }

  @Test
  void expiredApprovalIsRejected() {
    var preview = register(true, PREVIEW, processed);
    var token = gate.approve(preview.previewId()).orElseThrow();
    clock.advance(Duration.ofMinutes(6));

    assertThat(gate.consume(preview.previewId(), token).status()).isEqualTo(ApprovalStatus.EXPIRED);
  }

  @Test
  void invalidPrivacyPayloadIsRejectedBeforeExecution() {
    var preview = register(true, PREVIEW, invalidProcessed);
    var token = gate.approve(preview.previewId()).orElseThrow();

    assertThat(gate.consume(preview.previewId(), token).status())
        .isEqualTo(ApprovalStatus.PRIVACY_REJECTED);
  }

  @Test
  void pruneRemovesExpiredState() {
    register(false, PREVIEW, processed);
    clock.advance(Duration.ofMinutes(10));

    gate.prune();

    assertThat(gate.pendingCount()).isZero();
  }

  @Test
  void approvalsAreBounded() {
    var preview = register(true, PREVIEW, processed);
    for (int i = 0; i < 100; i++) {
      assertThat(gate.approve(preview.previewId())).isPresent();
    }

    assertThat(gate.approve(preview.previewId())).isEmpty();
  }

  @Test
  void concurrentConsumptionAllowsExactlyOneExecution() throws Exception {
    var preview = register(true, PREVIEW, processed);
    var token = gate.approve(preview.previewId()).orElseThrow();
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var first =
          pool.submit(() -> {
            start.await();
            return gate.consume(preview.previewId(), token).status();
          });
      var second =
          pool.submit(() -> {
            start.await();
            return gate.consume(preview.previewId(), token).status();
          });
      start.countDown();

      assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(ApprovalStatus.APPROVED, ApprovalStatus.CONSUMED);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void defaultGateIsStrictForEvenTheMockProvider() {
    var strict = new PrivacyGate(Duration.ofMinutes(5), Duration.ofMinutes(5), clock, id -> true);
    var preview =
        strict.register(
            PrivacyTestFixtures.request(),
            processed,
            "mock",
            PREVIEW,
            "standard",
            io.github.maynowak.drmays.ai.provider.ProviderCredentialBinding.none("mock"));

    assertThat(strict.consume(preview.previewId(), null).status())
        .isEqualTo(ApprovalStatus.APPROVAL_REQUIRED);
  }

  @Test
  void registryPolicyExemptsOnlyTheRealMock() {
    var registry = new ProviderRegistry(List.of(new MockModelProvider()));
    var policyGate =
        new PrivacyGate(
            Duration.ofMinutes(5),
            Duration.ofMinutes(5),
            clock,
            ProviderApprovalPolicy.forRegistry(registry));
    var mockPreview =
        policyGate.register(
            PrivacyTestFixtures.request(),
            processed,
            "mock",
            PREVIEW,
            "standard",
            io.github.maynowak.drmays.ai.provider.ProviderCredentialBinding.none("mock"));
    var fakePreview =
        policyGate.register(
            PrivacyTestFixtures.request(),
            processed,
            "fake",
            PREVIEW,
            "standard",
            io.github.maynowak.drmays.ai.provider.ProviderCredentialBinding.none("fake"));

    assertThat(policyGate.consume(mockPreview.previewId(), null).status())
        .isEqualTo(ApprovalStatus.APPROVED);
    assertThat(policyGate.consume(fakePreview.previewId(), null).status())
        .isEqualTo(ApprovalStatus.APPROVAL_REQUIRED);
  }

  @Test
  void registerHasNoCallerSuppliedApprovalParameter() {
    assertThat(
            java.util.Arrays.stream(PrivacyGate.class.getMethods())
                .filter(method -> method.getName().equals("register"))
                .allMatch(method ->
                    method.getParameterCount() == 6
                        && java.util.Arrays.stream(method.getParameterTypes())
                            .noneMatch(type -> type == boolean.class)))
        .isTrue();
  }

  @Test
  void authorizationCannotBeConstructedPubliclyAndRedactsToString() {
    var preview = register(false, PREVIEW, processed);
    var authorization =
        gate.consume(preview.previewId(), null).invocation().orElseThrow().authorization();

    assertThat(ProviderInvocationAuthorization.class.getConstructors()).isEmpty();
    assertThat(authorization.toString()).isEqualTo("ProviderInvocationAuthorization[redacted]");
  }

  @Test
  void authorizationBindsProviderModelExpertisePolicyAndHash() {
    var preview = register(false, PREVIEW, processed);
    var authorization =
        gate.consume(preview.previewId(), null).invocation().orElseThrow().authorization();

    assertThat(authorization.providerId()).isEqualTo("mock");
    assertThat(authorization.modelId()).isEqualTo(PREVIEW);
    assertThat(authorization.expertiseId()).isEqualTo("standard");
    assertThat(authorization.policyVersion()).isEqualTo(PrivacyPolicy.defaultPolicy().version());
    assertThat(authorization.payloadHash()).isEqualTo(processed.payloadHash());
    assertThat(authorization.expired(clock.instant())).isFalse();
    assertThat(authorization.expired(clock.instant().plus(Duration.ofSeconds(31)))).isTrue();
  }

  @Test
  void approvalTokenRedactsItsValue() {
    assertThat(new ApprovalToken("A".repeat(43)).toString()).isEqualTo("ApprovalToken[redacted]");
  }

  private PrivacyPreview register(
      boolean approvalRequired, String modelId, PrivacyProcessingResult result) {
    var provider = approvalRequired ? "remote" : "mock";
    return gate.register(
        PrivacyTestFixtures.request(),
        result,
        provider,
        modelId,
        "standard",
        io.github.maynowak.drmays.ai.provider.ProviderCredentialBinding.none(provider));
  }

  private static final class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    private void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
