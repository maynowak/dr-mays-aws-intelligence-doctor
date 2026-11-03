package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.core.dna.RelationshipEvidence;
import io.github.maynowak.drmays.core.dna.ResourceEdge;
import io.github.maynowak.drmays.core.dna.ResourceNode;
import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;

/** Shared mapping from graph domain values to versioned wire DTOs. */
final class GraphDtoMapper {

  private GraphDtoMapper() {}

  static GraphNodeResponse node(ResourceNode node) {
    return new GraphNodeResponse(
        resource(node.identity()), node.status().name(), node.tags(), node.attributes());
  }

  static GraphEdgeResponse edge(ResourceEdge edge) {
    return new GraphEdgeResponse(
        resource(edge.from()), resource(edge.to()), edge.type().name(), evidence(edge.evidence()));
  }

  static ResourceResponse resource(ResourceIdentity identity) {
    return new ResourceResponse(
        identity.provider(),
        identity.accountId(),
        identity.region(),
        identity.resourceType(),
        identity.resourceId());
  }

  private static EvidenceResponse evidence(RelationshipEvidence relationshipEvidence) {
    Evidence evidence = relationshipEvidence.evidence();
    return new EvidenceResponse(
        evidence.source(), evidence.observed(), evidence.expected(), evidence.details());
  }
}
