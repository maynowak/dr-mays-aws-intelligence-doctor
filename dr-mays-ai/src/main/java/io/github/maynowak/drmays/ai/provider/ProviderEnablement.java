package io.github.maynowak.drmays.ai.provider;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/** Trusted server-side enablement snapshot. Registration is not enablement. */
public final class ProviderEnablement {

  private final Set<String> enabledProviderIds;

  private ProviderEnablement(Set<String> enabledProviderIds) {
    this.enabledProviderIds = Set.copyOf(enabledProviderIds);
  }

  /** Returns the default mock-only enablement. */
  public static ProviderEnablement mockOnly() {
    return new ProviderEnablement(Set.of("mock"));
  }

  /**
   * Creates an explicit enablement from trusted server configuration.
   *
   * @param registry provider registry
   * @param providerIds explicitly enabled provider IDs
   * @return immutable enablement snapshot
   */
  public static ProviderEnablement of(ProviderRegistry registry, Collection<String> providerIds) {
    Objects.requireNonNull(registry, "registry");
    Objects.requireNonNull(providerIds, "providerIds");
    var copy = new java.util.LinkedHashSet<String>();
    for (var id : providerIds) {
      if (id == null
          || !id.matches("[a-z][a-z0-9-]{1,31}")
          || id.contains("*")
          || id.contains("?")) {
        throw new IllegalArgumentException("invalid provider enablement id");
      }
      if (!registry.contains(id)) {
        throw new IllegalArgumentException("provider is not registered");
      }
      if ("mock".equals(id)) {
        var provider = registry.find(id).orElseThrow();
        if (provider.getClass() != MockModelProvider.class) {
          throw new IllegalArgumentException("provider id mock is reserved");
        }
      }
      copy.add(id);
    }
    return new ProviderEnablement(copy);
  }

  /**
   * Returns whether a provider is enabled.
   *
   * @param providerId provider identifier
   * @return true only when explicitly enabled
   */
  public boolean isEnabled(String providerId) {
    return enabledProviderIds.contains(providerId);
  }

  /** Returns the immutable enabled-provider set. */
  public Set<String> enabledProviderIds() {
    return enabledProviderIds;
  }
}
