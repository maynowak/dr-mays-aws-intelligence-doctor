package io.github.maynowak.drmays.api.v1;

import java.time.Instant;
import java.util.List;

/**
 * Safe preview of the exact provider payload.
 *
 * @param previewId opaque server-side handle
 * @param payloadHash exact payload hash
 * @param policyVersion privacy policy version
 * @param providerId selected provider
 * @param modelId selected model
 * @param expertiseId selected expertise profile
 * @param approvalRequired whether execution needs a token
 * @param expiresAt preview expiry
 * @param payloadJson exact sanitized payload
 * @param decisions field-level decisions
 * @param validation privacy validation
 */
public record ExpertisePreviewResponse(
    String previewId,
    String payloadHash,
    String policyVersion,
    String providerId,
    String modelId,
    String expertiseId,
    boolean approvalRequired,
    Instant expiresAt,
    String payloadJson,
    List<ExpertiseFieldDecisionResponse> decisions,
    ExpertisePrivacyValidationResponse validation) {

  public ExpertisePreviewResponse {
    decisions = List.copyOf(decisions);
    validation = java.util.Objects.requireNonNull(validation, "validation");
  }
}
