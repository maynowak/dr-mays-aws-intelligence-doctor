# ADR-0006: Local Spring Boot REST Boundary

- Status: Accepted
- Date: 2026-11-03
- Scope: `dr-mays-api`

## Context

The core diagnosis engine is deterministic and AWS/AI-independent. DR-MAYS-02 needs a local HTTP interface without exposing domain internals directly or introducing cloud behavior.

## Decision

- Use Spring Boot 4.1.2 with the modular `spring-boot-starter-webmvc`, validation and actuator starters.
- Bind to `127.0.0.1` by default and expose only `/actuator/health`; no authentication is provided in this local-only phase.
- Keep the wire contract in `io.github.maynowak.drmays.api.v1` as dedicated request/response records. Domain types are never serialized directly.
- Map DTOs to `DiagnosticContext`/`InfrastructureResource` and map `DiagnosisResult` back to wire types in `DiagnosisService`. The existing `DiagnosisEngine` remains authoritative.
- Reject unknown JSON properties, malformed JSON, duplicate identities and invalid/oversized values.
- Return RFC 9457 `application/problem+json` with safe messages. The project advice is ordered above Boot's ProblemDetails handler so explicit responses win.
- Keep the API stateless: no database, no AWS SDK, no model calls, no timestamps in diagnosis results.

## Consequences

- The wire contract can evolve independently of domain records.
- Core stays reusable by CLI, worker or future adapters.
- Public exposure would require authentication, TLS, rate limiting and a security review; none is implemented here.
- Spring Boot 4 uses Jackson 3 (`tools.jackson.databind`); code must not assume Jackson 2 packages.
