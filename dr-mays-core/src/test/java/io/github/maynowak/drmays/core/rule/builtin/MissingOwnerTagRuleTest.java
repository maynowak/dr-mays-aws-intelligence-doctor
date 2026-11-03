package io.github.maynowak.drmays.core.rule.builtin;

import static io.github.maynowak.drmays.core.TestFixtures.NETWORK_ID;
import static io.github.maynowak.drmays.core.TestFixtures.context;
import static io.github.maynowak.drmays.core.TestFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MissingOwnerTagRuleTest {

  private final MissingOwnerTagRule rule = new MissingOwnerTagRule();

  @Test
  void detectsMissingOwnerTag() {
    var resource = resource(NETWORK_ID, Map.of(), Map.of());

    var outcome = rule.evaluate(resource, context(resource));

    assertThat(outcome).isInstanceOf(DiagnosticOutcome.FindingDetected.class);
    var finding = ((DiagnosticOutcome.FindingDetected) outcome).finding();
    assertThat(finding.ruleId()).isEqualTo(MissingOwnerTagRule.RULE_ID);
    assertThat(finding.severity()).isEqualTo(Severity.MEDIUM);
    assertThat(finding.evidence().observed()).isEqualTo("absent");
    assertThat(finding.evidence().details()).containsEntry("tag", "owner");
  }

  @Test
  void detectsBlankOwnerTagAsFinding() {
    var resource = resource(NETWORK_ID, Map.of("owner", "  "), Map.of());

    var outcome = rule.evaluate(resource, context(resource));

    var finding = ((DiagnosticOutcome.FindingDetected) outcome).finding();
    assertThat(finding.evidence().observed()).isEqualTo("blank");
  }

  @Test
  void returnsNoFindingForPresentOwnerTag() {
    var resource = resource(NETWORK_ID, Map.of("owner", "platform"), Map.of());

    var outcome = rule.evaluate(resource, context(resource));

    assertThat(outcome).isInstanceOf(DiagnosticOutcome.NoFinding.class);
    assertThat(outcome.ruleId()).isEqualTo(MissingOwnerTagRule.RULE_ID);
  }

  @Test
  void rejectsNullArguments() {
    var resource = resource(NETWORK_ID, Map.of(), Map.of());

    assertThatNullPointerException().isThrownBy(() -> rule.evaluate(null, context(resource)));
    assertThatNullPointerException().isThrownBy(() -> rule.evaluate(resource, null));
  }
}
