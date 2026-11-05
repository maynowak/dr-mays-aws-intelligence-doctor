# PROVIDER-SECURITY-03 — Credential / OpenCode Discovery

## 1. Status

`COMPLETE — READ-ONLY DISCOVERY.` No code changed.

## 2. Date/time

2026-11-04T14:58:30+01:00; session `ses_e5926f3a9ffe5F3AWBrGwm4u61`.

## 3. Branch/HEAD

`feature/dr-mays-provider-security-03` at `3ec0e3b`.

## 4. Scope

Credential reference/validator/kind, provider router, OpenCode adapter/command/runner, tests/docs.

## 5. Completed steps

Traced credential flow, searched process/env/file/AWS access, inspected OpenCode binary metadata read-only, reviewed sandbox controls.

## 6. Actual findings

- Current production path PASS: mock-only, no resolver, no process runner, no env/file/AWS access, redacted toString.
- MEDIUM latent: router boolean is a bypass switch, no credential-to-approval binding, FILE names can include `..`/`auth.json`, no tool-deny flag, no binary pin enforcement, no enforced timeout/output/environment clearing.
- OpenCode v2.0.24 binary matches documented SHA-256; binary was not executed.
- A shared OpenCode service/session is running on the host; a future adapter must never attach to it.
- `~/.opencode/.env` exists but was not read.

## 7. Evidence

`ProviderCredentialReference.java`, `CredentialReferenceValidator.java`, `ProviderRouter.java`, `OpenCodeCliAdapter.java`, `OpenCodeCliCommandFactory.java`, `CliProcessRunner.java`, docs; host metadata.

## 8. Classification

`GREEN` current wired path; `YELLOW` future readiness.

## 9. Terraform

N/A.

## 10. Git status

Clean; no files changed.

## 11. Files changed

None.

## 12. No-change confirmation

No file/model/provider/AWS/OpenCode execution/credential/network operation.

## 13. Open questions

Credential contract placement; exact sandbox controls; single canonical binary path; prefix separation.

## 14. Risks

Future boolean enablement/ambient auth, path references, doc-only pins/tool policy, shared service attachment.

## 15. Recommended next actions

Disabled resolver contract, credential binding/version concepts, reference denylist, exact binary pin policy, sandbox readiness policy, contract tests; keep live blocked.

## 16. Resume point

Contract freeze/implementation at `3ec0e3b`.
