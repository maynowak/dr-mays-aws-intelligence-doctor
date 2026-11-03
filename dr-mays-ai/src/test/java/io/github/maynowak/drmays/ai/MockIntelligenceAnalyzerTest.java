package io.github.maynowak.drmays.ai;

import static io.github.maynowak.drmays.ai.AiTestFixtures.RESOURCE;
import static io.github.maynowak.drmays.ai.AiTestFixtures.RULE;
import static io.github.maynowak.drmays.ai.AiTestFixtures.finding;
import static io.github.maynowak.drmays.ai.AiTestFixtures.relationship;
import static io.github.maynowak.drmays.ai.AiTestFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.finding.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockIntelligenceAnalyzerTest {

  private final MockIntelligenceAnalyzer analyzer = new MockIntelligenceAnalyzer();

  @Test
  void explainsEveryFindingWithEvidence() {
    var result = analyzer.analyze(request());

    assertThat(result.status()).isEqualTo(ModelExecutionStatus.SUCCESS);
    assertThat(result.explanations()).hasSize(1);
    assertThat(result.evidenceReferences()).hasSize(1);
    assertThat(result.explanations().getFirst().evidence().getFirst().source())
        .isEqualTo("resource-attribute");
    assertThat(result.metadata().provider()).isEqualTo("mock");
  }

  @Test
  void preservesOnlyRequestRelationships() {
    var result = analyzer.analyze(request());

    assertThat(result.verifiedRelationships()).containsExactly(relationship());
  }

  @Test
  void handlesEmptyFindingsWithoutInventingOne() {
    var result =
        analyzer.analyze(new IntelligenceRequest(List.of(), List.of(), GraphCompleteness.COMPLETE));

    assertThat(result.explanations()).isEmpty();
    assertThat(result.recommendations()).isEmpty();
    assertThat(result.uncertainties())
        .anyMatch(value -> value.contains("No deterministic findings"));
  }

  @Test
  void exposesPartialDiscoveryUncertainty() {
    var result =
        analyzer.analyze(
            new IntelligenceRequest(List.of(finding()), List.of(), GraphCompleteness.PARTIAL));

    assertThat(result.uncertainties()).anyMatch(value -> value.contains("partial"));
  }

  @Test
  void isDeterministic() {
    assertThat(analyzer.analyze(request())).isEqualTo(analyzer.analyze(request()));
  }

  @Test
  void ordersFindingsDeterministically() {
    var first = finding(RULE, RESOURCE, Severity.HIGH);
    var second =
        finding(
            new io.github.maynowak.drmays.core.rule.RuleId("TAG-OWNER-001"),
            new io.github.maynowak.drmays.core.resource.ResourceIdentity(
                "aws", "000000000000", "eu-west-1", "network", "net-2"),
            Severity.MEDIUM);
    var result =
        analyzer.analyze(
            new IntelligenceRequest(List.of(second, first), List.of(), GraphCompleteness.COMPLETE));

    assertThat(result.explanations())
        .extracting(explanation -> explanation.ruleId().value())
        .containsExactly("NET-PUBLIC-001", "TAG-OWNER-001");
  }
}
