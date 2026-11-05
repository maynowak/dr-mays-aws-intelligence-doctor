# Doctor Reports V1 — Discovery Agent C (Report Architecture & Privacy)

## 1. Status
COMPLETE — READ-ONLY discovery.

## 2. Date/time
2026-11-05T08:32 UTC.

## 3. Branch/HEAD
`feature/dr-mays-doctor-reports-01` at `91c6307`.

## 4. Scope
API DTO/controller patterns, RFC 9457, Jackson 3, OpenAPI, Docker, IntelligenceInputFactory, ExpertiseReport, privacy-v2 and provider authorization.

## 5. Steps
Read API patterns, limits, error handling, runtime constraints and privacy/provider boundaries; designed a minimal deterministic report contract.

## 6. Findings
- Report should be response-only and AI-free by default.
- JSON and Markdown endpoints should be separate paths.
- Dynamic Markdown must escape HTML/Markdown and avoid links.
- Request must not accept provider/model/token fields.
- Output must be bounded; add an explicit 413.
- Report contains local resource metadata and must not claim anonymity.
- PDF should remain deferred.

## 7. Evidence
Controllers, DTOs, ApiExceptionHandler, OpenAPI, compose/Dockerfile, demo scripts, privacy/provider contracts.

## 8. Classification
GREEN for discovery; YELLOW for implementation risks.

## 9. Terraform
N/A.

## 10. Git status
Clean; no changes by agent.

## 11. Files changed
None.

## 12. No-change/no-call
No file changes, provider/model/AWS/credential/OpenCode calls.

## 13. Open questions
Redaction before live discovery, timestamp policy, GFM autolinking.

## 14. Risks
Markdown injection, sensitive export, provider bypass, output amplification.

## 15. Next actions
Freeze `report-v1`, implement fixed renderer and bounded response.

## 16. Resume point
Contract freeze.
