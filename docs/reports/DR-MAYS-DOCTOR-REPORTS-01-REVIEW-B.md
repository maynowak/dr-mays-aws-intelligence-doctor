# Doctor Reports V1 — Review B (Privacy & Security)

## 1. Status
COMPLETE — READ-ONLY. No HIGH/CRITICAL blocker; current synthetic loopback path GREEN.

## 2. Date/time
2026-11-05T09:51:10+01:00.

## 3. Branch/HEAD
`feature/dr-mays-doctor-reports-01` at `91c6307` + uncommitted implementation.

## 4. Scope
Sensitive data, logging, JSON/Markdown injection, payload/output bounds, provider/authorization bypass, AI/AWS calls, persistence and error handling.

## 5. Steps
Read all report core/API sources, tests, diffs, runtime configuration and logs; grepped imports, logging, file/network/process APIs, provider/AWS/credential references; inspected verify/docker evidence.

## 6. Findings
- **MEDIUM-1:** exported reports contain full local tags/attributes/evidence; no live-data redaction policy. Documented as synthetic/local-only; no AI/provider call. Live-data redaction remains a later gate.
- **MEDIUM-2:** omitted `partialDiscovery` defaulted to complete. Fixed: field is now required and missing values are rejected.
- **LOW:** output limits are post-materialization; add streaming if exposure widens.
- **LOW:** escaper branches/test gaps. Added `MarkdownEscaperTest`, bidi/line-separator handling and graph/table escaping coverage.
- **LOW:** GFM bare-URL autolinking remains possible; no `[text](url)` or raw HTML injection.
- **LOW:** docs/governance drift; updated in this audit.

## 7. Evidence
Report source/tests, `application.yml`, `compose.yaml`, demo script, verify/docker logs.

## 8. Classification
GREEN for synthetic loopback V1; YELLOW latent for live data and wider exposure.

## 9. Terraform
N/A.

## 10. Git status
Reviewer changed nothing; orchestrator applied fixes.

## 11. Files changed
None by reviewer.

## 12. No-change/no-call
No provider/model/AWS/credential/OpenCode call.

## 13. Open questions
Live-data redaction, download headers, GFM autolinking, pre-flight size estimation.

## 14. Risks
Sensitive export after live integration; memory amplification before 413 if exposed beyond loopback.

## 15. Next actions
Define live-data redaction before phase 04; keep loopback-only and no persistence.

## 16. Resume point
Review complete; current path passes local security checks.
