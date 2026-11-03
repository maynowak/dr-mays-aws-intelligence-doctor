package io.github.maynowak.drmays.core.diagnosis;

import static io.github.maynowak.drmays.core.TestFixtures.NETWORK_ID;
import static io.github.maynowak.drmays.core.TestFixtures.STORAGE_ID;
import static io.github.maynowak.drmays.core.TestFixtures.finding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosisResultTest {

  private static final RuleId RULE_A = new RuleId("AAA-001");
  private static final RuleId RULE_B = new RuleId("BBB-001");

  @Test
  void sortsOutcomesByResourceThenRule() {
    var storageOutcome = new DiagnosticOutcome.NoFinding(RULE_A, STORAGE_ID, "none");
    var networkOutcome = new DiagnosticOutcome.NoFinding(RULE_B, NETWORK_ID, "none");

    var result = new DiagnosisResult(List.of(storageOutcome, networkOutcome));

    assertThat(result.outcomes())
        .extracting(DiagnosticOutcome::summary)
        .containsExactly(
            "NO_FINDING BBB-001 @ " + NETWORK_ID.canonicalId(),
            "NO_FINDING AAA-001 @ " + STORAGE_ID.canonicalId());
  }

  @Test
  void derivesFindingsInDeterministicOrder() {
    var networkFinding = new DiagnosticOutcome.FindingDetected(finding(NETWORK_ID, RULE_B));
    var storageNoFinding = new DiagnosticOutcome.NoFinding(RULE_A, STORAGE_ID, "none");

    var result = new DiagnosisResult(List.of(storageNoFinding, networkFinding));

    assertThat(result.findings()).extracting(finding -> finding.ruleId()).containsExactly(RULE_B);
    assertThat(result.findingCount()).isEqualTo(1);
    assertThat(result.hasFindings()).isTrue();
  }

  @Test
  void rejectsDuplicateRuleAndResourcePairs() {
    var first = new DiagnosticOutcome.NoFinding(RULE_A, NETWORK_ID, "first");
    var second = new DiagnosticOutcome.NotApplicable(RULE_A, NETWORK_ID, "second");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new DiagnosisResult(List.of(first, second)))
        .withMessageContaining("duplicate outcome");
  }

  @Test
  void exposesImmutableOutcomes() {
    var result =
        new DiagnosisResult(List.of(new DiagnosticOutcome.NoFinding(RULE_A, NETWORK_ID, "none")));

    assertThatThrownBy(() -> result.outcomes().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void supportsEmptyResults() {
    var result = new DiagnosisResult(List.of());

    assertThat(result.outcomes()).isEmpty();
    assertThat(result.findings()).isEmpty();
    assertThat(result.findingCount()).isZero();
    assertThat(result.hasFindings()).isFalse();
  }

  @Test
  void rejectsNullInputs() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DiagnosisResult(null))
        .withMessage("outcomes");
    assertThatNullPointerException()
        .isThrownBy(() ->
            new DiagnosisResult(
                java.util.Arrays.asList(
                    new DiagnosticOutcome.NoFinding(RULE_A, NETWORK_ID, "none"), null)))
        .withMessage("outcome");
  }
}
