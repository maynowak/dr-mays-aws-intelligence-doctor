# OpenCode Sandbox Readiness

Status: **BLOCKED — live execution not enabled.**

## Installed baseline

- OpenCode `2.0.24` (previously observed; re-check before any future work).
- Binary SHA-256 documented in `OPENCODE_CLI_ADAPTER.md`.
- Do not attach to the shared OpenCode service/session running on the host.
- Do not read `auth.json`, `~/.opencode/.env` or provider credentials.

## Readiness policy

`OpenCodeSandboxPolicy.ready()` requires all controls:

1. OS/container isolation.
2. No writable project/home filesystem.
3. No AWS credentials.
4. No existing OpenCode/other credentials.
5. Deny all tools.
6. No MCP/plugins.
7. Controlled provider-only egress.
8. Bounded runtime/process-tree timeout.
9. Bounded memory.
10. Streaming output cap.
11. Ephemeral session storage.
12. Cleanup of session/temp artifacts.

The default is `blocked()`; the adapter also requires `liveExecutionEnabled=true`. There is no production `CliProcessRunner`.

## Missing technical work

- Binary path/version/SHA verification inside the runner.
- Enforced environment clearing and HOME/XDG isolation.
- Container/netns/egress policy.
- Process-tree timeout, memory and streaming output limits.
- JSONL completeness/exit-code canary and tool-denial detection.
- Session cleanup/no git snapshots.
- Separate approval + audit event per invocation.
- Canary E2E with synthetic data only.

CLI permission settings alone are **not** OS isolation. Live OpenCode remains a separate, explicitly approved follow-up.
