# Doctor Reports V1 — Discovery Agent A (Diagnosis & Evidence)

## 1. Status
COMPLETE — READ-ONLY discovery.

## 2. Date/time
2026-11-05T09:34+01:00.

## 3. Branch/HEAD
`feature/dr-mays-doctor-reports-01` at `91c6307`.

## 4. Scope
DiagnosisService, DiagnosisEngine, DiagnosisResult, DiagnosticFinding, DiagnosticOutcome, Evidence, DTOs and tests.

## 5. Steps
Traced the single diagnosis path; inspected ordering, severity, evidence invariants, outcome types and API mapping.

## 6. Findings
- The existing engine is the only rule evaluator and already produces deterministic outcomes.
- Non-finding outcomes carry no rule version; the report must join the registered rule catalog.
- DiagnosisResponse does not echo scope; the report must use the request resource list.
- Evidence is mandatory for findings; non-findings have only a reason.
- No report contract exists yet.

## 7. Evidence
`DiagnosisEngine`, `DiagnosisResult`, `DiagnosticOutcome`, `DiagnosticFinding`, `DiagnosisService`, built-in rules, tests.

## 8. Classification
YELLOW — feasible without a second engine; contract decisions required.

## 9. Terraform
N/A.

## 10. Git status
Clean at start; no changes by agent.

## 11. Files changed
None.

## 12. No-change/no-call
No file changes, provider/model/AWS/credential/OpenCode calls.

## 13. Open questions
Endpoint shape, outcome version join, ordering, scope metadata.

## 14. Risks
Report mapping drift, invented findings, missing evidence for clean checks.

## 15. Next actions
Reuse one engine run and the existing rule catalog; do not rerun or duplicate rules.

## 16. Resume point
Contract freeze.
