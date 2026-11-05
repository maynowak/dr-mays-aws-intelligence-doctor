package io.github.maynowak.drmays.ai.provider;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Server-owned approval policy. Only the exact deterministic mock implementation is exempt; no
 * descriptor flag or provider ID can grant the exemption.
 */
public final class ProviderApprovalPolicy
    implements io.github.maynowak.drmays.ai.privacy.ApprovalRequirement {

  private final Set<String> exemptProviderIds;

  private ProviderApprovalPolicy(Set<String> exemptProviderIds) {
    this.exemptProviderIds =
        Set.copyOf(Objects.requireNonNull(exemptProviderIds, "exemptProviderIds"));
  }

  /** Returns a strict policy with no exemptions. */
  public static ProviderApprovalPolicy strict() {
    return new ProviderApprovalPolicy(Set.of());
  }

  /**
   * Derives exemptions from the concrete provider type, not descriptor fields.
   *
   * @param registry provider registry
   * @return immutable policy
   */
  public static ProviderApprovalPolicy forRegistry(ProviderRegistry registry) {
    Objects.requireNonNull(registry, "registry");
    var exempt = new HashSet<String>();
    for (var id : registry.ids()) {
      var provider = registry.find(id).orElseThrow();
      if (provider.getClass() == MockModelProvider.class) {
        exempt.add(id);
      }
    }
    return new ProviderApprovalPolicy(exempt);
  }

  /**
   * Returns whether a human approval token is required.
   *
   * @param providerId provider identifier
   * @return true unless the exact mock is registered under this ID
   */
  public boolean requiresApproval(String providerId) {
    return !exemptProviderIds.contains(providerId);
  }
}
