package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds a deterministic graph only from explicit resource metadata. */
public final class InfrastructureGraphBuilder {

  private static final String VPC_ATTRIBUTE = "vpc.id";
  private static final String REFERENCED_SECURITY_GROUPS = "ingress.referenced.securityGroupIds";

  /**
   * Builds a graph with COMPLETE completeness.
   *
   * @param resources inventoried resources
   * @return graph build result
   */
  public GraphBuildResult build(List<InfrastructureResource> resources) {
    return build(resources, false);
  }

  /**
   * Builds a graph from inventoried resources.
   *
   * @param resources inventoried resources
   * @param partialDiscovery whether discovery was incomplete
   * @return graph build result
   */
  public GraphBuildResult build(List<InfrastructureResource> resources, boolean partialDiscovery) {
    Objects.requireNonNull(resources, "resources");
    var nodes = new LinkedHashMap<ResourceIdentity, ResourceNode>();
    var edges = new ArrayList<ResourceEdge>();
    var edgeKeys = new java.util.HashSet<String>();
    var issues = new ArrayList<GraphValidationIssue>();

    var orderedResources =
        resources.stream()
            .map(resource -> Objects.requireNonNull(resource, "resource"))
            .sorted(Comparator.comparing(resource -> resource.identity().canonicalId()))
            .toList();

    for (var resource : orderedResources) {
      if (nodes.containsKey(resource.identity())) {
        issues.add(
            new GraphValidationIssue(
                GraphIssueCode.DUPLICATE_RESOURCE,
                "duplicate resource: " + resource.identity().canonicalId()));
        continue;
      }
      nodes.put(
          resource.identity(),
          new ResourceNode(
              resource.identity(),
              ResourceResolutionStatus.INVENTORIED,
              resource.tags(),
              resource.attributes()));
    }

    for (var resource : orderedResources) {
      if (nodes.get(resource.identity()) == null) {
        continue;
      }
      addVpcEdge(resource, nodes, edges, edgeKeys, issues);
      addSecurityGroupReferenceEdges(resource, nodes, edges, edgeKeys, issues);
    }

    if (partialDiscovery) {
      issues.add(
          new GraphValidationIssue(
              GraphIssueCode.PARTIAL_DISCOVERY, "graph built from incomplete discovery data"));
    }
    issues.sort(
        Comparator.comparing((GraphValidationIssue issue) -> issue.code().name())
            .thenComparing(GraphValidationIssue::message));

    var graph = new InfrastructureGraph(List.copyOf(nodes.values()), edges);
    var completeness = partialDiscovery ? GraphCompleteness.PARTIAL : GraphCompleteness.COMPLETE;
    return new GraphBuildResult(graph, List.copyOf(issues), completeness);
  }

  private static void addVpcEdge(
      InfrastructureResource resource,
      Map<ResourceIdentity, ResourceNode> nodes,
      List<ResourceEdge> edges,
      Set<String> edgeKeys,
      List<GraphValidationIssue> issues) {
    var vpcId =
        resource.attribute(VPC_ATTRIBUTE).map(String::trim).filter(value -> !value.isBlank());
    if (vpcId.isEmpty()) {
      return;
    }
    var target = referencedIdentity(resource.identity(), "vpc", vpcId.get());
    nodes.putIfAbsent(
        target, new ResourceNode(target, ResourceResolutionStatus.REFERENCED, Map.of(), Map.of()));
    var evidence =
        new RelationshipEvidence(
            new Evidence(
                resource.identity(),
                "resource-attribute",
                VPC_ATTRIBUTE + "=" + vpcId.get(),
                VPC_ATTRIBUTE,
                Map.of("attribute", VPC_ATTRIBUTE, "value", vpcId.get())));
    addEdge(
        new ResourceEdge(
            resource.identity(), target, RelationshipType.SECURITY_GROUP_IN_VPC, evidence),
        edges,
        edgeKeys,
        issues);
  }

  private static void addSecurityGroupReferenceEdges(
      InfrastructureResource resource,
      Map<ResourceIdentity, ResourceNode> nodes,
      List<ResourceEdge> edges,
      Set<String> edgeKeys,
      List<GraphValidationIssue> issues) {
    var references = resource.attribute(REFERENCED_SECURITY_GROUPS);
    if (references.isEmpty()) {
      return;
    }
    var ids =
        java.util.Arrays.stream(references.get().split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .sorted()
            .toList();
    if (ids.isEmpty()) {
      issues.add(
          new GraphValidationIssue(
              GraphIssueCode.INVALID_REFERENCE,
              "referenced security group list contains no valid IDs"));
      return;
    }
    for (var id : ids) {
      var target = referencedIdentity(resource.identity(), "network", id);
      nodes.putIfAbsent(
          target,
          new ResourceNode(target, ResourceResolutionStatus.REFERENCED, Map.of(), Map.of()));
      var evidence =
          new RelationshipEvidence(
              new Evidence(
                  resource.identity(),
                  "resource-attribute",
                  REFERENCED_SECURITY_GROUPS + "=" + id,
                  "referenced security group ID",
                  Map.of("attribute", REFERENCED_SECURITY_GROUPS, "value", id)));
      addEdge(
          new ResourceEdge(
              resource.identity(),
              target,
              RelationshipType.SECURITY_GROUP_REFERENCES_SECURITY_GROUP,
              evidence),
          edges,
          edgeKeys,
          issues);
    }
  }

  private static ResourceIdentity referencedIdentity(
      ResourceIdentity source, String resourceType, String resourceId) {
    return new ResourceIdentity(
        source.provider(), source.accountId(), source.region(), resourceType, resourceId);
  }

  private static void addEdge(
      ResourceEdge edge,
      List<ResourceEdge> edges,
      Set<String> edgeKeys,
      List<GraphValidationIssue> issues) {
    var key = edge.from().canonicalId() + "->" + edge.to().canonicalId() + ":" + edge.type().name();
    if (!edgeKeys.add(key)) {
      issues.add(new GraphValidationIssue(GraphIssueCode.DUPLICATE_EDGE, "duplicate edge: " + key));
      return;
    }
    edges.add(edge);
  }
}
