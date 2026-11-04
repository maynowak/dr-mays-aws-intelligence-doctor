# ORCH-01A — Agent A — Java Architecture Discovery

## 1. Current status

`COMPLETE — READ-ONLY DISCOVERY.` No implementation and no build/test execution by this agent.

## 2. Audit date/time

- Date: 2026-11-04
- Observed: 2026-11-04T08:05:36+01:00
- Agent session: `ses_e5aa1afaaffeUPHrRJX9jFGC4w`

## 3. Git branch/HEAD observed

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD: `36ef4e5a105d8a11f10bf202c7cc60e978540c3f`
- Working tree: only the orchestrator's untracked execution log.

## 4. Audit scope

`dr-mays-core`, `dr-mays-api`, `dr-mays-ai`, existing intelligence contracts/adapters, REST DTOs, POMs, tests and relevant docs. No implementation, no AWS, no model call.

## 5. Completed work steps

- Read all AI production classes, intelligence API classes, core domain records and module POMs.
- Read existing AI/API tests and observed Surefire artifacts (not re-run).
- Read AI/security docs, ADR-0010/0002, testing/quality standards and OpenAPI intelligence schemas.
- Searched for existing privacy/provider/BYOK/approval/OpenCode concepts.
- Derived extension points, module decision and implementation order.

## 6. Actual findings

- `IntelligenceRequest` already minimizes input to findings, verified edges and completeness; it excludes tags/attributes. It is the correct reuse seam but not a typed privacy boundary.
- `PromptBuilder` serializes `ResourceIdentity.canonicalId()` and evidence observed/expected directly. Raw account/resource/VPC/SG IDs can reach a provider.
- `IntelligenceResultValidator` rejects unknown findings/evidence/relationships and duplicates, but does not validate recommendations or free text.
- No provider registry/router, no BYOK credential reference, no preview/approval store, no approval-bound status.
- `IntelligenceService` constructs diagnosis/DNA/request inline and calls the analyzer directly; no shared request factory for preview/explain.
- `dr-mays-ai` is the right home for privacy/provider/approval/expertise packages. A new Maven module would add Dockerfile/POM/JaCoCo/CI work without isolation benefit for mock-only scope.
- Existing default is deterministic mock and offline-safe; existing endpoint must remain functional.
- Observed baseline artifacts show 263 tests (86 core, 84 API, 64 AWS, 29 AI), but this agent did not re-execute the build.

## 7. Evidence/file references

- `dr-mays-ai/src/main/java/io/github/maynowak/drmays/ai/` — contracts, mock, Ollama adapter, prompt builder, validator.
- `dr-mays-api/src/main/java/io/github/maynowak/drmays/api/v1/IntelligenceService.java` — inline request construction and direct analyzer call.
- `dr-mays-core/src/main/java/io/github/maynowak/drmays/core/resource/ResourceIdentity.java` — canonical ID includes account/resource ID.
- `dr-mays-core/src/main/java/io/github/maynowak/drmays/core/evidence/Evidence.java` — observed/expected/details.
- `dr-mays-ai/src/main/resources/prompts/intelligence-v1.txt` — current prompt.
- `docs/decisions/ADR-0010-ai-intelligence-boundary.md`, `docs/development/AI_INTELLIGENCE.md`, `docs/security/AI_MODEL_SECURITY.md`.
- `docs/api/openapi.yaml` — existing explain endpoint and schemas.

## 8. Classification

`YELLOW` — feasible architecture, but privacy/approval/provider pieces must be built. Raw-data leakage and approval bypass are ORANGE risks.

## 9. Terraform checks and results

N/A — no Terraform/CDK/AWS command executed.

## 10. Git status

Read-only; no tracked modifications. Only `docs/reports/DR-MAYS-ORCHESTRATION-01-EXECUTION_LOG.md` untracked.

## 11. Files changed

None. Report returned to orchestrator; no audit file written by the agent.

## 12. Explicit no-change confirmation

No production/test/doc/POM file changed, no model/provider call, no REST/AWS/network call, no credential read, no Terraform/CDK/Docker/Maven execution.

## 13. Open questions

- Status values for approval/privacy outcomes.
- Whether to add a second hard remote-provider switch.
- Approval storage/TTL/HTTP semantics.
- Preview content and pseudonymization scope.
- BYOK key source.
- OpenCode CLI scope.

## 14. Risks

- Raw identity/evidence leakage if `PromptBuilder` is reused without an explicit allowlist.
- Approval bypass if concrete analyzers remain injectable or a findings-accepting endpoint is added.
- Free-text/recommendation validation gaps.
- Duplicate diagnosis logic in preview/explain.
- Relationship equality includes evidence details; resolving aliases must restore original edges.

## 15. Recommended next actions

1. Freeze ADR/contract: payload allowlist, pseudonymization, approval binding, statuses, provider IDs.
2. Implement privacy projection and sentinel tests first.
3. Add provider registry/router with mock as the only enabled provider.
4. Add approval gate with single-use TTL binding.
5. Add expertise validation and mock-only orchestration.
6. Extend API additively and preserve existing `/explain`.
7. Defer live OpenCode CLI; mock-only adapter.
8. Run full gates and Docker regression.

## 16. Current resume point

Discovery complete at base `36ef4e5`. Proceed to ORCH-01B contract freeze; no code changed.
