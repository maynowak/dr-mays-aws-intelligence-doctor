# ORCH-02A-01/02 — REST Entry Points & Approval Security Review

## 1. Current status

`COMPLETE — READ-ONLY REVIEW.` No blocking bypass found in the current mock-only production path. Findings were fixed where demonstrated; residual items documented.

## 2. Audit date/time

- Review: 2026-11-04T11:24:17+01:00
- Agent session: `ses_e59ed8dd8ffeWo3wJJIiq4zsqb`

## 3. Git branch/HEAD observed

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD: `87e73ad` (matches expected)
- Base: `36ef4e5`

## 4. Scope

All REST entry points, privacy/approval implementation, DTOs, error handling and related tests/docs.

## 5. Completed steps

- Enumerated all controllers/routes.
- Reviewed `PrivacyGate`, `ApprovalToken`, `PrivacyPreview`, `PrivacyProcessor`, `ExpertiseService`, `ExpertiseController` and DTOs.
- Checked hash/provider/model/expertise/policy binding, TTL, replay, single-use, concurrency, tampering and payload substitution.
- Reviewed in-memory limits, unknown JSON and log/error behavior.

## 6. Actual findings

- PASS: no REST path bypasses the privacy engine in the mock-only configuration.
- PASS: legacy `/intelligence/explain` default mock; enabled path returns `APPROVAL_REQUIRED` without provider call.
- PASS: expertise explain accepts only preview ID + optional token; no payload/hash/provider substitution.
- PASS: approval binds exact payload hash + provider + model + expertise + policy; single-use, TTL, replay and cross-preview token checks are present.
- PASS: gate methods are synchronized; concurrent consumption should allow exactly one execution.
- PASS by inspection: unknown JSON rejected by `fail-on-unknown-properties`; errors do not echo internal values.
- MEDIUM residual at review time: arbitrary relationship evidence (e.g. `vpc.id=prod-vpc`, `igw-...`) could survive regex sanitization. **Fixed in 02A** by typed evidence-value aliasing.
- LOW residuals: model ID syntax-only allowlist; local approval is not authenticated human authorization; global caps can be exhausted locally; token lookup not constant-time; no concurrency/unknown-property tests initially. **Concurrency and unknown-property tests added; token timing/authenticated approval remain documented residuals.**

## 7. Evidence/file references

- `dr-mays-ai/.../privacy/PrivacyGate.java`, `PrivacyProcessor.java`, `PrivacyValidator.java`, `PrivacyPreview.java`, `ApprovalToken.java`
- `dr-mays-api/.../ExpertiseController.java`, `ExpertiseService.java`, `ExpertiseExplainRequest.java`
- `dr-mays-ai/.../IntelligenceConfiguration.java`, `PrivacyGatedLegacyAnalyzer.java`
- Tests: `PrivacyGateTest`, `ExpertiseControllerTest`, `ExpertiseServiceTest`, `IntelligenceServiceTest`

## 8. Classification

Initial `YELLOW`; no ORANGE/RED bypass. After evidence-value fix and added tests: **GREEN (local)** for current mock-only path.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

Read-only; only the orchestrator's 02A log was untracked at review time.

## 11. Files changed

None by reviewer.

## 12. Explicit no-change confirmation

No file/model/API/AWS/credential call by reviewer.

## 13. Open questions

- Model allowlist and authenticated approval are future remote-provider requirements.
- Router-level approval enforcement should be revisited before any non-mock allowlist.

## 14. Risks

- Future remote enablement without fixing F2 would leak attribute values.
- Local approval is not human authorization; loopback trust boundary.
- In-memory/single-instance limits.

## 15. Recommended next actions

1. Fix/verify relationship evidence projection (done).
2. Add concurrency and unknown-field tests (done).
3. Keep remote providers disabled.
4. Add model allowlist and authenticated approval before BYOK live use.

## 16. Current resume point

REST/approval review complete; orchestrator fixes verified by tests. No push/merge.
