package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable directed graph of inventoried and referenced infrastructure resources.
 *
 * @param nodes graph nodes, sorted by canonical identity
 * @param edges directed edges, sorted by source, target and type
 */
public record InfrastructureGraph(List<ResourceNode> nodes, List<ResourceEdge> edges) {

  public InfrastructureGraph {
    Objects.requireNonNull(nodes, "nodes");
    Objects.requireNonNull(edges, "edges");

    var sortedNodes = new ArrayList<ResourceNode>(nodes.size());
    var identities = new HashSet<ResourceIdentity>();
    for (var node : nodes) {
      var value = Objects.requireNonNull(node, "node");
      if (!identities.add(value.identity())) {
        throw new IllegalArgumentException(
            "duplicate graph node: " + value.identity().canonicalId());
      }
      sortedNodes.add(value);
    }
    sortedNodes.sort(Comparator.comparing(node -> node.identity().canonicalId()));

    var sortedEdges = new ArrayList<ResourceEdge>(edges.size());
    var edgeKeys = new HashSet<String>();
    for (var edge : edges) {
      var value = Objects.requireNonNull(edge, "edge");
      if (!identities.contains(value.from()) || !identities.contains(value.to())) {
        throw new IllegalArgumentException("edge endpoint is not present in graph");
      }
      var key =
          value.from().canonicalId() + "->" + value.to().canonicalId() + ":" + value.type().name();
      if (!edgeKeys.add(key)) {
        throw new IllegalArgumentException("duplicate graph edge: " + key);
      }
      sortedEdges.add(value);
    }
    sortedEdges.sort(
        Comparator.comparing((ResourceEdge edge) -> edge.from().canonicalId())
            .thenComparing(edge -> edge.to().canonicalId())
            .thenComparing(edge -> edge.type().name()));

    nodes = List.copyOf(sortedNodes);
    edges = List.copyOf(sortedEdges);
  }

  /**
   * Creates an empty graph.
   *
   * @return empty graph
   */
  public static InfrastructureGraph empty() {
    return new InfrastructureGraph(List.of(), List.of());
  }

  public Optional<ResourceNode> node(ResourceIdentity identity) {
    Objects.requireNonNull(identity, "identity");
    return nodes.stream().filter(node -> node.identity().equals(identity)).findFirst();
  }

  public List<ResourceNode> inventoriedNodes() {
    return nodes.stream().filter(ResourceNode::inventoried).toList();
  }

  public List<ResourceNode> referencedNodes() {
    return nodes.stream().filter(node -> !node.inventoried()).toList();
  }

  public List<ResourceEdge> outgoing(ResourceIdentity identity) {
    Objects.requireNonNull(identity, "identity");
    return edges.stream().filter(edge -> edge.from().equals(identity)).toList();
  }

  public List<ResourceEdge> incoming(ResourceIdentity identity) {
    Objects.requireNonNull(identity, "identity");
    return edges.stream().filter(edge -> edge.to().equals(identity)).toList();
  }

  public List<ResourceEdge> directEdges(ResourceIdentity identity) {
    Objects.requireNonNull(identity, "identity");
    return edges.stream()
        .filter(edge -> edge.from().equals(identity) || edge.to().equals(identity))
        .toList();
  }

  public Set<ResourceIdentity> identities() {
    return nodes.stream()
        .map(ResourceNode::identity)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }
}
