# Infrastructure DNA

The DNA engine models inventoried and referenced resources as a deterministic directed graph. It is local/offline and never calls AWS or a model provider.

## Model

| Type | Purpose |
| --- | --- |
| `InfrastructureGraph` | Immutable nodes/edges, sorted by canonical identity/type |
| `ResourceNode` | Identity + `INVENTORIED`/`REFERENCED` status + tags/attributes |
| `ResourceEdge` | Directed relationship with type and provenance |
| `RelationshipType` | `SECURITY_GROUP_IN_VPC`, `SECURITY_GROUP_REFERENCES_SECURITY_GROUP` |
| `RelationshipEvidence` | Wraps existing `Evidence` |
| `ResourceResolutionStatus` | Distinguishes real inventory from references |
| `GraphBuildResult` | Graph + issues + `GraphCompleteness` |
| `GraphValidationIssue` | Stable issue code + safe message |
| `InfrastructureGraphBuilder` | Evidence-only relationship extraction |
| `GraphAnalyzer` | Bounded deterministic queries |

## Identity and provenance

- `ResourceIdentity` is reused unchanged; account and region are part of identity, so equal IDs in different accounts/regions never merge.
- Every edge source must match its evidence resource.
- Edges only come from explicit attributes:
  - `vpc.id` on a network resource → referenced `vpc` node.
  - `ingress.referenced.securityGroupIds` on a network resource → referenced `network` nodes.
- `ingress.group.reference.count` alone never creates an edge.
- No internet-reachability or attack-path claims are made from public ingress.
- S3 versioning/public-access-block remain node attributes, never artificial edges.

## Completeness

- `COMPLETE`: no partial-discovery flag.
- `PARTIAL`: source discovery was incomplete; the graph keeps a `PARTIAL_DISCOVERY` issue.
- `UNKNOWN`: reserved.
- Referenced nodes are explicitly not inventoried. Missing metadata produces no edge.

## Analysis

`GraphAnalyzer` supports inventoried/referenced nodes, direct/incoming/outgoing edges, reachability with a maximum radius of 10, connected components, cycle detection (including self-loops), account/region filtering and stable sorting. It uses only Java collections.

## API

`POST /api/v1/dna/graphs` accepts the existing synthetic resource contract plus `partialDiscovery`, and returns versioned nodes/edges/issues/stats/completeness. Existing resource limits, validation and RFC 9457 errors apply. Diagnosis responses now include a `relationships` array per finding with verified outgoing graph edges.

OpenAPI: `docs/api/openapi.yaml`.

## Fixtures

`examples/fixtures/dna-*.json` cover single resources, VPC references, SG references, multiple accounts, cycles and partial discovery. `scripts/demo.sh` verifies a 2-node/1-VPC-edge graph through HTTP.

## Boundaries

- No graph database.
- No AWS live calls.
- No AI calls.
- No public deployment.
