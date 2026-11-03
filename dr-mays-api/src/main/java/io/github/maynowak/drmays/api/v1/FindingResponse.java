package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Wire representation of one technical finding.
 *
 * @param ruleId stable rule identifier
 * @param ruleVersion rule version
 * @param severity finding severity
 * @param resource affected resource
 * @param title short title
 * @param description factual description
 * @param evidence traceable evidence
 * @param recommendation recommended remediation
 * @param relationships verified outgoing graph relationships for the resource
 */
public record FindingResponse(
    String ruleId,
    String ruleVersion,
    String severity,
    ResourceResponse resource,
    String title,
    String description,
    EvidenceResponse evidence,
    String recommendation,
    List<GraphEdgeResponse> relationships) {

  public FindingResponse {
    relationships = relationships == null ? List.of() : List.copyOf(relationships);
  }
}
