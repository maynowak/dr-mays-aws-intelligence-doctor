package io.github.maynowak.drmays.api.v1;

import java.util.Map;

/**
 * Wire representation of evidence for a finding.
 *
 * @param source technical source of the evidence
 * @param observed value actually observed
 * @param expected value expected by the rule
 * @param details additional structured evidence
 */
public record EvidenceResponse(
    String source, String observed, String expected, Map<String, String> details) {

  public EvidenceResponse {
    details = details == null ? Map.of() : Map.copyOf(details);
  }
}
