# OpenCode CLI Adapter

Status: **adapter mock-verified; live execution BLOCKED.**

## Installed version

- CLI: OpenCode `v2.0.24` (V2 line)
- Binary: `/home/dci-student/.opencode/bin/opencode`
- SHA-256: `d73436b88f2c412abb4e72056c2f6d133d9651f121952ade7fc3fe95fc9f5ddc`
- Existing shared service was observed on this host; a live adapter must not attach to it.

## Why live execution is blocked

- Default permissions are effectively allow-all, including shell/edit/web tools.
- Non-interactive runs cannot ask; tool denials may still produce exit 0 and a degraded answer.
- No CLI-level timeout; provider/tool/MCP execution can run far longer than the product allows.
- JSON output is JSONL events, not a stable aggregate schema; answer text is split across events.
- Sessions and git snapshots persist transcripts locally.
- Project config/instruction discovery can inject repository content.
- Plugins/MCP children can execute code and inherit credentials.
- Unknown global flags may be ignored; configuration is version-sensitive.
- Egress cannot be enforced from inside the CLI.

## Implemented adapter surface

- Disabled-by-default `OpenCodeCliAdapter`; no `ProcessBuilder`, no `Runtime.exec`, no shell.
- Fixed argv list built by `OpenCodeCliCommandFactory`:
  `binary run --format json --model <model>`.
- Isolated `HOME`, XDG dirs and `OPENCODE_CONFIG_DIR`; project config/update/model fetch disabled.
- Minimal `PATH`; no AWS/OpenAI credentials.
- Bounded stdin/stdout and a 30-second timeout.
- `CliProcessRunner` is an interface exercised only with simulated results.
- Live execution remains false; a call returns `DISABLED` with `OPENCODE_LIVE_EXECUTION_BLOCKED`.
- Nonzero exit, oversized output and blank output map to explicit failure statuses.

## Preconditions for any future live adapter

1. OS-level sandbox or container with no host repo/home access.
2. Deny-all tool policy verified on the exact pinned version.
3. Egress restricted to the approved provider.
4. Ephemeral session/history storage; no infrastructure transcript.
5. Sanitized environment; no AWS/OpenCode credentials.
6. Hard external timeout and process-tree termination.
7. Exit/output/JSONL completeness canary.
8. Separate explicit approval and audit log.

Until all are proven: direct Spring AI/provider adapters remain the regular inference path, and OpenCode live execution stays **BLOCKED**.
