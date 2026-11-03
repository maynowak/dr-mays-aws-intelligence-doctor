package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Explanation of one existing finding.
 *
 * @param ruleId finding rule
 * @param resource finding resource
 * @param explanation human-readable explanation
 * @param evidence existing evidence references
 */
public record IntelligenceExplanationResponse(
    String ruleId,
    ResourceResponse resource,
    String explanation,
    List<EvidenceReferenceResponse> evidence) {

  public IntelligenceExplanationResponse {
    evidence = List.copyOf(evidence);
  }
}
