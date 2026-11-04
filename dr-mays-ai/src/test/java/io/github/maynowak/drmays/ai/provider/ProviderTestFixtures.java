package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.ai.privacy.SanitizedExpertiseRequest;
import io.github.maynowak.drmays.ai.privacy.SanitizedFinding;
import io.github.maynowak.drmays.ai.privacy.SanitizedRelationship;
import java.time.Duration;
import java.util.List;

/** Shared provider test data with aliases only. */
final class ProviderTestFixtures {

  static final String PAYLOAD_JSON = "{\"payloadVersion\":\"privacy-payload/v1\"}";

  private ProviderTestFixtures() {}

  static SanitizedExpertiseRequest payload() {
    return new SanitizedExpertiseRequest(
        "privacy-payload/v1",
        "privacy-v1",
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
        providerId, modelId, payload(), PAYLOAD_JSON, reference, Duration.ofSeconds(5), 64_000);
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
        payload(),
        payloadJson,
        reference,
        Duration.ofSeconds(5),
        maxOutputChars);
  }
}
