# PROVIDER-SECURITY-03H — Review B (Credential / OpenCode)

## 1. Status

`COMPLETE — READ-ONLY.` No HIGH/CRITICAL; credential binding remains contract-only/deferred.

## 2. Date/time

2026-11-04T16:09:48+01:00.

## 3. Branch/HEAD

`feature/dr-mays-provider-security-03`, `0319e02` + 03H working tree.

## 4. Scope

Credential references/binding/policy/resolver, router credential checks, OpenCode sandbox/adapter, privacy-v2, docs, 467-test/Docker logs.

## 5. Steps

Read credential/privacy/provider code and tests; traced REST path; searched env/file/process/AWS access; inspected docs/logs.

## 6. Findings

- PASS: FILE/KEYRING construction disabled; ENV/NONE strict; disabled resolver; no secret/env/file/AWS access; no production CLI runner.
- PASS: approval is gate-owned; unique auth; payload integrity; explicit model allowlist; privacy-v2 preserved.
- MEDIUM: full credential↔authorization binding is not wired (contract-only), `ProviderCredentialBinding` inert/weak, dead FILE/KEYRING denylist before disabled throw, approval policy not tied to enablement.
- LOW: docs drift, FIFO nonce eviction, self-declared OpenCode sandbox booleans, status collapsing.

## 7. Evidence

`ProviderCredentialReference`, `ProviderCredentialBinding`, `CredentialResolutionPolicy`, `DisabledCredentialResolver`, `ProviderRouter`, `OpenCodeSandboxPolicy`, `PrivacyPolicy`, tests/docs/logs.

## 8. Classification

`GREEN` current mock-only; `YELLOW` future credential/OpenCode.

## 9. Terraform

N/A.

## 10. Git status

Read-only; no changes.

## 11. Files changed

None.

## 12. No-change confirmation

No file/OpenCode/model/AWS/credential/network operation.

## 13. Open questions

Wire credential binding before resolver; complete denylist; couple approval to enablement.

## 14. Risks

Rotation blindness, inert binding false assurance, future path-shaped refs, self-asserted sandbox.

## 15. Next actions

Keep disabled; wire binding/rotation before BYOK; document deferral.

## 16. Resume point

Review complete; mock-only path GREEN.
