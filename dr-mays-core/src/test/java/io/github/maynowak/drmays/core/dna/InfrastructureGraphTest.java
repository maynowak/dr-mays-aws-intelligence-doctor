package io.github.maynowak.drmays.core.dna;

import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.edge;
import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InfrastructureGraphTest {

  @Test
  void sortsNodesAndEdgesDeterministically() {
    var a = identity("network", "sg-a");
    var b = identity("network", "sg-b");
    var c = identity("vpc", "vpc-1");
    var graph =
        new InfrastructureGraph(
            List.of(
                node(b, ResourceResolutionStatus.REFERENCED),
                node(a, ResourceResolutionStatus.INVENTORIED),
                node(c, ResourceResolutionStatus.REFERENCED)),
            List.of(edge(b, c), edge(a, b)));

    assertThat(graph.nodes())
        .extracting(node -> node.identity().resourceId())
        .containsExactly("sg-a", "sg-b", "vpc-1");
    assertThat(graph.edges())
        .extracting(edge -> edge.from().resourceId() + "->" + edge.to().resourceId())
        .containsExactly("sg-a->sg-b", "sg-b->vpc-1");
  }

  @Test
  void rejectsDuplicateNodesAndMissingEndpoints() {
    var a = identity("network", "sg-a");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new InfrastructureGraph(
                List.of(
                    node(a, ResourceResolutionStatus.INVENTORIED),
                    node(a, ResourceResolutionStatus.INVENTORIED)),
                List.of()))
        .withMessageContaining("duplicate graph node");

    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new InfrastructureGraph(
                List.of(node(a, ResourceResolutionStatus.INVENTORIED)),
                List.of(edge(a, identity("vpc", "missing")))))
        .withMessageContaining("endpoint is not present");
  }

  @Test
  void rejectsDuplicateEdges() {
    var a = identity("network", "sg-a");
    var b = identity("vpc", "vpc-1");

    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new InfrastructureGraph(
                List.of(
                    node(a, ResourceResolutionStatus.INVENTORIED),
                    node(b, ResourceResolutionStatus.REFERENCED)),
                List.of(edge(a, b), edge(a, b))))
        .withMessageContaining("duplicate graph edge");
  }

  @Test
  void exposesLookupsAndResolutionStatus() {
    var inventoried = identity("network", "sg-a");
    var referenced = identity("vpc", "vpc-1");
    var graph =
        new InfrastructureGraph(
            List.of(
                node(inventoried, ResourceResolutionStatus.INVENTORIED),
                node(referenced, ResourceResolutionStatus.REFERENCED)),
            List.of(edge(inventoried, referenced)));

    assertThat(graph.node(inventoried)).isPresent();
    assertThat(graph.inventoriedNodes()).hasSize(1);
    assertThat(graph.referencedNodes()).hasSize(1);
    assertThat(graph.outgoing(inventoried)).hasSize(1);
    assertThat(graph.incoming(referenced)).hasSize(1);
    assertThat(graph.directEdges(referenced)).hasSize(1);
    assertThat(graph.identities()).containsExactlyInAnyOrder(inventoried, referenced);
  }

  @Test
  void emptyGraphIsEmpty() {
    var graph = InfrastructureGraph.empty();

    assertThat(graph.nodes()).isEmpty();
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  void rejectsEvidenceForAnotherSource() {
    var source = identity("network", "sg-a");
    var other = identity("network", "sg-b");
    var target = identity("vpc", "vpc-1");
    var evidence =
        new RelationshipEvidence(
            new io.github.maynowak.drmays.core.evidence.Evidence(
                other, "test", "observed", "expected", Map.of()));

    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new ResourceEdge(source, target, RelationshipType.SECURITY_GROUP_IN_VPC, evidence))
        .withMessageContaining("evidence resource must match edge source");
  }

  private static ResourceNode node(
      io.github.maynowak.drmays.core.resource.ResourceIdentity identity,
      ResourceResolutionStatus status) {
    return new ResourceNode(identity, status, Map.of(), Map.of());
  }
}
