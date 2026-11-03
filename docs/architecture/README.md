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
