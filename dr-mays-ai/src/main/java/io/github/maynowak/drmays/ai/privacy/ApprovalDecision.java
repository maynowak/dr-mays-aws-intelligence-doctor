package io.github.maynowak.drmays.ai.privacy;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of consuming an approval.
 *
 * @param status approval outcome
 * @param invocation approved invocation material when successful
 */
public record ApprovalDecision(ApprovalStatus status, Optional<ApprovedInvocation> invocation) {

  public ApprovalDecision {
    status = Objects.requireNonNull(status, "status");
    invocation = invocation == null ? Optional.empty() : invocation;
  }

  /**
   * Creates an approved decision.
   *
   * @param invocation approved material
   * @return decision
   */
  public static ApprovalDecision approved(ApprovedInvocation invocation) {
    return new ApprovalDecision(ApprovalStatus.APPROVED, Optional.of(invocation));
  }

  /**
   * Creates a rejected decision without invocation material.
   *
   * @param status failure status
   * @return decision
   */
  public static ApprovalDecision rejected(ApprovalStatus status) {
    return new ApprovalDecision(status, Optional.empty());
  }
}
