# ADR-0009: Infrastructure DNA Graph

- Status: Accepted
- Date: 2026-11-03
- Scope: `dr-mays-core`, `dr-mays-api`, `dr-mays-aws` mapper extension

## Context

Dr. Mays needs to explain verified relationships between resources without inventing topology or attack paths. The existing domain has stable `ResourceIdentity`, immutable `InfrastructureResource` and `Evidence`. AWS discovery produces resources with attributes such as `vpc.id`; EC2 currently records only a count of referenced security groups.

## Decision

### Model

- Keep the graph in `dr-mays-core`; no AWS SDK, graph database or external service.
- `ResourceNode` uses the existing `ResourceIdentity`, a `ResourceResolutionStatus` (`INVENTORIED` or `REFERENCED`), plus immutable tags/attributes.
- `ResourceEdge` is directed and carries a `RelationshipType` and `RelationshipEvidence`.
- `RelationshipEvidence` wraps the existing `Evidence` type so provenance is explicit without duplicating fields.
- `InfrastructureGraph` stores immutable, identity-keyed nodes and edges. Every edge endpoint must exist as a node.
- `GraphBuildResult` carries the graph, validation issues and `GraphCompleteness` (`COMPLETE`, `PARTIAL`, `UNKNOWN`).
- `GraphValidationIssue` records stable issue codes/messages; no raw SDK payloads.

### Relationship rules

Only relationships directly supported by explicit metadata are emitted:

1. `SECURITY_GROUP_IN_VPC`: network resource with a non-blank `vpc.id` → a referenced `vpc` node in the same account/region.
2. `SECURITY_GROUP_REFERENCES_SECURITY_GROUP`: network resource with explicit referenced security-group IDs in `ingress.referenced.securityGroupIds` → each referenced `network` node in the same account/region.

A count of referenced groups is **not** sufficient to create an edge. The EC2 mapper is extended backwards-compatibly to add the concrete sorted ID list while retaining the count. S3 versioning/public-access-block remain node properties, never edges. No inferred internet reachability or attack paths are produced.

### Identity and completeness

- Account and region remain part of `ResourceIdentity`; nodes from different accounts/regions never merge.
- Referenced nodes are not reported as inventoried.
- PARTIAL discovery remains visible via graph completeness and issues.
- Duplicate identities, duplicate edges and missing references become validation issues deterministically.

### Analysis

`GraphAnalyzer` provides inventoried/referenced nodes, direct/incoming/outgoing edges, bounded reachability (max radius 10), connected components, cycle detection, account/region filters and stable sorting. No unbounded traversal.

### API

Versioned graph DTOs under `api.v1`; domain graph types are never serialized directly. `POST /api/v1/dna/graphs` accepts the existing synthetic resource contract plus an optional `partialDiscovery` flag and returns nodes, edges, relationship types, evidence, resolution status, issues and statistics. Existing validation/ProblemDetail behavior applies.

## Consequences

- The graph is explainable: every edge has a source identity, observed/expected values and details.
- Unknown or denied discovery data cannot silently become topology.
- The API remains local/offline; it does not call AWS discovery.
- SG→SG edges require the mapper extension; old resources without the new attribute simply produce no SG→SG edges.
- Cycle handling and bounded traversal must be tested explicitly.
