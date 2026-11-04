# ORCH-01A — Agent B — OpenCode Research

## 1. Current status

`COMPLETE — READ-ONLY RESEARCH.` Live OpenCode execution is **BLOCKED** for regular product operation.

## 2. Audit date/time

- Date: 2026-11-04
- Completed: 2026-11-04T08:07:44+01:00
- Agent session: `ses_e5aa1afa7ffeg3HM8Y3gkp6n5j`

## 3. Git branch/HEAD observed

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD: `36ef4e5a105d8a11f10bf202c7cc60e978540c3f`

## 4. Audit scope

Installed OpenCode CLI version/lineage, non-interactive contract, provider/model selection, structured output, exit codes, timeouts, config precedence, permissions, MCP/plugins, sessions, API-key handling, isolation feasibility, and direct-adapter comparison. No model calls, no credential reads.

## 5. Completed work steps

- Verified binary, `--version`, hash and platform.
- Enumerated safe help surfaces (`run`, `models`, `session`, `mcp`, `plugin`, `debug`, `auth`, `serve`, `service`).
- Probed safe failure paths for exit codes.
- Verified isolated HOME/XDG/config behavior with `debug paths` and `service status`.
- Reviewed upstream V2 docs/source and safe local behavior.
- Compared direct Spring AI/provider adapters with the CLI path.

## 6. Actual findings

- Installed CLI: **OpenCode v2.0.24** (V2 line); binary SHA-256 `d73436b88f2c412abb4e72056c2f6d133d9651f121952ade7fc3fe95fc9f5ddc`.
- `run` concatenates non-TTY stdin into the prompt; an unclosed pipe blocks.
- No CLI-level timeout; provider/MCP/tool timeouts are longer or unbounded.
- `--format json` is JSONL events, not an aggregate schema; text is split across events.
- Default permissions are effectively allow-all (shell/read/edit/web), which violates this project's hard controls.
- Unknown global flags can be silently ignored; auto-rejected tools may still yield exit 0.
- Config discovery can pick up project files; plugins/MCP can execute code and inherit credentials.
- Session SQLite and git snapshots create durable local transcripts.
- Isolated HOME/config and no shared service are possible, but external egress/tool/credential isolation is not proven on this build.
- **Direct Spring AI/provider adapters remain the correct regular path**; OpenCode adds agentic/tool risk with no functional need for finding explanation.

## 7. Evidence/file references

- `/home/dci-student/.opencode/bin/opencode` v2.0.24; `--help`, `run --help`, `debug paths`, `service status` under isolated HOME.
- Upstream docs: `opencode.ai/v2/docs/cli`, `/config`, `/permissions`, `/policies`, `/mcp-servers`, `/plugins`, `/providers`, `/models`, `/tools`, `/network`.
- Project controls: `docs/security/AI_MODEL_SECURITY.md`, `dr-mays-ai/.../PromptBuilder.java`, `IntelligenceResultValidator.java`.

## 8. Classification

`GREEN` for research completeness; `ORANGE/BLOCKED` for live CLI product operation. Direct adapter: `GREEN`.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

Read-only; no tracked modifications. Only the orchestrator's untracked log.

## 11. Files changed

None in the repository. Scratch artifacts under `/tmp/opencode/` only.

## 12. Explicit no-change confirmation

No model/provider call, no `opencode run`, no credential/auth file content read, no secret printed, no AWS/network call, no repo file changed.

## 13. Open questions

- Exact permission schema behavior in 2.0.24.
- Exit semantics for auto-rejected tools.
- Whether `OPENCODE_DISABLE_PROJECT_CONFIG`/`OPENCODE_MODELS_PATH` are honored.
- Whether container/netns isolation is acceptable.
- Whether any OpenCode integration is actually required.

## 14. Risks

- Default allow-all tools and host-authority shell.
- Silent degradation with exit 0.
- Session/prompt persistence and repo instruction injection.
- Credential inheritance if HOME/env are not isolated.
- Unbounded runtime; version drift; fail-open argv.

## 15. Recommended next actions

1. Keep direct adapter as product path; do not make OpenCode a live dependency.
2. If an adapter is required, scope it as disabled-by-default, containerized/mock-only.
3. Pin any future CLI version/hash and run an isolated canary.
4. Document that OpenCode is not on the inference path.

## 16. Current resume point

Research complete at `36ef4e5`. No OpenCode execution authorized without separate approval.
