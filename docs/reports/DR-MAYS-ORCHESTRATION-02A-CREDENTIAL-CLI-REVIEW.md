# ORCH-02A-04 — Credential & CLI Boundary Review

## 1. Current status

`COMPLETE — READ-ONLY REVIEW.` No blocking credential/CLI exposure in the wired production path. Documentation corrections and small hardening were recommended.

## 2. Audit date/time

- Review: 2026-11-04T11:23:47+01:00
- Agent session: `ses_e59ed8dd3ffeEwdLgmOM4jw28V`

## 3. Git branch/HEAD observed

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD: `87e73ad` (match expected)

## 4. Scope

Credential references, provider resolution, OpenCode adapter/process boundaries, AWS reachability, tool calls, startup behavior and module boundaries.

## 5. Completed steps

- Reviewed provider/privacy/expertise code and Maven/config/docs.
- Searched for process/env/auth-file/AWS access.
- Inspected Spring AI initialization behavior and tests.
- Checked module dependency boundaries and runtime wiring.

## 6. Actual findings

- PASS: no API key resolution/acceptance; no environment reads; credential references are logical.
- PASS: no OpenCode auth-file reuse; isolated HOME/XDG if adapter is ever constructed.
- PASS: no production `ProcessBuilder`/`Runtime.exec`; no production `CliProcessRunner`; adapter live flag defaults false.
- PASS: no AWS SDK in core/AI/API; no AWS credential access.
- PASS: engine router allowlist is mock-only; no external provider reachable.
- PASS: all descriptors declare no tools; Spring AI path registers no tools; OpenCode live blocked.
- PASS: no provider contact at startup by default.
- MEDIUM latent: `ProviderRouter` itself does not enforce `requiresApproval`; current engine gate and mock-only allowlist prevent exploitation. Documented as a precondition before non-mock allowlisting.
- LOW: docs incorrectly said Ollama remained opt-in via `dr.mays.ai.enabled=true`; **corrected** to fail-closed.
- LOW: CLI timeout/stdin/stdout bounds are command-descriptor metadata, not enforced while live is disabled; documentation clarified.
- LOW: FILE/KEYRING credential references are not shape-validated/redacted; no resolution/logging path exists.

## 7. Evidence/file references

- `dr-mays-ai/.../provider/` and `ai/privacy/`
- `dr-mays-ai/.../IntelligenceConfiguration.java`, `PrivacyGatedLegacyAnalyzer.java`
- `dr-mays-api/.../application.yml`
- POM module boundaries
- Docs: `BYOK_PROVIDER_INTEGRATION.md`, `BYOK_SECRET_HANDLING.md`, `OPENCODE_CLI_ADAPTER.md`

## 8. Classification

Initial `YELLOW` (latent router/CLI/doc issues); current wired path **GREEN**. No ORANGE/RED.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

Read-only; only the orchestrator's 02A log was untracked at review time.

## 11. Files changed

None by reviewer.

## 12. Explicit no-change confirmation

No file/OpenCode/model/AWS/credential/network call by reviewer.

## 13. Open questions

- Router-level approval enforcement before future non-mock allowlist.
- Explicit `pull-model-strategy=never` defense-in-depth.
- Credential-reference hardening before future serialization/logging.

## 14. Risks

- Future wiring could bypass gate if Router is reused without approval.
- Future live CLI needs path/binary/timeout/output enforcement.
- Docs drift can mislead operators.

## 15. Recommended next actions

1. Correct docs (done).
2. Keep OpenCode live and BYOK disabled.
3. Add router approval enforcement/credential hardening before remote enablement.
4. Re-verify tests/coverage at HEAD.

## 16. Current resume point

Credential/CLI review complete; mock-only production path remains fail-closed.
