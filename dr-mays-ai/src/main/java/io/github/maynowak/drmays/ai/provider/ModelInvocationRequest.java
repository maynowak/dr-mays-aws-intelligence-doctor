package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.ai.privacy.SanitizedExpertiseRequest;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.time.Duration;
import java.util.Objects;

/**
 * Bounded provider invocation request. The payload is already sanitized.
 *
 * @param providerId selected provider
 * @param modelId selected model
 * @param expertiseId selected expertise profile
 * @param payload sanitized provider payload
 * @param payloadJson exact serialized payload used for size checks
 * @param payloadHash exact sanitized payload hash
 * @param credentialReference logical credential reference
 * @param timeout hard invocation timeout
 * @param maxOutputChars maximum accepted output
 */
public record ModelInvocationRequest(
    String providerId,
    String modelId,
    String expertiseId,
    SanitizedExpertiseRequest payload,
    String payloadJson,
    String payloadHash,
    ProviderCredentialReference credentialReference,
    Duration timeout,
    int maxOutputChars) {

  public ModelInvocationRequest {
    providerId = DomainChecks.requireText(providerId, "providerId");
    modelId = DomainChecks.requireText(modelId, "modelId");
    if (!modelId.matches("[A-Za-z0-9._:/-]{1,128}")) {
      throw new IllegalArgumentException("modelId is invalid");
    }
    expertiseId = DomainChecks.requireText(expertiseId, "expertiseId");
    payload = Objects.requireNonNull(payload, "payload");
    payloadJson = DomainChecks.requireText(payloadJson, "payloadJson");
    payloadHash = DomainChecks.requireText(payloadHash, "payloadHash");
    credentialReference = Objects.requireNonNull(credentialReference, "credentialReference");
    timeout = Objects.requireNonNull(timeout, "timeout");
    if (timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
    if (maxOutputChars <= 0) {
      throw new IllegalArgumentException("maxOutputChars must be positive");
    }
  }
}
