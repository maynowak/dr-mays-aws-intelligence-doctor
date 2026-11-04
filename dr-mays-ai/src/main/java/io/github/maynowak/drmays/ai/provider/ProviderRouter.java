package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.ai.privacy.ProviderInvocationAuthorization;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Enforces authorization, allowlists and explicit failure instead of fallback. */
public final class ProviderRouter {

  private static final int MAX_USED_AUTHORIZATIONS = 1_000;

  private final ProviderRegistry registry;
  private final Set<String> enabledProviderIds;
  private final ProviderModelPolicy modelPolicy;
  private final CredentialResolutionPolicy credentialPolicy;
  private final Clock clock;
  private final CredentialReferenceValidator credentialValidator =
      new CredentialReferenceValidator();
  private final DisabledCredentialResolver credentialResolver = new DisabledCredentialResolver();
  private final Set<String> usedAuthorizations = new LinkedHashSet<>();

  /**
   * Creates a router with the mock-only model policy and disabled credential resolution.
   *
   * @param registry provider registry
   * @param enabledProviderIds explicitly enabled provider IDs
   */
  public ProviderRouter(ProviderRegistry registry, Set<String> enabledProviderIds) {
    this(
        registry,
        enabledProviderIds,
        ProviderModelPolicy.mockOnly(),
        CredentialResolutionPolicy.disabled(),
        Clock.systemUTC());
  }

  /**
   * Creates a router with explicit security policies.
   *
   * @param registry provider registry
   * @param enabledProviderIds explicitly enabled provider IDs
   * @param modelPolicy exact model allowlist
   * @param credentialPolicy disabled or future credential policy
   * @param clock expiration clock
   */
  public ProviderRouter(
      ProviderRegistry registry,
      Set<String> enabledProviderIds,
      ProviderModelPolicy modelPolicy,
      CredentialResolutionPolicy credentialPolicy,
      Clock clock) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.enabledProviderIds =
        Set.copyOf(Objects.requireNonNull(enabledProviderIds, "enabledProviderIds"));
    this.modelPolicy = Objects.requireNonNull(modelPolicy, "modelPolicy");
    this.credentialPolicy = Objects.requireNonNull(credentialPolicy, "credentialPolicy");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Routes exactly one authorized invocation. No provider is called without a server-minted
   * authorization bound to this exact request.
   *
   * @param request bounded request
   * @param authorization server-minted authorization
   * @return bounded result
   */
  public synchronized ModelInvocationResult route(
      ModelInvocationRequest request, ProviderInvocationAuthorization authorization) {
    Objects.requireNonNull(request, "request");
    if (authorization == null) {
      return failure(request, ProviderExecutionStatus.APPROVAL_REQUIRED, "AUTHORIZATION_MISSING");
    }
    if (authorization.expired(clock.instant())) {
      return failure(request, ProviderExecutionStatus.APPROVAL_REQUIRED, "AUTHORIZATION_EXPIRED");
    }
    if (usedAuthorizations.contains(authorization.nonce())) {
      return failure(request, ProviderExecutionStatus.APPROVAL_REQUIRED, "AUTHORIZATION_CONSUMED");
    }
    var provider = registry.find(request.providerId());
    if (provider.isEmpty()) {
      return failure(request, ProviderExecutionStatus.NOT_ALLOWED, "PROVIDER_NOT_REGISTERED");
    }
    if (!enabledProviderIds.contains(request.providerId())) {
      return failure(request, ProviderExecutionStatus.DISABLED, "PROVIDER_DISABLED");
    }
    if (!modelPolicy.allows(request.providerId(), request.modelId())) {
      return failure(request, ProviderExecutionStatus.MODEL_NOT_ALLOWED, "MODEL_NOT_ALLOWED");
    }
    var descriptor = provider.orElseThrow().descriptor();
    if (descriptor.capabilities().toolsAllowed()) {
      return failure(request, ProviderExecutionStatus.PRIVACY_REJECTED, "TOOLS_NOT_ALLOWED");
    }
    if (!bindingMatches(request, authorization)) {
      return failure(request, ProviderExecutionStatus.APPROVAL_REQUIRED, "AUTHORIZATION_MISMATCH");
    }
    if (request.payloadJson().length() > descriptor.capabilities().maxPayloadChars()) {
      return failure(request, ProviderExecutionStatus.PRIVACY_REJECTED, "PAYLOAD_TOO_LARGE");
    }
    var credential = credentialValidator.validate(request.credentialReference(), descriptor);
    if (!credential.allowed()) {
      return failure(
          request, ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE, "CREDENTIAL_REFERENCE_INVALID");
    }
    if (descriptor.requiresCredential()) {
      if (!credentialPolicy.allows(request.credentialReference().kind())) {
        return failure(
            request,
            ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE,
            "CREDENTIAL_RESOLUTION_DISABLED");
      }
      if (credentialResolver.resolve(request.credentialReference())
          != CredentialAccessDecision.RESOLVED) {
        return failure(
            request,
            ProviderExecutionStatus.CREDENTIAL_UNAVAILABLE,
            "CREDENTIAL_RESOLVER_DISABLED");
      }
    }
    usedAuthorizations.add(authorization.nonce());
    if (usedAuthorizations.size() > MAX_USED_AUTHORIZATIONS) {
      usedAuthorizations.remove(usedAuthorizations.iterator().next());
    }
    try {
      var result = provider.orElseThrow().invoke(request);
      if (result == null) {
        return failure(request, ProviderExecutionStatus.FAILED, "PROVIDER_RETURNED_NULL");
      }
      if (result.outputChars() > descriptor.capabilities().maxOutputChars()) {
        return failure(request, ProviderExecutionStatus.INVALID_RESPONSE, "OUTPUT_TOO_LARGE");
      }
      return result;
    } catch (RuntimeException exception) {
      return failure(request, ProviderExecutionStatus.UNAVAILABLE, "PROVIDER_EXCEPTION");
    }
  }

  private static boolean bindingMatches(
      ModelInvocationRequest request, ProviderInvocationAuthorization authorization) {
    return authorization.providerId().equals(request.providerId())
        && authorization.modelId().equals(request.modelId())
        && authorization.expertiseId().equals(request.expertiseId())
        && authorization.payloadHash().equals(request.payloadHash())
        && authorization.policyVersion().equals(request.payload().policyVersion());
  }

  private static ModelInvocationResult failure(
      ModelInvocationRequest request, ProviderExecutionStatus status, String code) {
    return ModelInvocationResult.failure(status, request.providerId(), request.modelId(), code);
  }

  /** Returns the enabled provider allowlist. */
  public Set<String> enabledProviderIds() {
    return enabledProviderIds;
  }

  /** Returns the current instant used for authorization expiry checks. */
  Instant now() {
    return clock.instant();
  }
}
