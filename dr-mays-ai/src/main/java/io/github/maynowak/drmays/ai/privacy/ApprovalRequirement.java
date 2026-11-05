package io.github.maynowak.drmays.ai.privacy;

/** Server-owned policy deciding whether a provider requires a human approval token. */
@FunctionalInterface
public interface ApprovalRequirement {

  /**
   * Returns whether approval is required for the provider.
   *
   * @param providerId provider identifier
   * @return true when a token is required
   */
  boolean requiresApproval(String providerId);
}
