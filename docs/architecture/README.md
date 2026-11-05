# Architecture Overview

Dr. Mays is a read-only AWS infrastructure diagnostics system. Technical evidence is produced by deterministic rules; AI may explain findings but never replaces evidence.

## Modules

| Module | Responsibility | Allowed dependencies |
| --- | --- | --- |
| `dr-mays-common` | Shared primitives with no AWS or AI types | None |
| `dr-mays-core` | Domain model, findings, evidence, relations, rule engine | `common` |
| `dr-mays-aws` | Read-only AWS discovery foundation (implemented, mocked only) | `core` |
| `dr-mays-api` | Spring Boot 4 REST API (implemented, local only) | `core` |
| `dr-mays-ai` | Optional model adapters and explanations | `core` |
| `dr-mays-worker` | Future scheduled or Lambda execution | `core` |
| `dr-mays-infrastructure` | Future AWS CDK v2 definitions | None initially |

The dependency direction is always inward: AWS, API, AI and worker may depend on the domain, but the domain depends on neither AWS nor AI. `infrastructure` remains isolated until the deployment phase.

## REST boundary (DR-MAYS-02)

```text
POST /api/v1/diagnoses  -> wire DTOs -> DiagnosisService -> DiagnosticContext
                        -> DiagnosisEngine -> DiagnosisResult -> wire DTOs -> JSON
GET  /api/v1/rules      -> registered core rules -> wire DTOs
GET  /actuator/health   -> Spring Boot Actuator health
```

The API binds to `127.0.0.1`, is unauthenticated and uses no database, AWS SDK or model provider. Wire DTOs are versioned under `api.v1`; domain types are never serialized directly. Failures use RFC 9457 `application/problem+json`.

## Container runtime (DR-MAYS-03)

```text
docker compose -> image dr-mays-api:local
  -> UID 10001, read-only root, /tmp tmpfs, cap-drop ALL, no-new-privileges
  -> host publish 127.0.0.1:18080 -> container 0.0.0.0:8080
  -> external demo runner checks health/rules/fixtures (no extra runtime tools)
```

The default jar and local app remain loopback-only. The `docker` profile is the only place the server binds all interfaces, and only inside the container network namespace.

## AWS discovery boundary (DR-MAYS-04)

```text
DiscoveryScope -> STS identity verification (fail closed)
               -> EC2 security groups  -> AwsResourceMapper -> core resources
               -> S3 bucket metadata   -> AwsResourceMapper -> core resources
               -> DiscoveryResult (COMPLETE / PARTIAL / FAILED)
```

All AWS SDK types stay in `dr-mays-aws`. The adapter is not wired into Spring, the API or Docker, and no live AWS call has executed. See `docs/development/AWS_DISCOVERY.md`, ADR-0008 and `docs/security/AWS_SCANNER_IAM.md`.

## Infrastructure DNA (DR-MAYS-05)

```text
InfrastructureResource list
  -> InfrastructureGraphBuilder (only explicit vpc.id / referenced SG IDs)
  -> InfrastructureGraph (INVENTORIED vs REFERENCED nodes, evidence-backed edges)
  -> GraphAnalyzer (bounded traversal, components, cycles, account/region filters)
```

The graph lives in `dr-mays-core` and uses no graph database. `POST /api/v1/dna/graphs` exposes versioned DTOs; diagnosis findings carry verified outgoing relationships, not inferred reachability. See `docs/development/INFRASTRUCTURE_DNA.md` and ADR-0009.

## AI intelligence (DR-MAYS-06)

```text
POST /api/v1/intelligence/explain
  -> deterministic diagnosis + DNA edges
  -> IntelligenceRequest -> IntelligenceAnalyzer
       default: deterministic mock
       legacy enabled path: APPROVAL_REQUIRED (no raw external call)
  -> IntelligenceResultValidator -> versioned response
```

The model only explains findings/evidence/relationships. Unknown resources, evidence or edges are rejected; technical diagnosis remains authoritative. See `docs/development/AI_INTELLIGENCE.md`, `docs/security/AI_MODEL_SECURITY.md` and ADR-0010.

## Privacy-first provider orchestration (ORCH-01)

```text
verified IntelligenceRequest
  -> PrivacyProcessor (allowlist, HMAC aliases, validation)
  -> PrivacyGate preview + server-side approval (exact payload hash)
  -> ProviderRouter (mock enabled; remote disabled)
  -> ExpertiseParser -> alias resolution
  -> IntelligenceResultValidator + ExpertiseValidator
  -> local ExpertiseReport
```

`dr-mays-ai` now contains `privacy`, `provider` and `expertise` packages. `dr-mays-core` remains free of provider, Spring AI, OpenCode and credential types. Preview never invokes a provider; execution accepts only a stored preview ID and optional token. OpenCode live execution is BLOCKED. See `docs/architecture/PRIVACY_FIRST_PROVIDER_CONTRACT.md` and ADR-0011.

## Doctor Reports (DR-MAYS-DOCTOR-REPORTS-01)

```text
validated resources
  -> existing DiagnosisEngine (single run)
  -> existing InfrastructureGraphBuilder (same snapshot)
  -> DoctorReportAssembler (aggregation only)
  -> deterministic JSON (report-v1) or escaped Markdown
```

`POST /api/v1/reports` and `POST /api/v1/reports/markdown` are local-only, response-only and AI-free. The report never creates findings or graph edges and requires explicit `partialDiscovery`. See `docs/development/DOCTOR_REPORTS.md` and ADR-0014.

## Target flow

```text
AWS adapters or offline fixtures
        -> validated resource snapshots + verified links
        -> deterministic rule engine (authoritative findings)
        -> JSON diagnosis report
        -> optional AI explanation
```

## Principles

1. The domain engine works without AWS.
2. The domain engine works without AI models.
3. AWS access happens only through explicit adapters.
4. Findings are rule-based and carry stable evidence.
5. AI interprets findings; it does not create technical evidence.
6. AWS permissions follow least privilege.
7. Diagnosis never mutates inspected resources.
8. Modules remain independently testable.

## Quality and testability

- The domain is exercised without AWS or model access.
- Spotless, Checkstyle and JaCoCo run during `./mvnw -B clean verify`; see `docs/development/QUALITY_GATES.md`.
- `dr-mays-core` enforces 85 % line and 75 % branch coverage; rules and engine remain deterministic.
- `dr-mays-api` enforces the same thresholds over its testable production code and exercises real HTTP, MVC slices and ProblemDetail error paths.
- `dr-mays-aws` enforces the same thresholds over discovery contracts, mappers, identity and SDK-failure handling; all AWS clients are mocked.
- Public domain contracts carry Javadocs, including record component parameters.
- Coding, testing and Definition-of-Done rules live in `docs/development/CODING_STANDARDS.md`, `docs/development/TESTING_STANDARDS.md` and `docs/development/QUALITY_GATES.md`.
