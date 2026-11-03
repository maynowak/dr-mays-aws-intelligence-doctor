package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.List;

/**
 * Internal JSON response shape returned by the model.
 *
 * @param summary explanation summary
 * @param explanations explanations for existing findings
 * @param recommendations suggested actions
 * @param uncertainties explicitly unknown impacts
 */
public record ModelResponse(
    String summary,
    List<ModelExplanation> explanations,
    List<ModelRecommendation> recommendations,
    List<String> uncertainties) {

  public record ModelExplanation(
      String ruleId, ModelResource resource, String explanation, List<ModelEvidence> evidence) {}

  public record ModelEvidence(String source) {}

  public record ModelRecommendation(String title, String action, String severity) {}

  public record ModelResource(
      String provider, String accountId, String region, String resourceType, String resourceId) {

    public ResourceIdentity toIdentity() {
      return new ResourceIdentity(provider, accountId, region, resourceType, resourceId);
    }
  }
}
