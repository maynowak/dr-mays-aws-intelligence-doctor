package io.github.maynowak.drmays.ai;

import static io.github.maynowak.drmays.ai.AiTestFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OllamaIntelligenceAnalyzerTest {

  private static final String VALID =
      """
      {
        "summary": "One finding explained.",
        "explanations": [{
          "ruleId": "NET-PUBLIC-001",
          "resource": {"provider":"aws","accountId":"000000000000","region":"eu-west-1",
            "resourceType":"network","resourceId":"net-1"},
          "explanation": "Public ingress was observed.",
          "evidence": [{"source":"resource-attribute"}]
        }],
        "recommendations": [{"title":"Restrict","action":"Restrict ingress.","severity":"HIGH"}],
        "uncertainties": ["Reachability is not proven."]
      }
      """;

  @Test
  void parsesValidStructuredResponse() {
    var result = analyzer(prompt -> VALID).analyze(request());

    assertThat(result.status()).isEqualTo(ModelExecutionStatus.SUCCESS);
    assertThat(result.explanations()).hasSize(1);
    assertThat(result.recommendations()).hasSize(1);
    assertThat(result.metadata().provider()).isEqualTo("ollama");
  }

  @Test
  void stripsMarkdownFences() {
    var result = analyzer(prompt -> "```json\n" + VALID + "\n```").analyze(request());

    assertThat(result.status()).isEqualTo(ModelExecutionStatus.SUCCESS);
  }

  @Test
  void marksBlankModelResponseUnavailable() {
    assertThat(analyzer(prompt -> " ").analyze(request()).status())
        .isEqualTo(ModelExecutionStatus.UNAVAILABLE);
  }

  @Test
  void rejectsMalformedJson() {
    assertThat(analyzer(prompt -> "{not-json").analyze(request()).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsUnknownEvidence() {
    var invalid = VALID.replace("resource-attribute", "invented");

    assertThat(analyzer(prompt -> invalid).analyze(request()).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsInvalidSeverity() {
    var invalid = VALID.replace("\"HIGH\"", "\"SEVERE\"");

    assertThat(analyzer(prompt -> invalid).analyze(request()).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void marksConnectionFailureUnavailable() {
    var analyzer =
        new OllamaIntelligenceAnalyzer(
            prompt -> {
              throw new IllegalStateException("offline");
            },
            new PromptBuilder(),
            new ObjectMapper(),
            new ModelMetadata("ollama", "test", "1"));

    assertThat(analyzer.analyze(request()).status()).isEqualTo(ModelExecutionStatus.UNAVAILABLE);
  }

  private static OllamaIntelligenceAnalyzer analyzer(ChatCompletion completion) {
    return new OllamaIntelligenceAnalyzer(
        completion,
        new PromptBuilder(),
        new ObjectMapper(),
        new ModelMetadata("ollama", "test", "1"));
  }
}
