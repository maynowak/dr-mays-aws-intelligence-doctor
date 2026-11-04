# ORCH-01D — BYOK & OpenCode Adapter (Mock-Verified)

## 1. Current status

`COMPLETE — PROVIDER CONTRACTS AND MOCK ROUTER IMPLEMENTED.` OpenCode live execution remains **BLOCKED**.

## 2. Audit date/time

- Implementation: 2026-11-04T08:21–08:23+01:00
- Provider tests: 2026-11-04T08:22:47+01:00

## 3. Git branch and HEAD

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- Last commit: `5723946` (privacy)
- Provider commit: pending after this report

## 4. Audit scope

Provider-neutral contracts, explicit provider/model selection, logical credential references, controlled provider config, mock provider, bounded timeouts/output, error statuses and disabled OpenCode CLI adapter.

## 5. Completed work steps

- Added `ProviderKind`, `ProviderCapabilities`, `ModelProviderDescriptor`.
- Added `ProviderExecutionStatus`, `ProviderCredentialReference`, `CredentialKind`, `CredentialValidationResult` and `CredentialReferenceValidator`.
- Added `ModelInvocationRequest`, `ModelInvocationResult`, `ModelProvider`.
- Added `MockModelProvider`.
- Added `ProviderRegistry` and allowlist-only `ProviderRouter` with no fallback.
- Added `OpenCodeCliCommand`, command factory, `CliProcessRunner`, `CliProcessResult` and disabled `OpenCodeCliAdapter`.
- Added 33 provider tests; targeted suite passed.

## 6. Actual findings

- Mock provider is the only enabled provider; Ollama/OpenCode descriptors are disabled.
- Remote providers with credential requirements cannot execute while credential resolution is disabled.
- No API key is accepted, stored, logged or resolved.
- OpenCode adapter uses fixed argv, sanitized environment, isolated HOME/config, bounded stdin/stdout/timeout and no shell.
- No `ProcessBuilder` or real CLI execution exists; enabled path is only exercised with a simulated runner.
- Router rejects unknown/disabled providers, invalid credential references, oversized payloads/outputs and provider exceptions without fallback.
- `OpenCode v2.0.24` live operation is BLOCKED by default allow-all tools, session persistence and unproven OS isolation.

## 7. Evidence/file references

- `dr-mays-ai/src/main/java/io/github/maynowak/drmays/ai/provider/`
- `dr-mays-ai/src/test/java/io/github/maynowak/drmays/ai/provider/`
- `docs/reports/DR-MAYS-ORCHESTRATION-01A-OPENCODE-RESEARCH-EXECUTION_LOG.md`
- `docs/decisions/ADR-0011-privacy-first-provider-orchestration.md`

## 8. Classification

`GREEN (local)` for provider contracts/mock/router/disabled adapter. `BLOCKED` for live OpenCode and real BYOK.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

New provider source/test files pending commit; no provider model call.

## 11. Files changed

- Added provider contracts, mock/router and disabled CLI adapter.
- Added 33 provider tests.
- Added this report.

## 12. Explicit no-change confirmation

No external model call, API key use, credential read, AWS call, process execution, push, merge or deployment.

## 13. Open questions

- Which credential backend will be approved for a later phase.
- Whether OpenCode will ever be required; OS-level sandbox approval is mandatory.
- Real provider/model verification requires a separate E2E approval.

## 14. Risks

- A future live CLI adapter must not inherit AWS/OpenCode credentials or project config.
- Real provider timeout/stream behavior remains unverified.
- Approval-to-credential binding needs a real credential fingerprint before production transmission.

## 15. Recommended next actions

1. Implement expertise parser/validator/engine.
2. Integrate mock-only preview/approval/explain API.
3. Run full gates and Docker regression.
4. Keep OpenCode live and real BYOK disabled.

## 16. Current resume point

ORCH-01E AI Expertise. Provider package green locally; 33 new tests; no live provider.
