package io.github.maynowak.drmays.api.v1;

/**
 * Single-use opaque approval token.
 *
 * @param approvalToken token value; never log it
 */
public record ExpertiseApprovalResponse(String approvalToken) {

  @Override
  public String toString() {
    return "ExpertiseApprovalResponse[approvalToken=redacted]";
  }
}
