package io.github.maynowak.drmays.ai.provider;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Exact provider/model allowlist. No wildcards, prefixes or implicit fallback. */
public final class ProviderModelPolicy {

  private final Map<String, Set<String>> allowedModels;

  private ProviderModelPolicy(Map<String, Set<String>> allowedModels) {
    this.allowedModels = Map.copyOf(Objects.requireNonNull(allowedModels, "allowedModels"));
  }

  /** Returns the mock-only policy used by the current application. */
  public static ProviderModelPolicy mockOnly() {
    return new ProviderModelPolicy(Map.of("mock", Set.of("deterministic-v1")));
  }

  /**
   * Creates an explicit policy.
   *
   * @param allowedModels exact provider/model mapping
   * @return immutable policy
   */
  public static ProviderModelPolicy of(Map<String, Set<String>> allowedModels) {
    var copy = new java.util.LinkedHashMap<String, Set<String>>();
    allowedModels.forEach((provider, models) ->
        copy.put(provider, Set.copyOf(Objects.requireNonNull(models, "models"))));
    return new ProviderModelPolicy(copy);
  }

  /**
   * Returns whether the exact provider/model pair is allowed.
   *
   * @param providerId provider identifier
   * @param modelId model identifier
   * @return true only for an explicit exact match
   */
  public boolean allows(String providerId, String modelId) {
    return allowedModels.getOrDefault(providerId, Set.of()).contains(modelId);
  }

  /** Returns the immutable allowlist. */
  public Map<String, Set<String>> allowedModels() {
    return allowedModels;
  }
}
