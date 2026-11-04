package io.github.maynowak.drmays.ai.expertise;

import java.util.List;

/**
 * Internal JSON shape of a structured expertise response.
 *
 * @param summary explanation summary
 * @param explanations finding explanations
 * @param recommendations recommendations
 * @param assumptions unverified model assumptions
 * @param missingInformation explicitly missing information
 * @param uncertainties uncertainty statements
 */
public record ExpertiseModelResponse(
    String summary,
    List<Explanation> explanations,
    List<Recommendation> recommendations,
    List<String> assumptions,
    List<String> missingInformation,
    List<String> uncertainties) {

  public ExpertiseModelResponse {
    summary = summary == null ? "" : summary;
    explanations = explanations == null ? List.of() : List.copyOf(explanations);
    recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
    missingInformation = missingInformation == null ? List.of() : List.copyOf(missingInformation);
    uncertainties = uncertainties == null ? List.of() : List.copyOf(uncertainties);
  }

  public record Explanation(
      String ruleId, String resourceAlias, String explanation, List<Evidence> evidence) {

    public Explanation {
      evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
  }

  public record Evidence(String source) {}

  public record Recommendation(String title, String action, String severity) {}
}
