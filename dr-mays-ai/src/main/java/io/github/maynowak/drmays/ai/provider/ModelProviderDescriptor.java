package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Explicit provider configuration and safety requirements.
 *
 * @param id stable provider identifier
 * @param displayName human-readable name
 * @param kind execution category
 * @param capabilities bounded capabilities
 * @param requiresCredential whether a credential reference is required
 * @param requiresApproval whether execution needs a valid approval token
 * @param enabledByDefault whether the provider is enabled without an explicit allowlist entry
 */
public record ModelProviderDescriptor(
    String id,
    String displayName,
    ProviderKind kind,
    ProviderCapabilities capabilities,
    boolean requiresCredential,
    boolean requiresApproval,
    boolean enabledByDefault) {

  public ModelProviderDescriptor {
    id = DomainChecks.requireText(id, "id");
    if (!id.matches("[a-z][a-z0-9-]{1,31}")) {
      throw new IllegalArgumentException("id must be a short lowercase provider identifier");
    }
    displayName = DomainChecks.requireText(displayName, "displayName");
    kind = Objects.requireNonNull(kind, "kind");
    capabilities = Objects.requireNonNull(capabilities, "capabilities");
  }

  /** Returns the deterministic local mock descriptor. */
  public static ModelProviderDescriptor mock() {
    return new ModelProviderDescriptor(
        "mock",
        "Deterministic mock",
        ProviderKind.DETERMINISTIC_MOCK,
        new ProviderCapabilities(true, true, false, 32_000, 64_000),
        false,
        false,
        true);
  }

  /** Returns the disabled local Ollama descriptor. */
  public static ModelProviderDescriptor ollama() {
    return new ModelProviderDescriptor(
        "ollama",
        "Local Ollama",
        ProviderKind.LOCAL_MODEL,
        new ProviderCapabilities(true, true, false, 32_000, 64_000),
        false,
        true,
        false);
  }

  /** Returns the disabled OpenCode CLI descriptor. */
  public static ModelProviderDescriptor openCode() {
    return new ModelProviderDescriptor(
        "opencode",
        "OpenCode CLI",
        ProviderKind.CLI_AGENT,
        new ProviderCapabilities(true, false, false, 32_000, 64_000),
        true,
        true,
        false);
  }
}
