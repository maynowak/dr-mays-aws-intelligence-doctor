package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraphServiceTest {

  private final GraphService service =
      new GraphService(
          new DiagnosisService(
              new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule()))));

  @Test
  void buildsSingleNodeGraph() {
    var response = service.graph(new GraphRequest(List.of(bucket("bucket-1")), false));

    assertThat(response.nodes()).hasSize(1);
    assertThat(response.edges()).isEmpty();
    assertThat(response.stats().inventoriedCount()).isEqualTo(1);
    assertThat(response.stats().componentCount()).isEqualTo(1);
    assertThat(response.completeness()).isEqualTo("COMPLETE");
  }

  @Test
  void exposesVpcReferenceAsEdge() {
    var response = service.graph(new GraphRequest(List.of(network("sg-1", "vpc-1", null)), false));

    assertThat(response.nodes()).hasSize(2);
    assertThat(response.edges()).hasSize(1);
    assertThat(response.edges().getFirst().type()).isEqualTo("SECURITY_GROUP_IN_VPC");
    assertThat(response.nodes())
        .filteredOn(node -> node.resource().resourceType().equals("vpc"))
        .allMatch(node -> node.status().equals("REFERENCED"));
  }

  @Test
  void exposesReferencedSecurityGroupEdgeOnlyWhenIdIsPresent() {
    var withReference =
        service.graph(new GraphRequest(List.of(network("sg-1", null, "sg-2")), false));
    var countOnlyResource =
        new ResourceRequest(
            new ResourceIdentityRequest("aws", "000000000000", "eu-west-1", "network", "sg-1"),
            Map.of(),
            Map.of("ingress.group.reference.count", "1"));
    var countOnly = service.graph(new GraphRequest(List.of(countOnlyResource), false));

    assertThat(withReference.edges()).hasSize(1);
    assertThat(withReference.edges().getFirst().type())
        .isEqualTo("SECURITY_GROUP_REFERENCES_SECURITY_GROUP");
    assertThat(countOnly.edges()).isEmpty();
  }

  @Test
  void keepsS3MetadataAsNodeAttributes() {
    var response = service.graph(new GraphRequest(List.of(bucket("bucket-1")), false));

    assertThat(response.nodes().getFirst().attributes())
        .containsEntry("s3.versioning.status", "Enabled");
    assertThat(response.edges()).isEmpty();
  }

  @Test
  void exposesPartialDiscovery() {
    var response = service.graph(new GraphRequest(List.of(bucket("bucket-1")), true));

    assertThat(response.completeness()).isEqualTo("PARTIAL");
    assertThat(response.issues())
        .extracting(GraphIssueResponse::code)
        .contains("PARTIAL_DISCOVERY");
  }

  @Test
  void separatesAccounts() {
    var first = network("sg-1", "vpc-1", null, "000000000000", "eu-west-1");
    var second = network("sg-1", "vpc-1", null, "111111111111", "us-east-1");

    var response = service.graph(new GraphRequest(List.of(first, second), false));

    assertThat(response.nodes()).hasSize(4);
    assertThat(response.edges()).hasSize(2);
    assertThat(response.edges())
        .allMatch(edge -> edge.from().accountId().equals(edge.to().accountId()));
  }

  @Test
  void detectsCyclesInReferenceGraph() {
    var response =
        service.graph(
            new GraphRequest(
                List.of(network("sg-1", null, "sg-2"), network("sg-2", null, "sg-1")), false));

    assertThat(response.stats().cyclic()).isTrue();
    assertThat(response.stats().componentCount()).isEqualTo(1);
  }

  @Test
  void isDeterministicForRepeatedRequests() {
    var request = new GraphRequest(List.of(network("sg-1", "vpc-1", "sg-2")), false);

    assertThat(service.graph(request)).isEqualTo(service.graph(request));
  }

  private static ResourceRequest network(String id, String vpcId, String references) {
    return network(id, vpcId, references, "000000000000", "eu-west-1");
  }

  private static ResourceRequest network(
      String id, String vpcId, String references, String account, String region) {
    var attributes = new java.util.LinkedHashMap<String, String>();
    if (vpcId != null) {
      attributes.put("vpc.id", vpcId);
    }
    if (references != null) {
      attributes.put("ingress.referenced.securityGroupIds", references);
    }
    return new ResourceRequest(
        new ResourceIdentityRequest("aws", account, region, "network", id), Map.of(), attributes);
  }

  private static ResourceRequest bucket(String id) {
    return new ResourceRequest(
        new ResourceIdentityRequest("aws", "000000000000", "eu-west-1", "s3-bucket", id),
        Map.of("owner", "platform"),
        Map.of("s3.versioning.status", "Enabled"));
  }
}
