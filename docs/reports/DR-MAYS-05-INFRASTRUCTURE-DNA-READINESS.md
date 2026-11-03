# DR-MAYS-05 — Infrastructure DNA Engine — Readiness Report

- Date: 2026-11-03
- Repository: `/home/dci-student/projects/dr-mays-aws-intelligence-doctor`
- Branch: `feature/dr-mays-infrastructure-dna-05`
- Base commit: `1ba7c5f9a9b52af3ddadf57d0bafeea196663091`
- Local status: **GREEN**
- Live AWS/AI: **NOT EXECUTED**
- CI status: **NOT VERIFIED** — no push permitted for DR-MAYS-05.

## 1. Graph data model

Core package `io.github.maynowak.drmays.core.dna` provides immutable, sorted nodes/edges, explicit resolution status, relationship evidence, completeness and validation issues. See `docs/development/INFRASTRUCTURE_DNA.md` and ADR-0009.

## 2. Relationship types

| Type | Proof |
| --- | --- |
| `SECURITY_GROUP_IN_VPC` | non-blank `vpc.id` on a network resource |
| `SECURITY_GROUP_REFERENCES_SECURITY_GROUP` | explicit `ingress.referenced.securityGroupIds` |

No edge is created from a count alone. S3 properties are node attributes, not edges.

## 3. Identity and provenance

- Reuses `ResourceIdentity`; account/region never merge.
- Edges require `RelationshipEvidence` wrapping existing `Evidence`.
- Evidence source must equal the edge source.
- Referenced nodes are `REFERENCED`, never `INVENTORIED`.

## 4. Analysis

`GraphAnalyzer` provides inventoried/referenced nodes, direct/incoming/outgoing edges, radius-bounded reachability (max 10), components, cycle detection, account/region filters and deterministic sorting. No external graph database.

## 5. Diagnostic context integration

Diagnosis findings now include `relationships` — verified outgoing graph edges for the finding resource. Existing rules/findings/evidence are unchanged. Public ingress remains a technical finding, not a claim of internet reachability.

## 6. API

`POST /api/v1/dna/graphs` accepts synthetic resources + optional `partialDiscovery`, returning versioned nodes, edges, relationship types, evidence, resolution status, issues, completeness and stats. Existing 100-resource limit, validation and ProblemDetail handling apply. OpenAPI is updated.

## 7. Fixtures

`examples/fixtures/dna-*.json` cover single resource, SG→VPC, SG→SG reference, multiple accounts, cycle, partial discovery and S3 properties. The offline/Docker demo verifies a 2-node/1-edge graph.

## 8. Tests and coverage

| Module | Tests | Line | Branch |
| --- | ---: | ---: | ---: |
| Core | 86 | 99.36 % | 95.65 % |
| API | 73 | 98.48 % | 85.19 % |
| AWS | 64 | 95.25 % | 83.93 % |
| **Total** | **223** | — | — |

All prior tests retained; 44 new tests (23 core graph + 6 DNA API/controller + 8 graph service + 6 fixture + 1 diagnostic context, plus adjusted mapper coverage).

## 9. Verification

- `./mvnw -B clean verify`: all 8 modules `SUCCESS`.
- Spotless PASS; Checkstyle 0 violations.
- Docker regression: image build, health, rules, diagnosis fixtures, DNA graph, invalid input, non-root/read-only/cap-drop, clean stop.
- `git diff --check`: clean; no secrets.

## 10. Git commits

| Step | Commit | Message |
| --- | --- | --- |
| 05A/B | `b61fe62` | `feat: add infrastructure DNA graph model` |
| 05C | `df953de` | `feat: expose referenced security group IDs` |
| 05F | `e62570b` | `feat: add offline DNA graph API` |
| 05G | `8f50fb8` | `feat: add DNA graph fixtures and demo check` |
| 05I | current HEAD | Diagnostic context, docs, readiness and audit close-out |

No push, merge, deployment, live AWS call or model call was performed.

## 11. Risks and open points

- Relationship support is intentionally narrow; routing/interface/service evidence is required before reachability claims.
- Referenced nodes have no attributes until inventoried.
- Graph completeness is only PARTIAL/COMPLETE in this phase; UNKNOWN is reserved.
- No CI run exists for this branch because no push was authorized.
- Docker demo remains local-only and unauthenticated.

## 12. Recommended next phase

DR-MAYS-06 can add richer verified relationships (e.g., network interfaces/routing) only with explicit discovery data and evidence. Do not infer reachability from public ingress alone.

## 13. Recovery point

- Branch: `feature/dr-mays-infrastructure-dna-05`
- Base: `1ba7c5f9a9b52af3ddadf57d0bafeea196663091`
- Last implementation commit: `8f50fb8`
- Final commit: current HEAD (`git log -1`)
- Resume with `git status` and `./mvnw -B clean verify`.
