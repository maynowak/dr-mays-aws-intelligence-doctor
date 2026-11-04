# ORCH-01 — Independent Security & Architecture Review

## 1. Current status

`COMPLETE — READ-ONLY REVIEW.` Initial classification `ORANGE`; two blocking issues were reported and subsequently addressed by the orchestrator. No external call or credential access occurred.

## 2. Audit date/time

- Review: 2026-11-04T08:44:27+01:00
- Agent session: `ses_e5a80e6c2ffekmjdM5zXBUfEZ8`

## 3. Git branch/HEAD observed

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD at review: `e740f15`
- Base: `36ef4e5`
- Working tree was dirty with final fixes/docs.

## 4. Audit scope

Full ORCH-01 diff plus working tree: privacy projection, approval gate, provider router, OpenCode adapter, expertise validation, API paths, module boundaries, tests, coverage, Docker and docs.

## 5. Completed work steps

- Reviewed privacy/provider/expertise packages and API changes.
- Checked approval replay/TTL/binding and raw-data leakage.
- Checked provider allowlisting/no fallback/credential references.
- Checked OpenCode process/shell/credential/egress surface.
- Checked expertise unknown/evidence/relationship/alias handling.
- Recomputed test totals and coverage from reports.
- Checked Docker log and docs for stale claims.

## 6. Actual findings

### Blockers reported

1. **Legacy `/api/v1/intelligence/explain` privacy bypass** when `dr.mays.ai.enabled=true`: legacy analyzer could call Ollama with raw canonical IDs/evidence without preview/approval.
2. **Short/placeholder resource IDs at committed HEAD** (`vpc-unknown`) were not sanitized/rejected; fix existed only in the working tree.

### Non-blocking findings

- Summary free text was not passed through unsafe-text checks; hostname/IPv6 were missing from free-text detection.
- Hostname redaction over-redacted dotted attribute names such as `ingress.public`.
- Secret/IPv6 patterns and docs were broader than implementation.
- Approval token/consumed-state growth was unbounded.
- `resourceType` was client-controlled, not coarse.
- OpenCode live limits remained declarative; adapter remains disabled.
- Audit/docs were stale relative to the working tree.

## 7. Evidence/file references

- `dr-mays-ai/src/main/java/io/github/maynowak/drmays/ai/privacy/`
- `dr-mays-ai/src/main/java/io/github/maynowak/drmays/ai/provider/`
- `dr-mays-ai/src/main/java/io/github/maynowak/drmays/ai/expertise/`
- `dr-mays-api/src/main/java/io/github/maynowak/drmays/api/v1/`
- `dr-mays-ai/src/main/java/io/github/maynowak/drmays/ai/PromptBuilder.java`
- `/tmp/opencode/orch01-full-verify.log`
- `/tmp/opencode/orch01-docker.log`

## 8. Classification

Initial `ORANGE`: default mock path GREEN; legacy opt-in raw path ORANGE; short-ID fail-open at HEAD. No RED incident observed.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

Read-only; working tree left unchanged by the review. Only orchestrator changes were present.

## 11. Files changed

None by the reviewer.

## 12. Explicit no-change confirmation

No file modified, no model/provider/AWS/credential call, no Maven/test/Docker command executed by the reviewer.

## 13. Open questions

- Scope of legacy Ollama path.
- Summary/hostname/IPv6 hardening.
- Approval-state bounds.
- `resourceType` coarse mapping.
- Final audit/re-Docker after fixes.

## 14. Risks

- Raw egress via legacy opt-in.
- Fail-open short IDs at HEAD.
- Pattern-based free-text/secret gaps.
- Approval memory growth and audit drift.
- OpenCode future enablement remains unsafe without OS isolation.

## 15. Recommended next actions

1. Fail-close legacy raw path or explicitly scope guarantees.
2. Commit short-ID fix.
3. Harden summary/hostname/IPv6 checks.
4. Preserve dotted attribute names while redacting hostnames.
5. Bound approval state.
6. Re-run full verify and Docker; record ORCH-01G.
7. Follow-up review.

## 16. Current resume point

Fixes applied after review; follow-up review and final readiness report remain.
