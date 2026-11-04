package io.github.maynowak.drmays.ai.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Deterministic local provider used by default and in CI. */
public final class MockModelProvider implements ModelProvider {

  private final ObjectMapper objectMapper;

  public MockModelProvider() {
    this(new ObjectMapper());
  }

  /**
   * Creates a mock provider with an explicit JSON mapper.
   *
   * @param objectMapper JSON mapper
   */
  public MockModelProvider(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public ModelProviderDescriptor descriptor() {
    return ModelProviderDescriptor.mock();
  }

  @Override
  public ModelInvocationResult invoke(ModelInvocationRequest request) {
    Objects.requireNonNull(request, "request");
    var payload = request.payload();
    var explanations = new ArrayList<Map<String, Object>>();
    var recommendations = new ArrayList<Map<String, Object>>();
    for (var finding : payload.findings()) {
      var evidence = List.of(Map.of("source", finding.evidenceSource()));
      explanations.add(
          Map.of(
              "ruleId",
              finding.ruleId(),
              "resourceAlias",
              finding.resourceAlias(),
              "explanation",
              "Rule "
                  + finding.ruleId()
                  + " observed "
                  + finding.observed()
                  + ". The technical finding remains authoritative.",
              "evidence",
              evidence));
      recommendations.add(
          Map.of(
              "title",
              "Review " + finding.ruleId(),
              "action",
              "Review the existing evidence and apply the deterministic remediation.",
              "severity",
              finding.severity()));
    }
    var body = new LinkedHashMap<String, Object>();
    body.put(
        "summary",
        payload.findings().isEmpty()
            ? "No deterministic findings were produced."
            : payload.findings().size()
                + " deterministic finding(s) explained by the mock provider.");
    body.put("explanations", explanations);
    body.put("recommendations", recommendations);
    body.put("assumptions", List.of());
    body.put("missingInformation", List.of());
    body.put("uncertainties", payload.uncertainties());
    try {
      var content = objectMapper.writeValueAsString(body);
      if (content.length() > request.maxOutputChars()) {
        return ModelInvocationResult.failure(
            ProviderExecutionStatus.INVALID_RESPONSE,
            request.providerId(),
            request.modelId(),
            "MOCK_OUTPUT_TOO_LARGE");
      }
      return ModelInvocationResult.success(
          request.providerId(), request.modelId(), content, request.payloadJson().length());
    } catch (JacksonException exception) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.INVALID_RESPONSE,
          request.providerId(),
          request.modelId(),
          "MOCK_SERIALIZATION_FAILED");
    }
  }
}
