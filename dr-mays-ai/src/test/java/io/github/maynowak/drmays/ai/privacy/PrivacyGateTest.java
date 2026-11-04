package io.github.maynowak.drmays.ai.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PrivacyGateTest {

  private static final String PREVIEW = "model";

  private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
  private final PrivacyGate gate =
      new PrivacyGate(Duration.ofMinutes(5), Duration.ofMinutes(5), clock);
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
  void approvalTokenRedactsItsValue() {
    assertThat(new ApprovalToken("A".repeat(43)).toString()).isEqualTo("ApprovalToken[redacted]");
  }

  private PrivacyPreview register(
      boolean approvalRequired, String modelId, PrivacyProcessingResult result) {
    return gate.register(
        PrivacyTestFixtures.request(), result, "mock", modelId, "standard", approvalRequired);
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
