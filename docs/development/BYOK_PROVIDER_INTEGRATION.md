# BYOK Provider Integration

Status: mock provider only; real credentials and external providers disabled.

## Provider contracts

| Type | Role |
| --- | --- |
| `ModelProviderDescriptor` | ID, kind, capabilities, credential/approval requirements |
| `ProviderCapabilities` | JSON, local-only, tools flag, payload/output caps |
| `ProviderCredentialReference` | Logical reference only; no secret |
| `ModelInvocationRequest` | Provider/model, sanitized payload, payload hash source, credential ref, timeout, output cap |
| `ModelInvocationResult` | Status, bounded content, sizes, safe error code |
| `ModelProvider` | Single invocation contract |
| `ProviderRegistry` | Explicit catalog; duplicate IDs rejected |
| `ProviderRouter` | Allowlist, credential precheck, no fallback |

## Provider defaults

| Provider | Enabled | Local | Credential | Approval | Tools |
| --- | --- | --- | --- | --- | --- |
| `mock` | yes | yes | none | no | no |
| `ollama` | no | yes | none | yes | no |
| `opencode` | no | no | reference required | yes | no |

The mock provider is deterministic and offline. It receives the same sanitized payload shape as a future remote provider.

## BYOK rules

- No API key in REST requests, query strings, URLs, logs, Git, prompts or responses.
- No automatic environment scanning or reuse of `AWS_*`, `OPENCODE_*` or provider credentials.
- No reading of OpenCode `auth.json` or any existing user credential store.
- Credential references are provider-scoped logical names. Environment references must use the `DR_MAYS_` prefix.
- Credential resolution is disabled in this phase. A remote provider requiring credentials returns `CREDENTIAL_UNAVAILABLE`.
- Real key storage (OS keychain / approved secret backend) requires a separate security review.

## Routing and failure model

`ProviderRouter`:

1. Rejects unregistered providers with `NOT_ALLOWED`.
2. Rejects providers not in the enabled allowlist with `DISABLED`.
3. Rejects oversized payloads with `PRIVACY_REJECTED`.
4. Rejects invalid/unresolved credential references with `CREDENTIAL_UNAVAILABLE`.
5. Invokes exactly one provider; no fallback or changed-payload retry.
6. Maps provider exceptions to `UNAVAILABLE` and oversized output to `INVALID_RESPONSE`.

## Configuration

Current production default is mock-only. The legacy `dr.mays.ai.enabled=true` path is fail-closed and returns `APPROVAL_REQUIRED` without constructing or calling Ollama; the new expertise engine is also mock-only. OpenCode is not registered for execution.

Future provider configuration must be explicit, allowlisted and approval-bound. It must not use a user-supplied base URL or arbitrary model/provider strings without validation.

## Test evidence

- 36 provider-specific tests cover descriptors, credential references, registry duplicates, allowlisting, no fallback, payload/output bounds, mock output and disabled OpenCode execution.
- No real provider or credential was used.
