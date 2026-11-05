# PROVIDER-SECURITY-03 — Review B: Credential & CLI Isolation

## 1. Status

`COMPLETE — READ-ONLY REVIEW.` No HIGH/CRITICAL; latent MEDIUM/LOW credential and sandbox gaps documented.

## 2. Date/time

2026-11-04T15:26:06+01:00; session `ses_e59108261ffeEc67202XfKDkQm`.

## 3. Branch/HEAD

`feature/dr-mays-provider-security-03` at `d8cbb7d`; follow-up fix `2933aee`.

## 4. Scope

Credential references/policy/resolver, router credential checks, model policy, OpenCode adapter/sandbox/command, startup, module boundaries, docs.

## 5. Steps

Read provider/privacy/expertise code; traced REST path; searched for env/file/process/AWS access; verified no production runner; inspected config/tests/docs.

## 6. Findings

- PASS: no API-key/env/file/keychain/AWS/OpenCode credential read.
- PASS: `CredentialResolutionPolicy.disabled()` + `DisabledCredentialResolver`; router fails credential-requiring providers.
- PASS: no production `ProcessBuilder`/runner; adapter default disabled; sandbox `blocked()` default.
- MEDIUM latent: FILE/KEYRING denylist incomplete (`.ssh`, `.aws`, `.netrc`, `id_rsa`, `oauth.json`, etc.). Partially hardened; still document before future resolver.
- LOW: payload hash trust (fixed by Review A follow-up), credential reference not in authorization tuple, registry membership = enablement, CLI timeout/output not enforced, docs drift.
- No HIGH/CRITICAL; no live process or provider reachable.

## 7. Evidence

`ProviderCredentialReference`, `CredentialResolutionPolicy`, `DisabledCredentialResolver`, `ProviderRouter`, `OpenCodeCliAdapter`, `OpenCodeCliCommandFactory`, `OpenCodeSandboxPolicy`, docs/tests.

## 8. Classification

`GREEN` current mock-only path; `YELLOW` future credential/OpenCode readiness.

## 9. Terraform

N/A.

## 10. Git status

Clean at review; no changes by reviewer.

## 11. Files changed

None.

## 12. No-change confirmation

No file/OpenCode/model/AWS/credential/network operation.

## 13. Open questions

Expand denylist, bind credential reference/version, make live adapter structurally unavailable, pin pull strategy, fix docs.

## 14. Risks

Future resolver/CLI activation without denylist/runner controls; docs overclaims.

## 15. Next actions

Keep BYOK/OpenCode blocked; expand denylist/tests; enforce controls in a future runner; re-verify.

## 16. Resume point

Reviewed at `d8cbb7d`; follow-up `2933aee`.
