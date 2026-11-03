package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Deterministic, bounded graph queries without an external graph database. */
public final class GraphAnalyzer {

  public static final int MAX_RADIUS = 10;

  private final InfrastructureGraph graph;

  public GraphAnalyzer(InfrastructureGraph graph) {
    this.graph = Objects.requireNonNull(graph, "graph");
  }

  public List<ResourceNode> inventoriedNodes() {
    return graph.inventoriedNodes();
  }

  public List<ResourceNode> referencedNodes() {
    return graph.referencedNodes();
  }

  public List<ResourceEdge> outgoing(ResourceIdentity identity) {
    return graph.outgoing(identity);
  }

  public List<ResourceEdge> incoming(ResourceIdentity identity) {
    return graph.incoming(identity);
  }

  public List<ResourceEdge> directEdges(ResourceIdentity identity) {
    return graph.directEdges(identity);
  }

  /**
   * Returns nodes reachable via outgoing edges within a bounded radius (start included).
   *
   * @param start start identity
   * @param radius maximum traversal depth, 0 through {@value #MAX_RADIUS}
   * @return sorted reachable nodes
   */
  public List<ResourceNode> reachable(ResourceIdentity start, int radius) {
    Objects.requireNonNull(start, "start");
    if (radius < 0 || radius > MAX_RADIUS) {
      throw new IllegalArgumentException("radius must be between 0 and " + MAX_RADIUS);
    }
    if (graph.node(start).isEmpty()) {
      throw new IllegalArgumentException("unknown graph node: " + start.canonicalId());
    }

    var visited = new HashSet<ResourceIdentity>();
    var queue = new ArrayDeque<NodeDepth>();
    queue.add(new NodeDepth(start, 0));
    visited.add(start);

    while (!queue.isEmpty()) {
      var current = queue.removeFirst();
      if (current.depth >= radius) {
        continue;
      }
      for (var edge : graph.outgoing(current.identity)) {
        if (visited.add(edge.to())) {
          queue.addLast(new NodeDepth(edge.to(), current.depth + 1));
        }
      }
    }

    return visited.stream()
        .map(identity -> graph.node(identity).orElseThrow())
        .sorted(Comparator.comparing(node -> node.identity().canonicalId()))
        .toList();
  }

  /**
   * Returns undirected connected components, with each component and the component list sorted.
   *
   * @return connected components as identity lists
   */
  public List<List<ResourceIdentity>> components() {
    var adjacency = new HashMap<ResourceIdentity, Set<ResourceIdentity>>();
    for (var node : graph.nodes()) {
      adjacency.put(node.identity(), new LinkedHashSet<>());
    }
    for (var edge : graph.edges()) {
      adjacency.get(edge.from()).add(edge.to());
      adjacency.get(edge.to()).add(edge.from());
    }

    var remaining = new HashSet<>(adjacency.keySet());
    var components = new ArrayList<List<ResourceIdentity>>();
    while (!remaining.isEmpty()) {
      var start =
          remaining.stream().min(Comparator.comparing(ResourceIdentity::canonicalId)).orElseThrow();
      var component = new ArrayList<ResourceIdentity>();
      var queue = new ArrayDeque<ResourceIdentity>();
      queue.add(start);
      remaining.remove(start);
      while (!queue.isEmpty()) {
        var current = queue.removeFirst();
        component.add(current);
        for (var next : adjacency.get(current)) {
          if (remaining.remove(next)) {
            queue.addLast(next);
          }
        }
      }
      component.sort(Comparator.comparing(ResourceIdentity::canonicalId));
      components.add(List.copyOf(component));
    }
    components.sort(
        Comparator.comparing(
            (List<ResourceIdentity> component) -> component.getFirst().canonicalId()));
    return List.copyOf(components);
  }

  /**
   * Detects a directed cycle, including self-loops.
   *
   * @return true when a cycle exists
   */
  public boolean hasCycles() {
    var state = new HashMap<ResourceIdentity, Integer>();
    for (var node : graph.nodes()) {
      if (visitForCycle(node.identity(), state)) {
        return true;
      }
    }
    return false;
  }

  public List<ResourceNode> nodesForAccount(String accountId) {
    var value = DomainChecks.requireText(accountId, "accountId");
    return graph.nodes().stream()
        .filter(node -> node.identity().accountId().equals(value))
        .toList();
  }

  public List<ResourceNode> nodesForRegion(String region) {
    var value = DomainChecks.requireText(region, "region");
    return graph.nodes().stream().filter(node -> node.identity().region().equals(value)).toList();
  }

  private boolean visitForCycle(ResourceIdentity identity, Map<ResourceIdentity, Integer> state) {
    var current = state.getOrDefault(identity, 0);
    if (current == 1) {
      return true;
    }
    if (current == 2) {
      return false;
    }
    state.put(identity, 1);
    for (var edge : graph.outgoing(identity)) {
      if (visitForCycle(edge.to(), state)) {
        return true;
      }
    }
    state.put(identity, 2);
    return false;
  }

  private record NodeDepth(ResourceIdentity identity, int depth) {}
}
