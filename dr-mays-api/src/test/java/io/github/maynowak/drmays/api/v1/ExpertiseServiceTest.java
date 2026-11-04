package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.ai.expertise.PrivacyFirstExpertiseEngine;
import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpertiseServiceTest {

  private final DiagnosisService diagnosisService =
      new DiagnosisService(
          new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule())));
  private final ExpertiseService service =
      new ExpertiseService(
          new IntelligenceInputFactory(diagnosisService), new PrivacyFirstExpertiseEngine());

  @Test
  void previewContainsSanitizedPayloadAndNoRawIdentifiers() {
    var response = service.preview(previewRequest());

    assertThat(response.validation().valid()).isTrue();
    assertThat(response.approvalRequired()).isFalse();
    assertThat(response.payloadJson())
        .doesNotContain("123456789012")
        .doesNotContain("vpc-0def5678")
        .contains("res_");
    assertThat(response.decisions()).hasSize(14);
  }

  @Test
  void explainReturnsValidatedExpertise() {
    var preview = service.preview(previewRequest());

    var response = service.explain(new ExpertiseExplainRequest(preview.previewId(), null));

    assertThat(response.status()).isEqualTo("SUCCESS");
    assertThat(response.explanations()).hasSize(2);
    assertThat(response.evidenceReferences()).isNotEmpty();
    assertThat(response.verifiedRelationships()).isNotEmpty();
    assertThat(response.facts()).hasSize(2);
    assertThat(response.privacyReceipt().approvalStatus()).isEqualTo("NOT_REQUIRED");
  }

  @Test
  void approvalTokenCanBeUsedForMockExecution() {
    var preview = service.preview(previewRequest());
    var approval = service.approve(new ExpertiseApprovalRequest(preview.previewId()));

    var response =
        service.explain(new ExpertiseExplainRequest(preview.previewId(), approval.approvalToken()));

    assertThat(response.status()).isEqualTo("SUCCESS");
  }

  @Test
  void unknownPreviewIsRejectedAsApprovalRequired() {
    var response = service.explain(new ExpertiseExplainRequest("unknown", null));

    assertThat(response.status()).isEqualTo("APPROVAL_REQUIRED");
  }

  @Test
  void unknownPreviewCannotBeApproved() {
    assertThatThrownBy(() -> service.approve(new ExpertiseApprovalRequest("unknown")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static ExpertisePreviewRequest previewRequest() {
    return new ExpertisePreviewRequest(
        List.of(
            new ResourceRequest(
                new ResourceIdentityRequest("aws", "123456789012", "eu-west-1", "network", "net-1"),
                Map.of(),
                Map.of("ingress.public", "true", "vpc.id", "vpc-0def5678"))),
        false,
        "mock",
        "deterministic-v1",
        "standard");
  }
}
