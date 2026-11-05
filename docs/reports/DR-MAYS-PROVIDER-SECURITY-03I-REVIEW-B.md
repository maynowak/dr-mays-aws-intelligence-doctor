# DR-MAYS-PROVIDER-SECURITY-03I — Review B (Replay & TTL)

## 1. Status
`COMPLETE — READ-ONLY.` No HIGH/CRITICAL; TTL/capacity assurance gaps documented and partly closed.

## 2. Date/time
2026-11-04T17:21:07+01:00.

## 3. Branch/HEAD
`feature/dr-mays-provider-security-03` at `57351fe` + 03I working tree.

## 4. Scope
Nonce lifecycle, replay cache, TTL, capacity, concurrency, restart, gate consumed state.

## 5. Steps
Read router/gate/engine/request; traced replay/TTL; checked tests, JaCoCo and logs.

## 6. Findings
- PASS: nonce consumed before invoke; TTL pruning; fail-closed capacity; synchronized route; concurrent tests; gate single-use.
- MEDIUM: process-local/restart replay window, wall-clock TTL (no monotonicity), credential binding future propagation, missing direct TTL/capacity tests.
- LOW: local capacity DoS, status collapsing, timeout/output not bound, docs drift.

## 7. Evidence
`ProviderRouter`, `PrivacyGate`, `ProviderInvocationAuthorization`, tests, JaCoCo, logs.

## 8. Classification
GREEN current mock-only; YELLOW future multi-instance/BYOK.

## 9. Terraform
N/A.

## 10. Git status
Read-only; no changes.

## 11. Files changed
None.

## 12. No-change confirmation
No file/model/provider/AWS/credential/network operation.

## 13. Open questions
Restart state, monotonic clock, capacity policy, persistent replay state.

## 14. Risks
Restart ≤30s replay, clock rollback, local store saturation.

## 15. Next actions
Keep single-instance; add TTL/capacity tests; document restart semantics.

## 16. Resume point
Review complete; final implementation includes capacity test and binding propagation.
