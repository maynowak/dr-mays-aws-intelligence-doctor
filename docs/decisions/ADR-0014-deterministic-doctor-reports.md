# ADR-0014: Deterministic Doctor Reports V1

- Status: Accepted
- Date: 2026-11-05
- Scope: `dr-mays-core`, `dr-mays-api`
- Task: DR-MAYS-DOCTOR-REPORTS-01

## Context
The project has deterministic diagnosis, evidence and Infrastructure DNA, but no report contract. A report must not become a second rule engine or introduce AI/provider assumptions.

## Decision
1. Add a core `report` package with an immutable report model, assembler, deterministic hashes and a safe Markdown renderer.
2. Assemble from one existing `DiagnosisEngine` run and one `InfrastructureGraphBuilder` result over the same resource list.
3. Findings/evidence/recommendations are copied verbatim; no report code evaluates rules or creates edges.
4. Include all rule outcomes (`FINDING`, `NO_FINDING`, `NOT_APPLICABLE`) with rule versions from the catalog.
5. Require explicit `partialDiscovery`; render PARTIAL prominently; reject UNKNOWN completeness.
6. Expose response-only JSON and Markdown endpoints under `/api/v1/reports`; no persistence, AI, provider or AWS calls.
7. Bound output (1 MiB JSON, 256 KiB Markdown) and map overflow to RFC 9457 413.
8. Escape all dynamic Markdown and sort maps/collections deterministically; omit timestamps/random IDs.
9. Keep PDF and AI commentary as later, separate work.

## Consequences
- Reports are reproducible and traceable to the existing engine and graph.
- API DTOs remain separate from domain types.
- Live-data redaction, PDF and optional AI commentary require separate decisions.

## Rejected alternatives
- A second rule engine or report-derived findings.
- Serializing domain objects directly.
- AI-first report generation.
- Silent `COMPLETE` default when partial discovery is unknown.
- Server-side report files or a new database.
