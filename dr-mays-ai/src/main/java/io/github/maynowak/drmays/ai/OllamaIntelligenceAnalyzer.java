package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.dna.ResourceEdge;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.List;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Opt-in Ollama/Spring AI analyzer with strict response verification. */
public final class OllamaIntelligenceAnalyzer implements IntelligenceAnalyzer {

  private final ChatCompletion completion;
  private final PromptBuilder promptBuilder;
  private final ObjectMapper objectMapper;
  private final ModelMetadata metadata;
  private final IntelligenceResultValidator validator;

  public OllamaIntelligenceAnalyzer(
      ChatCompletion completion,
      PromptBuilder promptBuilder,
      ObjectMapper objectMapper,
      ModelMetadata metadata) {
    this.completion = Objects.requireNonNull(completion, "completion");
    this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    this.metadata = Objects.requireNonNull(metadata, "metadata");
    this.validator = new IntelligenceResultValidator();
  }

  @Override
  public IntelligenceResult analyze(IntelligenceRequest request) {
    Objects.requireNonNull(request, "request");
    try {
      var prompt = promptBuilder.build(request);
      var content = completion.complete(prompt);
      if (content == null || content.isBlank()) {
        return IntelligenceResult.unavailable(metadata);
      }
      var response = objectMapper.readValue(stripFences(content), ModelResponse.class);
      var explanations =
          response.explanations() == null
              ? List.<FindingExplanation>of()
              : response.explanations().stream().map(this::toExplanation).toList();
      var evidence =
          explanations.stream().flatMap(explanation -> explanation.evidence().stream()).toList();
      var recommendations =
          response.recommendations() == null
              ? List.<Recommendation>of()
              : response.recommendations().stream().map(this::toRecommendation).toList();
      var result =
          new IntelligenceResult(
              ModelExecutionStatus.SUCCESS,
              metadata,
              response.summary() == null ? "" : response.summary(),
              explanations,
              recommendations,
              evidence,
              request.relationships(),
              response.uncertainties() == null ? List.of() : response.uncertainties());
      return validator.validate(request, result);
    } catch (JacksonException | IllegalArgumentException | NullPointerException exception) {
      return IntelligenceResult.invalidResponse(metadata);
    } catch (PromptBuildException exception) {
      return IntelligenceResult.failed(metadata);
    } catch (RuntimeException exception) {
      return IntelligenceResult.unavailable(metadata);
    }
  }

  private FindingExplanation toExplanation(ModelResponse.ModelExplanation explanation) {
    var ruleId = new RuleId(explanation.ruleId());
    var resource = explanation.resource().toIdentity();
    var evidence =
        explanation.evidence() == null
            ? List.<EvidenceReference>of()
            : explanation.evidence().stream()
                .map(value -> new EvidenceReference(ruleId, resource, value.source()))
                .toList();
    return new FindingExplanation(ruleId, resource, explanation.explanation(), evidence);
  }

  private Recommendation toRecommendation(ModelResponse.ModelRecommendation recommendation) {
    return new Recommendation(
        recommendation.title(),
        recommendation.action(),
        Severity.valueOf(recommendation.severity()));
  }

  private static String stripFences(String content) {
    var trimmed = content.strip();
    if (trimmed.startsWith("```")) {
      var firstNewline = trimmed.indexOf('\n');
      var lastFence = trimmed.lastIndexOf("```");
      if (firstNewline >= 0 && lastFence > firstNewline) {
        return trimmed.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return trimmed;
  }

  static List<ResourceEdge> verifiedRelationships(IntelligenceRequest request) {
    return request.relationships();
  }
}
