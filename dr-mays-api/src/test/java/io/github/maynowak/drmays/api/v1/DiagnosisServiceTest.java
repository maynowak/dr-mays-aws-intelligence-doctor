package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiagnosisServiceTest {

  private final DiagnosisService service =
      new DiagnosisService(
          new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule())));

  @Test
  void diagnosesMissingOwnerAndPublicIngress() {
    var response = service.diagnose(networkRequest());

    assertThat(response.findingCount()).isEqualTo(2);
    assertThat(response.findings())
        .extracting(FindingResponse::ruleId)
        .containsExactly("NET-PUBLIC-001", "TAG-OWNER-001");
    assertThat(response.findings().get(0).severity()).isEqualTo("HIGH");
    assertThat(response.findings().get(0).evidence().observed()).isEqualTo("ingress.public=true");
    assertThat(response.findings().get(1).evidence().observed()).isEqualTo("absent");
    assertThat(response.outcomes())
        .extracting(OutcomeResponse::type)
        .containsExactly("FINDING", "FINDING");
  }

  @Test
  void returnsNoFindingsForCleanStorage() {
    var response = service.diagnose(cleanStorageRequest());

    assertThat(response.findingCount()).isZero();
    assertThat(response.findings()).isEmpty();
    assertThat(response.outcomes())
        .extracting(OutcomeResponse::type)
        .containsExactly("NOT_APPLICABLE", "NO_FINDING");
  }

  @Test
  void addsVerifiedGraphRelationshipsToFindings() {
    var request =
        new DiagnosisRequest(
            List.of(
                new ResourceRequest(
                    new ResourceIdentityRequest(
                        "aws", "123456789012", "eu-central-1", "network", "net-1"),
                    Map.of(),
                    Map.of("ingress.public", "true", "vpc.id", "vpc-1"))));

    var response = service.diagnose(request);

    assertThat(response.findings()).isNotEmpty();
    assertThat(response.findings().getFirst().relationships())
        .extracting(GraphEdgeResponse::type)
        .contains("SECURITY_GROUP_IN_VPC");
  }

  @Test
  void listsRulesInRuleIdOrder() {
    var response = service.rules();

    assertThat(response.rules())
        .extracting(RuleResponse::ruleId)
        .containsExactly("NET-PUBLIC-001", "TAG-OWNER-001");
    assertThat(response.rules()).extracting(RuleResponse::version).containsOnly("1.0");
  }

  @Test
  void isDeterministicAcrossResourceInputOrder() {
    var first = service.diagnose(twoResourceRequest(false));
    var second = service.diagnose(twoResourceRequest(true));

    assertThat(first).isEqualTo(second);
  }

  private static DiagnosisRequest networkRequest() {
    return new DiagnosisRequest(
        List.of(
            new ResourceRequest(
                new ResourceIdentityRequest(
                    "aws", "123456789012", "eu-central-1", "network", "net-1"),
                Map.of(),
                Map.of("ingress.public", "true"))));
  }

  private static DiagnosisRequest cleanStorageRequest() {
    return new DiagnosisRequest(
        List.of(
            new ResourceRequest(
                new ResourceIdentityRequest(
                    "aws", "123456789012", "eu-central-1", "storage", "store-1"),
                Map.of("owner", "platform"),
                Map.of())));
  }

  private static DiagnosisRequest twoResourceRequest(boolean reverse) {
    var network =
        new ResourceRequest(
            new ResourceIdentityRequest("aws", "123456789012", "eu-central-1", "network", "net-1"),
            Map.of(),
            Map.of("ingress.public", "true"));
    var storage =
        new ResourceRequest(
            new ResourceIdentityRequest(
                "aws", "123456789012", "eu-central-1", "storage", "store-1"),
            Map.of("owner", "platform"),
            Map.of());
    return new DiagnosisRequest(reverse ? List.of(storage, network) : List.of(network, storage));
  }
}
