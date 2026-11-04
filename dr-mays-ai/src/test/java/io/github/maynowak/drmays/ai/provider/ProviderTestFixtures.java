package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.ai.privacy.PrivacyGate;
import io.github.maynowak.drmays.ai.privacy.PrivacyPolicy;
import io.github.maynowak.drmays.ai.privacy.PrivacyProcessor;
import io.github.maynowak.drmays.ai.privacy.ProviderInvocationAuthorization;
import io.github.maynowak.drmays.ai.privacy.SanitizedExpertiseRequest;
import io.github.maynowak.drmays.ai.privacy.SanitizedFinding;
import io.github.maynowak.drmays.ai.privacy.SanitizedRelationship;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import java.time.Duration;
import java.util.List;

/** Shared provider test data with aliases only. */
final class ProviderTestFixtures {

  static final String PAYLOAD_JSON = PayloadIntegrity.canonicalJson(payload());
  static final String PAYLOAD_HASH = PayloadIntegrity.hash(PAYLOAD_JSON);

  private ProviderTestFixtures() {}

  static SanitizedExpertiseRequest payload() {
    return new SanitizedExpertiseRequest(
        "privacy-payload/v1",
        "privacy-v2",
        List.of(
            new SanitizedFinding(
                "NET-PUBLIC-001",
                "1.0",
                "HIGH",
                "res_safe",
                "security-group",
                "REGION",
                "Public ingress",
                "The security group declares public ingress.",
                "resource-attribute",
                "ingress.public=true",
                "ingress.public=false")),
        List.of(
            new SanitizedRelationship(
                "SECURITY_GROUP_IN_VPC",
                "res_safe",
                "vpc_safe",
                "resource-attribute",
                "VPC_REFERENCE")),
        "COMPLETE",
        List.of());
  }

  static ModelInvocationRequest request(
      String providerId, String modelId, ProviderCredentialReference reference) {
    return new ModelInvocationRequest(
        providerId,
        modelId,
        "standard",
        payload(),
        PAYLOAD_JSON,
        PAYLOAD_HASH,
        reference,
        Duration.ofSeconds(5),
        64_000);
  }

  static ModelInvocationRequest request(
      String providerId,
      String modelId,
      String payloadJson,
      ProviderCredentialReference reference,
      int maxOutputChars) {
    return new ModelInvocationRequest(
        providerId,
        modelId,
        "standard",
        payload(),
        payloadJson,
        PAYLOAD_HASH,
        reference,
        Duration.ofSeconds(5),
        maxOutputChars);
  }

  static AuthorizedRequest authorizedRequest(String providerId, String modelId) {
    var source = new IntelligenceRequest(List.of(), List.of(), GraphCompleteness.COMPLETE);
    var processed = new PrivacyProcessor(PrivacyPolicy.defaultPolicy()).process(source);
    var gate =
        new PrivacyGate(
            java.time.Duration.ofMinutes(5),
            java.time.Duration.ofMinutes(5),
            java.time.Clock.systemUTC(),
            id -> false);
    var preview = gate.register(source, processed, providerId, modelId, "standard");
    var invocation = gate.consume(preview.previewId(), null).invocation().orElseThrow();
    var request =
        new ModelInvocationRequest(
            providerId,
            modelId,
            "standard",
            processed.payload(),
            processed.payloadJson(),
            processed.payloadHash(),
            ProviderCredentialReference.none(providerId),
            Duration.ofSeconds(5),
            64_000);
    return new AuthorizedRequest(request, invocation.authorization());
  }

  record AuthorizedRequest(
      ModelInvocationRequest request, ProviderInvocationAuthorization authorization) {}
}
