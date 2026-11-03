package io.github.maynowak.drmays.core.rule.builtin;

import static io.github.maynowak.drmays.core.TestFixtures.NETWORK_ID;
import static io.github.maynowak.drmays.core.TestFixtures.STORAGE_ID;
import static io.github.maynowak.drmays.core.TestFixtures.context;
import static io.github.maynowak.drmays.core.TestFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PublicIngressRuleTest {

  private final PublicIngressRule rule = new PublicIngressRule();

  @Test
  void detectsPublicIngress() {
    var network = resource(NETWORK_ID, Map.of(), Map.of("ingress.public", "true"));

    var outcome = rule.evaluate(network, context(network));

    assertThat(outcome).isInstanceOf(DiagnosticOutcome.FindingDetected.class);
    var finding = ((DiagnosticOutcome.FindingDetected) outcome).finding();
    assertThat(finding.ruleId()).isEqualTo(PublicIngressRule.RULE_ID);
    assertThat(finding.severity()).isEqualTo(Severity.HIGH);
    assertThat(finding.evidence().observed()).isEqualTo("ingress.public=true");
    assertThat(finding.evidence().expected()).isEqualTo("ingress.public=false");
  }

  @Test
  void returnsNoFindingForPrivateIngress() {
    var network = resource(NETWORK_ID, Map.of(), Map.of("ingress.public", "false"));

    var outcome = rule.evaluate(network, context(network));

    assertThat(outcome).isInstanceOf(DiagnosticOutcome.NoFinding.class);
  }

  @Test
  void returnsNotApplicableForMissingAttribute() {
    var network = resource(NETWORK_ID, Map.of(), Map.of());

    var outcome = rule.evaluate(network, context(network));

    assertThat(outcome).isInstanceOf(DiagnosticOutcome.NotApplicable.class);
    assertThat(((DiagnosticOutcome.NotApplicable) outcome).reason()).contains("not declared");
  }

  @Test
  void returnsNotApplicableForInvalidAttributeValue() {
    var network = resource(NETWORK_ID, Map.of(), Map.of("ingress.public", "sometimes"));

    var outcome = rule.evaluate(network, context(network));

    assertThat(outcome).isInstanceOf(DiagnosticOutcome.NotApplicable.class);
    assertThat(((DiagnosticOutcome.NotApplicable) outcome).reason())
        .contains("must be 'true' or 'false'");
  }

  @Test
  void returnsNotApplicableForNonNetworkResource() {
    var storage = resource(STORAGE_ID, Map.of(), Map.of("ingress.public", "true"));

    var outcome = rule.evaluate(storage, context(storage));

    assertThat(outcome).isInstanceOf(DiagnosticOutcome.NotApplicable.class);
    assertThat(((DiagnosticOutcome.NotApplicable) outcome).reason()).contains("network");
  }

  @Test
  void rejectsNullArguments() {
    var network = resource(NETWORK_ID, Map.of(), Map.of());

    assertThatNullPointerException().isThrownBy(() -> rule.evaluate(null, context(network)));
    assertThatNullPointerException().isThrownBy(() -> rule.evaluate(network, null));
  }
}
