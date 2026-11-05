# Doctor Reports V1 — Discovery Agent B (Infrastructure DNA)

## 1. Status
COMPLETE — READ-ONLY discovery.

## 2. Date/time
2026-11-05T09:31–09:33+01:00.

## 3. Branch/HEAD
`feature/dr-mays-doctor-reports-01` at `91c6307`.

## 4. Scope
InfrastructureGraphBuilder, InfrastructureGraph, GraphAnalyzer, GraphService, Graph DTOs, RelationshipEvidence, completeness and issues.

## 5. Steps
Read all DNA core/API types, builder rules, graph ordering, evidence invariants and fixtures.

## 6. Findings
- Only explicit `vpc.id` and `ingress.referenced.securityGroupIds` create edges.
- Referenced nodes are not inventoried and must be rendered separately.
- Graph issues and completeness must come from GraphBuildResult; DiagnosisResponse relationships are lossy.
- Cycles are reported as existence only; component count includes referenced nodes.
- Map ordering must be normalized for deterministic export.

## 7. Evidence
Core DNA package, GraphService, GraphResponse, DNA fixtures/tests, ADR-0009.

## 8. Classification
YELLOW — graph is deterministic and provenance-backed; report contract is missing.

## 9. Terraform
N/A.

## 10. Git status
Clean; no changes by agent.

## 11. Files changed
None.

## 12. No-change/no-call
No file changes, AWS/model/provider/credential/OpenCode calls.

## 13. Open questions
Completeness wording, identity exposure, cycle detail, issue rendering.

## 14. Risks
Mislabeling partial as complete, implying reachability, exposing referenced nodes as inventory.

## 15. Next actions
Build from GraphBuildResult; label partial and referenced-only nodes explicitly.

## 16. Resume point
Contract freeze.
