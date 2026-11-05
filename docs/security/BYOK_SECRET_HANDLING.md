# BYOK Secret Handling

Status: no real secret is accepted, read, resolved or transmitted in this phase.

## Hard rules

- No API key in REST JSON, query strings, URLs, headers captured by this app, logs, errors, Git or model payloads.
- No automatic provider registration.
- No automatic environment scan.
- No reading of `~/.aws`, OpenCode `auth.json`, keychains or secret files.
- No reuse of existing OpenCode/AWS credentials.
- No secret in exception messages or audit reports.

## Credential references

`ProviderCredentialReference` contains:

- `providerId` — must match the provider descriptor.
- `kind` — `NONE` or `ENV`; `FILE` and `KEYRING` are rejected at construction until a secure resolver/containment contract is approved.
- `reference` — logical name only.

Environment references must match `DR_MAYS_[A-Z0-9_]{2,64}`. A reference is validated structurally; the environment is not read. A remote provider requiring a credential returns `CREDENTIAL_UNAVAILABLE` while resolution is disabled.

## Future resolution requirements

- Resolve only after a valid approval.
- Use an approved backend (OS keychain or dedicated secret store).
- Bind approval to a credential fingerprint, not the secret.
- Zeroize best-effort; avoid string copies where feasible.
- Rotate/invalidate approvals on credential change.
- Fail closed; no fallback to default credential chains.

## OpenCode-specific

OpenCode credentials and session/auth files are not read. The disabled CLI adapter's environment contains no provider keys, only isolated HOME/XDG paths and a minimal PATH. MCP/plugins are not enabled.

## Tests

- Environment references outside the `DR_MAYS_` prefix are rejected.
- Mismatched provider references are rejected.
- Mock/no-credential references are allowed only for providers that do not require credentials.
- Remote credential resolution remains disabled and is tested as `CREDENTIAL_UNAVAILABLE`.
- Payload/process arguments/environment are checked for AWS/OpenAI key names.
