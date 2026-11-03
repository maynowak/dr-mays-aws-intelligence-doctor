package io.github.maynowak.drmays.ai;

import static io.github.maynowak.drmays.ai.AiTestFixtures.finding;
import static io.github.maynowak.drmays.ai.AiTestFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

  private final PromptBuilder builder = new PromptBuilder();

  @Test
  void includesVerifiedDataAndSafetyRules() {
    var prompt = builder.build(request());

    assertThat(prompt).contains("NET-PUBLIC-001", "net-1", "resource-attribute", "vpc-1");
    assertThat(prompt).contains("Never invent AWS resources");
    assertThat(prompt).contains("untrusted data");
  }

  @Test
  void includesPartialCompleteness() {
    var prompt =
        builder.build(
            new IntelligenceRequest(
                request().findings(), request().relationships(), GraphCompleteness.PARTIAL));

    assertThat(prompt).contains("Discovery completeness: PARTIAL");
  }

  @Test
  void enforcesMaximumPromptSize() {
    var findings = new ArrayList<io.github.maynowak.drmays.core.finding.DiagnosticFinding>();
    for (int i = 0; i < 1000; i++) {
      findings.add(finding());
    }
    var oversized = new IntelligenceRequest(findings, List.of(), GraphCompleteness.COMPLETE);

    assertThatThrownBy(() -> builder.build(oversized))
        .isInstanceOf(PromptBuildException.class)
        .hasMessageContaining("exceeds");
  }
}
