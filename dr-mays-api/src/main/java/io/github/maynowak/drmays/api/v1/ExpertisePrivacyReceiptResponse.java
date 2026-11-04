package io.github.maynowak.drmays.api.v1;

/**
 * Safe privacy/approval receipt in an expertise response.
 *
 * @param policyVersion privacy policy version
 * @param payloadHash exact payload hash
 * @param approvalStatus approval status
 * @param providerId provider identifier
 * @param modelId model identifier
 * @param expertiseId expertise profile identifier
 */
public record ExpertisePrivacyReceiptResponse(
    String policyVersion,
    String payloadHash,
    String approvalStatus,
    String providerId,
    String modelId,
    String expertiseId) {}
