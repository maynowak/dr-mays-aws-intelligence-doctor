package io.github.maynowak.drmays.core.finding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosticFindingTest {

  private static final ResourceIdentity ID =
      new ResourceIdentity("aws", "123456789012", "eu-central-1", "network", "net-1");
  private static final RuleId RULE_ID = new RuleId("NET-PUBLIC-001");
  private static final Evidence EVIDENCE =
      new Evidence(
          ID, "resource-attribute", "true", "false", Map.of("attribute", "ingress.public"));

  @Test
  void constructsValidFinding() {
    var finding = finding(EVIDENCE, Severity.HIGH);

    assertThat(finding.ruleId()).isEqualTo(RULE_ID);
    assertThat(finding.resource()).isEqualTo(ID);
    assertThat(finding.evidence()).isEqualTo(EVIDENCE);
    assertThat(finding.severity()).isEqualTo(Severity.HIGH);
  }

  @Test
  void rejectsEvidenceForAnotherResource() {
    var otherId = new ResourceIdentity("aws", "123456789012", "eu-central-1", "network", "net-2");
    var otherEvidence = new Evidence(otherId, "source", "observed", "expected", Map.of());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> finding(otherEvidence, Severity.HIGH))
        .withMessage("evidence resource must match finding resource");
  }

  @Test
  void rejectsBlankFields() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiagnosticFinding(
                RULE_ID, "1.0", Severity.HIGH, ID, " ", "description", EVIDENCE, "recommendation"))
        .withMessage("title must not be blank");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiagnosticFinding(
                RULE_ID, "1.0", Severity.HIGH, ID, "title", "", EVIDENCE, "recommendation"))
        .withMessage("description must not be blank");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiagnosticFinding(
                RULE_ID, "1.0", Severity.HIGH, ID, "title", "description", EVIDENCE, " "))
        .withMessage("recommendation must not be blank");
  }

  @Test
  void rejectsNullSeverity() {
    assertThatNullPointerException()
        .isThrownBy(() ->
            new DiagnosticFinding(
                RULE_ID, "1.0", null, ID, "title", "description", EVIDENCE, "recommendation"))
        .withMessage("severity");
  }

  private static DiagnosticFinding finding(Evidence evidence, Severity severity) {
    return new DiagnosticFinding(
        RULE_ID,
        "1.0",
        severity,
        ID,
        "Public ingress",
        "The network resource allows public ingress",
        evidence,
        "Restrict the ingress range");
  }
}
