# DR-MAYS-DOCTOR-REPORTS-01 — Discovery & Architecture Contract Freeze

## 1. Status
COMPLETE — read-only discovery and contract freeze. Implementation followed on `feature/dr-mays-doctor-reports-01`.

## 2. Date/time
2026-11-05, 09:28–09:55 CET.

## 3. Branch/HEAD
`feature/dr-mays-doctor-reports-01`, based on verified main `91c6307b9ec48f51e6f5bec335b1a65d509591cc`.

## 4. Scope
Diagnosis/evidence aggregation, Infrastructure DNA rendering, report API/DTO/privacy and export safety.

## 5. Steps
Three read-only agents analyzed diagnosis, DNA and architecture; orchestrator froze the contract and implemented it.

## 6. Decisions / Contract Freeze
- **Input:** `POST /api/v1/reports` and `POST /api/v1/reports/markdown`.
- **Request:** `resources` (1–100) plus **required** `partialDiscovery` boolean. No provider, model, preview, token or AI fields.
- **Diagnosis authority:** one `DiagnosisEngine` run through `DiagnosisService.diagnoseSnapshot`; the report assembler never evaluates rules.
- **DNA authority:** one `InfrastructureGraphBuilder` build from the same resource list; only `GraphBuildResult` edges/issues/completeness are rendered.
- **Findings:** copied from `DiagnosisResult.findings()`; evidence and recommendation are copied verbatim.
- **Outcomes:** every rule/resource outcome with `FINDING`, `NO_FINDING` or `NOT_APPLICABLE`; versions joined from the registered rule catalog.
- **Ordering:** safe resource tuple (provider/account/region/type/id), then rule ID/reason; map keys sorted; no timestamps or random IDs.
- **Metadata:** schema `doctor-report-v1`, generator, mode, diagnosis source, input hash, rules fingerprint, partial flag, resource count.
- **Completeness:** `PARTIAL`/`partialDiscovery=true` renders the explicit banner; `UNKNOWN` is rejected rather than silently labeled complete.
- **Graph:** inventoried and referenced nodes separated; verified edges and their evidence; cycles by existence only; no inferred reachability.
- **Output limits:** JSON 1 MiB, Markdown 256 KiB; overflow returns RFC 9457 413.
- **Markdown:** fixed template, escape dynamic values, no raw HTML/links, no file persistence.
- **AI:** no provider or AI dependency in the report path; optional AI commentary is deferred.
- **Tests:** core assembler/renderer tests plus API service/controller/handler tests; Docker demo includes JSON, Markdown, partial, cycle, determinism and invalid input.

## 7. Evidence
Agent reports A/B/C; existing core/API contracts; `docs/api/openapi.yaml`; local verify and Docker logs.

## 8. Classification
GREEN for deterministic local V1.

## 9. Terraform
N/A.

## 10. Git status
Implementation committed separately from this audit documentation.

## 11. Files changed
Core/API report implementation and tests; OpenAPI; demo; fixtures; audit docs.

## 12. No-change/no-call
No AWS, provider, model, credential or OpenCode operation.

## 13. Open questions
Redaction policy for live snapshots; GFM bare-URL autolinking; PDF export later.

## 14. Risks
Raw local metadata is sensitive; partial flag must be explicit; report layer is not a substitute for live discovery verification.

## 15. Next actions
Keep V1 deterministic and local; defer AI commentary/PDF and live-data redaction to separate approvals.

## 16. Resume point
Implementation and reviews complete; final local verification.
