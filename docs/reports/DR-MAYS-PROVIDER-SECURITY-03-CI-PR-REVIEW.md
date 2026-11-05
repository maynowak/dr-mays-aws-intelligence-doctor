# DR-MAYS-PROVIDER-SECURITY-03-CI — Independent PR Review

## 1. Status

`COMPLETE — READ-ONLY.` No HIGH/CRITICAL blockers. Five LOW documentation/error-semantics findings and six INFO observations. CI not claimed.

## 2. Date/time

- Started: 2026-11-04 16:55 UTC
- Finished: 2026-11-04 17:59 UTC
- Local evidence: verify 17:54:43 +01:00; Docker 17:55 +01:00.

## 3. Branch / HEAD

`feature/dr-mays-provider-security-03` at `5b1fe6a`; `origin/main` `3ec0e3b`; 8 feature commits.

## 4. Scope

Exact PR diff `origin/main...HEAD`: authorization/credential binding, rotation/provenance, `ApprovedInvocation`, router enforcement, replay/TTL/capacity/concurrency, enablement, mock exception, privacy v2, legacy fail-closed, credential/OpenCode disablement, public history and workflow security.

## 5. Steps

Read all changed sources/tests/docs; traced controller → service → engine → gate → router; checked single invoke caller; scanned diff/history for secrets; reviewed workflow; verified coverage/test totals from local artifacts; checked Docker security flags.

## 6. Findings

No HIGH/CRITICAL.

Confirmed PASS:
- Authorization final/package-private, single mint site, redacted toString.
- Router binds provider/model/expertise/policy/payload/credential; re-derives hash.
- TTL replay map, capacity fail-closed, synchronized consumption, concurrency test.
- Enablement separation, exact mock class exception, strict default gate.
- Privacy-v2 explicit allowlist; excluded fields rejected; relationships omitted.
- Credential resolution disabled; FILE/KEYRING unconstructible; OpenCode no production process runner.
- No real secrets in history; workflow read-only, pinned, no deploy/credentials.
- 470 tests and coverage match README/readiness.

LOW:
1. Capacity exhaustion maps to generic HTTP 500 rather than 429/`Retry-After`.
2. `consumed` preview set is FIFO-capped; old consumed ID may report `NOT_FOUND` instead of `CONSUMED`.
3. Authorization contract field list omitted `approvalRequired`/`credentialBinding` (corrected in this audit commit).
4. Audit index stale branch and duplicate 03 execution row (corrected).
5. `AGENTS.md` ADR range stale (corrected).

INFO: in-process constructor composition is not a REST boundary; fingerprint is caller-supplied placeholder; router monitor held across invoke/timeout unenforced; wrong-token attempts limited only by token entropy; per-preview alias key makes receipts non-reproducible; dead package-private Ollama analyzer remains.

## 7. Evidence

`/tmp/opencode/ps03ci-verify.log`, `/tmp/opencode/ps03ci-docker.log`, JaCoCo XML, source line references in the reviewer report, `origin/main...HEAD` diff, workflow file.

## 8. Classification

`GREEN (local read-only review)`; no merge recommendation on its own.

## 9. Terraform checks

None; no infrastructure diff; read-only scope.

## 10. Git status

Reviewer made no changes; working tree clean.

## 11. Files changed

None by reviewer. Orchestrator corrected LOW-3/4/5 documentation in the audit commit.

## 12. No-change/no-call confirmation

No push/merge/deploy, no model/provider/AWS/OpenCode/network call, no credential read.

## 13. Open questions

Capacity status semantics; consumed-status TTL; secret-scan false-positive handling; future approver identity/multi-instance ADR.

## 14. Risks

In-memory/single-instance state, wall-clock TTL, unauthenticated approver, non-authoritative fingerprint, declarative sandbox, pattern-based privacy, monitor-held invocation, no enforced timeout, CI unverified at review time.

## 15. Next actions

Independent review complete. Proceed only with exact-SHA CI and PR; keep live providers blocked. LOW-1/2 deferred with explicit acceptance.

## 16. Resume point

After audit commit: push feature branch, verify CI, create PR, verify PR CI, record evidence in PR body/comment.
