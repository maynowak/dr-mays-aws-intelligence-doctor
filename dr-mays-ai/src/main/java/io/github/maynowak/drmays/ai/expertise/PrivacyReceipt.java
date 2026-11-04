package io.github.maynowak.drmays.ai.expertise;

import io.github.maynowak.drmays.ai.privacy.ApprovalStatus;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Safe privacy metadata attached to an expertise report.
 *
 * @param policyVersion privacy policy version
 * @param payloadHash exact payload hash
 * @param approvalStatus approval outcome
 * @param providerId provider identifier
 * @param modelId model identifier
 * @param expertiseId expertise profile identifier
 */
public record PrivacyReceipt(
    String policyVersion,
    String payloadHash,
    ApprovalStatus approvalStatus,
    String providerId,
    String modelId,
    String expertiseId) {

  public PrivacyReceipt {
    policyVersion = DomainChecks.requireText(policyVersion, "policyVersion");
    payloadHash = DomainChecks.requireText(payloadHash, "payloadHash");
    approvalStatus = Objects.requireNonNull(approvalStatus, "approvalStatus");
    providerId = DomainChecks.requireText(providerId, "providerId");
    modelId = DomainChecks.requireText(modelId, "modelId");
    expertiseId = DomainChecks.requireText(expertiseId, "expertiseId");
  }

  /** Returns a safe receipt for failures before a projection exists. */
  public static PrivacyReceipt none() {
    return new PrivacyReceipt(
        "unknown", "unknown", ApprovalStatus.NOT_FOUND, "unknown", "unknown", "unknown");
  }
}
