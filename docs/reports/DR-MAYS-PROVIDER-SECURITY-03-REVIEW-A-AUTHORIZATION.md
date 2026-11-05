# PROVIDER-SECURITY-03 — Review A: Authorization & Dataflow

## 1. Status

`COMPLETE — READ-ONLY REVIEW.` No HIGH/CRITICAL; MEDIUM latent payload-hash issue fixed after review; MEDIUM gate-flag residual documented.

## 2. Date/time

2026-11-04T15:27:27+01:00; session `ses_e59108265ffe60iIZ360YqD09w`.

## 3. Branch/HEAD

`feature/dr-mays-provider-security-03` at reviewed code `d8cbb7d`; follow-up fix `2933aee`.

## 4. Scope

All provider entry points, authorization, router, model allowlist, approval gate, privacy-v2, API and tests.

## 5. Steps

Traced preview→approval→consume→authorization→router→invoke; inspected binding/expiry/single-use/concurrency; verified mock exact-class policy, model allowlist, privacy validation and API bypass surfaces.

## 6. Findings

- MEDIUM F-01: router trusted supplied payload hash and did not recompute canonical payload integrity. **Fixed** in `2933aee` via `PayloadIntegrity`; payload substitution test added.
- MEDIUM F-02: `PrivacyGate.register` still accepts caller-supplied approval flag. Current HTTP/engine path derives it from server policy; in-process only residual.
- LOW: nonce FIFO eviction, alias-order/router-policy test gaps, timeout metadata not enforced, docs drift.
- PASS: single authorized route, server-minted package-private authorization, exact mock class/ID invariant, model allowlist, approval/TTL/single-use/replay/concurrency, privacy-v2 excluded fields/relationships, strict API JSON.
- No provider call on negative tests.

## 7. Evidence

`ProviderRouter`, `ProviderInvocationAuthorization`, `PrivacyGate`, `ProviderApprovalPolicy`, `ProviderModelPolicy`, `PrivacyProcessor`, tests, ADR-0012.

## 8. Classification

`GREEN` current mock-only path after F-01 fix; `YELLOW` latent F-02/remote-enablement preconditions.

## 9. Terraform

N/A.

## 10. Git status

Clean at review; no changes by reviewer.

## 11. Files changed

None.

## 12. No-change confirmation

No file/model/provider/AWS/credential/network/build operation by reviewer.

## 13. Open questions

Should gate derive approval internally; should enabled providers be explicit separate from registry; router timeout enforcement.

## 14. Risks

Live provider activation before F-02/explicit enablement/credential binding would need further review; in-memory loopback trust.

## 15. Next actions

Keep mock-only; document residuals; re-run verify/CI for any new HEAD.

## 16. Resume point

Reviewed at `d8cbb7d`; F-01 fixed at `2933aee`.
