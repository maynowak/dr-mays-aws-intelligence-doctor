package io.github.maynowak.drmays.core.dna;

import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.identity;
import static io.github.maynowak.drmays.core.dna.GraphTestFixtures.network;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class GraphAnalyzerTest {

  private final InfrastructureGraphBuilder builder = new InfrastructureGraphBuilder();

  @Test
  void listsInventoriedAndReferencedNodes() {
    var graph = builder.build(List.of(network("sg-1", "vpc-1", "sg-2"))).graph();
    var analyzer = new GraphAnalyzer(graph);

    assertThat(analyzer.inventoriedNodes()).hasSize(1);
    assertThat(analyzer.referencedNodes()).hasSize(2);
  }

  @Test
  void returnsOutgoingIncomingAndDirectEdges() {
    var graph = builder.build(List.of(network("sg-1", "vpc-1", "sg-2"))).graph();
    var analyzer = new GraphAnalyzer(graph);
    var sg1 = identity("network", "sg-1");
    var vpc = identity("vpc", "vpc-1");

    assertThat(analyzer.outgoing(sg1)).hasSize(2);
    assertThat(analyzer.incoming(vpc)).hasSize(1);
    assertThat(analyzer.directEdges(vpc)).hasSize(1);
  }

  @Test
  void traversesWithinBoundedRadius() {
    var graph = builder.build(List.of(network("sg-1", "vpc-1", "sg-2"))).graph();
    var analyzer = new GraphAnalyzer(graph);
    var sg1 = identity("network", "sg-1");
    var sg2 = identity("network", "sg-2");

    assertThat(analyzer.reachable(sg1, 0)).hasSize(1);
    assertThat(analyzer.reachable(sg1, 1)).hasSize(3);
    assertThat(analyzer.reachable(sg2, 1)).hasSize(1);
  }

  @Test
  void rejectsUnknownStartAndOutOfRangeRadius() {
    var analyzer =
        new GraphAnalyzer(builder.build(List.of(network("sg-1", "vpc-1", null))).graph());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> analyzer.reachable(identity("network", "missing"), 1))
        .withMessageContaining("unknown graph node");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> analyzer.reachable(identity("network", "sg-1"), -1));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> analyzer.reachable(identity("network", "sg-1"), GraphAnalyzer.MAX_RADIUS + 1));
  }

  @Test
  void computesConnectedComponents() {
    var graph =
        builder
            .build(
                List.of(
                    network("sg-1", "vpc-1", null),
                    network("sg-2", "vpc-2", null),
                    new io.github.maynowak.drmays.core.resource.InfrastructureResource(
                        identity("s3-bucket", "bucket-1"), java.util.Map.of(), java.util.Map.of())))
            .graph();
    var analyzer = new GraphAnalyzer(graph);

    assertThat(analyzer.components()).hasSize(3);
    assertThat(analyzer.components()).extracting(List::size).containsExactly(2, 2, 1);
  }

  @Test
  void detectsCyclesAndHandlesSelfLoops() {
    var acyclic = new GraphAnalyzer(builder.build(List.of(network("sg-1", "vpc-1", null))).graph());
    assertThat(acyclic.hasCycles()).isFalse();

    var cyclic =
        builder.build(List.of(network("sg-1", null, "sg-2"), network("sg-2", null, "sg-1")));
    assertThat(new GraphAnalyzer(cyclic.graph()).hasCycles()).isTrue();

    var selfLoop = builder.build(List.of(network("sg-1", null, "sg-1")));
    assertThat(new GraphAnalyzer(selfLoop.graph()).hasCycles()).isTrue();
  }

  @Test
  void filtersByAccountAndRegion() {
    var resource =
        new io.github.maynowak.drmays.core.resource.InfrastructureResource(
            identity("network", "sg-1", "111111111111", "us-east-1"),
            java.util.Map.of(),
            java.util.Map.of("vpc.id", "vpc-1"));
    var analyzer = new GraphAnalyzer(builder.build(List.of(resource)).graph());

    assertThat(analyzer.nodesForAccount("111111111111")).hasSize(2);
    assertThat(analyzer.nodesForAccount("000000000000")).isEmpty();
    assertThat(analyzer.nodesForRegion("us-east-1")).hasSize(2);
    assertThat(analyzer.nodesForRegion("eu-west-1")).isEmpty();
  }
}
