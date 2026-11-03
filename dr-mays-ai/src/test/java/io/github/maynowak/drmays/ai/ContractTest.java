package io.github.maynowak.drmays.ai;

import static io.github.maynowak.drmays.ai.AiTestFixtures.RESOURCE;
import static io.github.maynowak.drmays.ai.AiTestFixtures.RULE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.maynowak.drmays.core.finding.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContractTest {

  @Test
  void modelMetadataRequiresText() {
    var metadata = new ModelMetadata("mock", "deterministic-v1", "1.0");
    assertThat(metadata.provider()).isEqualTo("mock");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ModelMetadata(" ", "m", "1"))
        .withMessage("provider must not be blank");
    assertThatNullPointerException()
        .isThrownBy(() -> new ModelMetadata(null, "m", "1"))
        .withMessage("provider");
  }

  @Test
  void evidenceReferenceRequiresSource() {
    var ref = new EvidenceReference(RULE, RESOURCE, "resource-attribute");
    assertThat(ref.source()).isEqualTo("resource-attribute");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new EvidenceReference(RULE, RESOURCE, " "));
  }

  @Test
  void findingExplanationRequiresEvidence() {
    var ref = new EvidenceReference(RULE, RESOURCE, "resource-attribute");
    var explanation = new FindingExplanation(RULE, RESOURCE, "explanation", List.of(ref));
    assertThat(explanation.evidence()).hasSize(1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new FindingExplanation(RULE, RESOURCE, "explanation", List.of()));
  }

  @Test
  void recommendationRequiresSeverity() {
    var recommendation = new Recommendation("Title", "Action", Severity.HIGH);
    assertThat(recommendation.severity()).isEqualTo(Severity.HIGH);
    assertThatNullPointerException().isThrownBy(() -> new Recommendation("Title", "Action", null));
  }

  @Test
  void resultFactoriesExposeNonSuccessStatuses() {
    var metadata = new ModelMetadata("mock", "m", "1");
    assertThat(IntelligenceResult.disabled(metadata).status())
        .isEqualTo(ModelExecutionStatus.DISABLED);
    assertThat(IntelligenceResult.unavailable(metadata).status())
        .isEqualTo(ModelExecutionStatus.UNAVAILABLE);
    assertThat(IntelligenceResult.failed(metadata).status()).isEqualTo(ModelExecutionStatus.FAILED);
    assertThat(IntelligenceResult.invalidResponse(metadata).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }
}
