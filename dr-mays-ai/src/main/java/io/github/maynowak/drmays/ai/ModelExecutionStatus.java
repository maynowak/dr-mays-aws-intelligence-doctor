package io.github.maynowak.drmays.ai;

/** Result state of an intelligence analysis. */
public enum ModelExecutionStatus {
  SUCCESS,
  DISABLED,
  APPROVAL_REQUIRED,
  UNAVAILABLE,
  FAILED,
  INVALID_RESPONSE
}
