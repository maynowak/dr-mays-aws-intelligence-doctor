package io.github.maynowak.drmays.ai.provider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Explicit provider catalog; duplicate IDs are rejected. */
public final class ProviderRegistry {

  private final Map<String, ModelProvider> providers = new LinkedHashMap<>();

  /** Creates an empty registry. */
  public ProviderRegistry() {}

  /**
   * Creates a registry from providers.
   *
   * @param providers initial providers
   */
  public ProviderRegistry(List<ModelProvider> providers) {
    Objects.requireNonNull(providers, "providers").forEach(this::register);
  }

  /**
   * Registers a provider.
   *
   * @param provider provider to register
   */
  public void register(ModelProvider provider) {
    var value = Objects.requireNonNull(provider, "provider");
    var id = value.descriptor().id();
    if (providers.putIfAbsent(id, value) != null) {
      throw new IllegalArgumentException("duplicate provider: " + id);
    }
  }

  /**
   * Finds a provider by ID.
   *
   * @param id provider identifier
   * @return provider when registered
   */
  public Optional<ModelProvider> find(String id) {
    return Optional.ofNullable(providers.get(Objects.requireNonNull(id, "id")));
  }

  /** Returns registered provider descriptors in registration order. */
  public List<ModelProviderDescriptor> descriptors() {
    return providers.values().stream().map(ModelProvider::descriptor).toList();
  }

  /** Returns registered provider IDs. */
  public Set<String> ids() {
    return Set.copyOf(providers.keySet());
  }
}
