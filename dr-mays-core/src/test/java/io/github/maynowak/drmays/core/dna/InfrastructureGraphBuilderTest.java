package io.github.maynowak.drmays.core.dna;

import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.ACCOUNT;
import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.REGION;
import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.bucket;
import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.identity;
import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.network;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InfrastructureGraphBuilderTest {

  private final InfrastructureGraphBuilder builder = new InfrastructureGraphBuilder();

  @Test
  void buildsSingleInventoriedNodeWithoutEdges() {
    var result = builder.build(List.of(bucket("bucket-1")));

    assertThat(result.completeness()).isEqualTo(GraphCompleteness.COMPLETE);
    assertThat(result.issues()).isEmpty();
    assertThat(result.graph().inventoriedNodes()).hasSize(1);
    assertThat(result.graph().referencedNodes()).isEmpty();
    assertThat(result.graph().edges()).isEmpty();
  }

  @Test
  void addsVpcReference() {
    var result = builder.build(List.of(network("sg-1", "vpc-1", null)));

    assertThat(result.graph().nodes()).hasSize(2);
    assertThat(result.graph().referencedNodes().getFirst().identity().resourceType())
        .isEqualTo("vpc");
    assertThat(result.graph().edges())
        .extracting(ResourceEdge::type)
        .containsExactly(RelationshipType.SECURITY_GROUP_IN_VPC);
    assertThat(result.graph().edges().getFirst().evidence().evidence().observed())
        .isEqualTo("vpc.id=vpc-1");
  }

  @Test
  void doesNotDowngradeInventoriedVpcToReference() {
    var vpc =
        new InfrastructureResource(
            identity("vpc", "vpc-1"), Map.of(), Map.of("state", "available"));
    var result = builder.build(List.of(network("sg-1", "vpc-1", null), vpc));

    assertThat(result.graph().inventoriedNodes()).hasSize(2);
    assertThat(result.graph().referencedNodes()).isEmpty();
  }

  @Test
  void addsReferencedSecurityGroupEdges() {
    var result = builder.build(List.of(network("sg-1", null, "sg-2,sg-3")));

    assertThat(result.graph().nodes()).hasSize(3);
    assertThat(result.graph().edges())
        .extracting(edge -> edge.to().resourceId())
        .containsExactly("sg-2", "sg-3");
    assertThat(result.graph().edges())
        .allMatch(edge -> edge.type() == RelationshipType.SECURITY_GROUP_REFERENCES_SECURITY_GROUP);
  }

  @Test
  void doesNotInventRelationshipsFromCountOnly() {
    var resource =
        new InfrastructureResource(
            identity("network", "sg-1"), Map.of(), Map.of("ingress.group.reference.count", "2"));

    var result = builder.build(List.of(resource));

    assertThat(result.graph().edges()).isEmpty();
    assertThat(result.graph().nodes()).hasSize(1);
  }

  @Test
  void keepsS3PropertiesAsNodeAttributes() {
    var result = builder.build(List.of(bucket("bucket-1")));

    var node = result.graph().inventoriedNodes().getFirst();
    assertThat(node.attributes())
        .containsEntry("s3.versioning.status", "Enabled")
        .containsEntry("s3.publicAccessBlock.enabled", "true");
    assertThat(result.graph().edges()).isEmpty();
  }

  @Test
  void separatesAccountsAndRegions() {
    var first =
        new InfrastructureResource(
            identity("network", "sg-1", ACCOUNT, REGION), Map.of(), Map.of("vpc.id", "vpc-1"));
    var second =
        new InfrastructureResource(
            identity("network", "sg-1", "111111111111", "us-east-1"),
            Map.of(),
            Map.of("vpc.id", "vpc-1"));

    var result = builder.build(List.of(first, second));

    assertThat(result.graph().nodes()).hasSize(4);
    assertThat(result.graph().edges()).hasSize(2);
    assertThat(result.graph().edges())
        .allMatch(edge -> edge.from().accountId().equals(edge.to().accountId()));
    assertThat(result.graph().edges())
        .allMatch(edge -> edge.from().region().equals(edge.to().region()));
  }

  @Test
  void marksPartialDiscovery() {
    var result = builder.build(List.of(bucket("bucket-1")), true);

    assertThat(result.completeness()).isEqualTo(GraphCompleteness.PARTIAL);
    assertThat(result.issues())
        .extracting(GraphValidationIssue::code)
        .containsExactly(GraphIssueCode.PARTIAL_DISCOVERY);
  }

  @Test
  void reportsDuplicateResourcesAndKeepsOneNode() {
    var duplicate = bucket("bucket-1");
    var result = builder.build(List.of(duplicate, duplicate));

    assertThat(result.graph().nodes()).hasSize(1);
    assertThat(result.issues())
        .extracting(GraphValidationIssue::code)
        .containsExactly(GraphIssueCode.DUPLICATE_RESOURCE);
  }

  @Test
  void ignoresBlankReferenceLists() {
    var result = builder.build(List.of(network("sg-1", " ", " , ")));

    assertThat(result.graph().edges()).isEmpty();
    assertThat(result.issues())
        .extracting(GraphValidationIssue::code)
        .containsExactly(GraphIssueCode.INVALID_REFERENCE);
  }
}
