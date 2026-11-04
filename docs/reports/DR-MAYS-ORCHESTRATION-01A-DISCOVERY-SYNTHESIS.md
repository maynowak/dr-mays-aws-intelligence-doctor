# ORCH-01A — Discovery Synthesis

## 1. Current status

`COMPLETE` — three discovery reports synthesized; no implementation exists yet.

## 2. Audit date/time

- Date: 2026-11-04
- Discovery batch: 2026-11-04T08:02:34+01:00 to approximately 2026-11-04T08:07:44+01:00
- Per-agent wall time: **NOT MEASURED** separately; parallel batch window measured.

## 3. Git branch and HEAD

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD: `36ef4e5a105d8a11f10bf202c7cc60e978540c3f`
- Base `main`: same commit.

## 4. Audit scope

Synthesis of Java architecture, OpenCode CLI and privacy/security discovery for the privacy-first provider orchestration.

## 5. Completed work steps

- Three independent read-only agents completed the 16-point reports:
  - `DR-MAYS-ORCHESTRATION-01A-JAVA-ARCHITECTURE-EXECUTION_LOG.md`
  - `DR-MAYS-ORCHESTRATION-01A-OPENCODE-RESEARCH-EXECUTION_LOG.md`
  - `DR-MAYS-ORCHESTRATION-01A-SECURITY-RESEARCH-EXECUTION_LOG.md`
- Compared findings and resolved the module/architecture direction.
- Identified required contracts, tests and integration order.

## 6. Actual findings

- Reuse `IntelligenceRequest`, `IntelligenceResult`, `FindingExplanation`, `Recommendation`, `EvidenceReference`, `IntelligenceResultValidator`; do not create a second diagnosis engine.
- Add privacy/provider/approval/expertise packages inside `dr-mays-ai`; **no new Maven module** for this phase.
- Remote providers must accept a typed `SanitizedExpertiseRequest`, never raw domain objects.
- Preview, approval and invocation must bind the exact payload hash plus provider/model/expertise/policy.
- Approval is server-side, in-memory, TTL-bounded and single-use; missing/invalid approval fails closed.
- OpenCode live execution is **BLOCKED**; implement only a disabled/mock-verified command adapter and document the isolation gaps.
- Direct Spring AI/Ollama/provider adapters remain the regular inference path.
- Default mock path must remain offline and existing `/api/v1/intelligence/explain` must keep working.
- Required minimum: at least 50 meaningful new tests, total at least 313.

## 7. Evidence/file references

- Three discovery reports above.
- `docs/decisions/ADR-0010-ai-intelligence-boundary.md`
- `docs/security/AI_MODEL_SECURITY.md`
- `docs/development/AI_INTELLIGENCE.md`
- `dr-mays-ai/`, `dr-mays-api/`, `dr-mays-core/` inspected read-only.

## 8. Classification

`YELLOW` — architecture feasible; implementation required. OpenCode live path `BLOCKED`; no GREEN claim yet.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

Discovery reports and execution log pending commit; no production code changed.

## 11. Files changed

- Added: three discovery reports and this synthesis.
- Added: main ORCH-01 execution log.
- No production code changed.

## 12. Explicit no-change confirmation

No model/provider/AWS call, no credential read, no external data transfer, no push/merge/deployment, no OpenCode execution.

## 13. Open questions

- Exact approval response semantics and TTL.
- Whether a future remote provider needs server-side persistence.
- Which credential backend will be approved later.
- Whether OpenCode is ever required.

## 14. Risks

- Raw-domain leakage if payload construction reuses `PromptBuilder` directly.
- Approval bypass if the API exposes a direct analyzer or accepts findings.
- CLI process risk if OpenCode is enabled without OS isolation.
- Coverage/test drift if implementation outpaces tests.

## 15. Recommended next actions

1. Freeze ADR-0011 and contract document.
2. Implement privacy package and tests.
3. Implement provider contracts/router and mock/OpenCode-disabled adapter.
4. Implement expertise validation/orchestration.
5. Integrate additive API preview/approval/explain endpoints.
6. Run full gates, Docker regression, security review and audit.

## 16. Current resume point

ORCH-01B Architecture Contract Freeze. Base branch verified; discovery reports pending commit.
