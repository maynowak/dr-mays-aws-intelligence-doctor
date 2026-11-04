package io.github.maynowak.drmays.ai.provider;

import java.util.Objects;
import java.util.Set;

/** Enforces provider allowlisting and explicit failure instead of fallback. */
public final class ProviderRouter {

  private final ProviderRegistry registry;
  private final Set<String> enabledProviderIds;
  private final boolean credentialResolutionEnabled;
  private final CredentialReferenceValidator credentialValidator =
      new CredentialReferenceValidator();

  /**
   * Creates a router with credential resolution disabled.
   *
   * @param registry provider registry
   * @param enabledProviderIds explicitly enabled provider IDs
   */
  public ProviderRouter(ProviderRegistry registry, Set<String> enabledProviderIds) {
    this(registry, enabledProviderIds, false);
  }

  /**
   * Creates a router with explicit credential-resolution policy.
   *
   * @param registry provider registry
   * @param enabledProviderIds explicitly enabled provider IDs
   * @param credentialResolutionEnabled whether real credential resolution is enabled
   */
  public ProviderRouter(
      ProviderRegistry registry,
      Set<String> enabledProviderIds,
      boolean credentialResolutionEnabled) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.enabledProviderIds =
        Set.copyOf(Objects.requireNonNull(enabledProviderIds, "enabledProviderIds"));
    this.credentialResolutionEnabled = credentialResolutionEnabled;
  }

  /**
   * Routes exactly one invocation; no fallback provider is attempted.
   *
   * @param request bounded request
   * @return bounded result
   */
  public ModelInvocationResult route(ModelInvocationRequest request) {
    Objects.requireNonNull(request, "request");
    var provider = registry.find(request.providerId());
    if (provider.isEmpty()) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.NOT_ALLOWED,
          request.providerId(),
          request.modelId(),
          "PROVIDER_NOT_REGISTERED");
    }
    if (!enabledProviderIds.contains(request.providerId())) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.DISABLED,
          request.providerId(),
          request.modelId(),
          "PROVIDER_DISABLED");
    }
    var descriptor = provider.orElseThrow().descriptor();
    if (request.payloadJson().length() > descriptor.capabilities().maxPayloadChars()) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.PRIVACY_REJECTED,
          request.providerId(),
          request.modelId(),
          "PAYLOAD_TOO_LARGE");
    }
    var credential = credentialValidator.validate(request.credentialReference(), descriptor);
    if (!credential.allowed()) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE,
          request.providerId(),
          request.modelId(),
          "CREDENTIAL_REFERENCE_INVALID");
    }
    if (descriptor.requiresCredential() && !credentialResolutionEnabled) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE,
          request.providerId(),
          request.modelId(),
          "CREDENTIAL_RESOLUTION_DISABLED");
    }
    try {
      var result = provider.orElseThrow().invoke(request);
      if (result == null) {
        return ModelInvocationResult.failure(
            ProviderExecutionStatus.FAILED,
            request.providerId(),
            request.modelId(),
            "PROVIDER_RETURNED_NULL");
      }
      if (result.outputChars() > descriptor.capabilities().maxOutputChars()) {
        return ModelInvocationResult.failure(
            ProviderExecutionStatus.INVALID_RESPONSE,
            request.providerId(),
            request.modelId(),
            "OUTPUT_TOO_LARGE");
      }
      return result;
    } catch (RuntimeException exception) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.UNAVAILABLE,
          request.providerId(),
          request.modelId(),
          "PROVIDER_EXCEPTION");
    }
  }

  /** Returns the enabled provider allowlist. */
  public Set<String> enabledProviderIds() {
    return enabledProviderIds;
  }
}
