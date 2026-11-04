package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.ai.MockIntelligenceAnalyzer;
import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntelligenceServiceTest {

  private final DiagnosisService diagnosisService =
      new DiagnosisService(
          new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule())));
  private final IntelligenceService service =
      new IntelligenceService(diagnosisService, new MockIntelligenceAnalyzer());

  @Test
  void explainsDeterministicFindings() {
    var response = service.explain(new IntelligenceExplainRequest(List.of(network()), false));

    assertThat(response.status()).isEqualTo("SUCCESS");
    assertThat(response.explanations()).hasSize(2);
    assertThat(response.evidenceReferences()).isNotEmpty();
    assertThat(response.metadata().provider()).isEqualTo("mock");
  }

  @Test
  void failsClosedForLegacyExternalPath() {
    var gated =
        new IntelligenceService(
            diagnosisService,
            new io.github.maynowak.drmays.ai.PrivacyGatedLegacyAnalyzer(
                new io.github.maynowak.drmays.ai.ModelMetadata(
                    "legacy", "approval-required", "1.0")));

    var response = gated.explain(new IntelligenceExplainRequest(List.of(network()), false));

    assertThat(response.status()).isEqualTo("APPROVAL_REQUIRED");
    assertThat(response.explanations()).isEmpty();
  }

  @Test
  void returnsNoExplanationsForHealthyResource() {
    var response = service.explain(new IntelligenceExplainRequest(List.of(healthyBucket()), false));

    assertThat(response.status()).isEqualTo("SUCCESS");
    assertThat(response.explanations()).isEmpty();
    assertThat(response.uncertainties()).anyMatch(value -> value.contains("No deterministic"));
  }

  @Test
  void exposesPartialDiscoveryUncertainty() {
    var response = service.explain(new IntelligenceExplainRequest(List.of(network()), true));

    assertThat(response.uncertainties()).anyMatch(value -> value.contains("partial"));
  }

  @Test
  void includesVerifiedGraphRelationships() {
    var response = service.explain(new IntelligenceExplainRequest(List.of(network()), false));

    assertThat(response.verifiedRelationships()).isNotEmpty();
    assertThat(response.verifiedRelationships().getFirst().type())
        .isEqualTo("SECURITY_GROUP_IN_VPC");
  }

  @Test
  void isDeterministic() {
    var request = new IntelligenceExplainRequest(List.of(network()), false);

    assertThat(service.explain(request)).isEqualTo(service.explain(request));
  }

  private static ResourceRequest network() {
    return new ResourceRequest(
        new ResourceIdentityRequest("aws", "000000000000", "eu-west-1", "network", "net-1"),
        Map.of(),
        Map.of("ingress.public", "true", "vpc.id", "vpc-1"));
  }

  private static ResourceRequest healthyBucket() {
    return new ResourceRequest(
        new ResourceIdentityRequest("aws", "000000000000", "eu-west-1", "s3-bucket", "bucket-1"),
        Map.of("owner", "platform"),
        Map.of("s3.versioning.status", "Enabled"));
  }
}
