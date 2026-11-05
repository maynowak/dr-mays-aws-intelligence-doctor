# Doctor Reports V1 — Review A (Report Correctness)

## 1. Status
COMPLETE — READ-ONLY. No HIGH/CRITICAL blocker on the deterministic API path.

## 2. Date/time
2026-11-05T09:52:55+01:00.

## 3. Branch/HEAD
`feature/dr-mays-doctor-reports-01` at `91c6307` + uncommitted implementation at review time.

## 4. Scope
Evidence provenance, findings integrity, graph integrity, severity, rule outcomes, partial discovery, determinism, API contracts.

## 5. Steps
Traced report inputs back through `DoctorReportService` → `DiagnosisService.diagnoseSnapshot` → `DiagnosisEngine`; compared assembled report fields against domain invariants; inspected tests, OpenAPI, verify/docker logs and JaCoCo.

## 6. Findings
- **MEDIUM-1:** assembler trusted caller-supplied inputs without scope coverage checks. Fixed by `assertScope`: findings/outcomes must be inside resources, inventoried graph nodes must be inside resources, and outcomes must cover resources × rules.
- **MEDIUM-2:** `UNKNOWN` completeness could be reported without a partial warning. Fixed by failing closed on `UNKNOWN`.
- **MEDIUM-3:** ad-hoc input hash serialization could be ambiguous. Fixed with length-prefixed canonical fields.
- **MEDIUM-4:** `ResourceIdentity.canonicalId()` uses an unescaped delimiter; report ordering now uses a safe multi-field comparator. Pre-existing graph dedup remains a documented limitation.
- **MEDIUM-5:** unknown-field test did not isolate top-level rejection. Fixed with valid resources and explicit `partialDiscovery`.
- **LOW:** Markdown section 4 skipped without findings; Unicode bidi/line separators; missing docs/test gaps. Section/escaping fixes and docs added.

## 7. Evidence
Report core/API sources, tests, `/tmp/opencode/dr-reports-final-verify.log`, `/tmp/opencode/dr-reports-final-docker.log`, OpenAPI.

## 8. Classification
GREEN after fixes; latent pre-existing canonical-ID limitation documented.

## 9. Terraform
N/A.

## 10. Git status
Reviewer changed nothing; orchestrator applied fixes.

## 11. Files changed
None by reviewer.

## 12. No-change/no-call
No provider/model/AWS/credential/OpenCode call.

## 13. Open questions
Redaction before live snapshots; GFM autolinking; PDF later.

## 14. Risks
Live metadata sensitivity, partial label correctness, canonical-ID collision in pre-existing graph builder.

## 15. Next actions
Keep report layer deterministic; revisit live-data policy and canonical-ID handling in a later phase.

## 16. Resume point
Review complete; fixes verified by final local suite.
