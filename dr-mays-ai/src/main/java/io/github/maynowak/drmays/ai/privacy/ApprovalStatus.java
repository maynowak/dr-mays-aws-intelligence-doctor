package io.github.maynowak.drmays.ai.privacy;

/** Result state of an approval-gated execution attempt. */
public enum ApprovalStatus {
  APPROVED,
  APPROVAL_REQUIRED,
  PRIVACY_REJECTED,
  NOT_FOUND,
  EXPIRED,
  MISMATCH,
  CONSUMED
}
