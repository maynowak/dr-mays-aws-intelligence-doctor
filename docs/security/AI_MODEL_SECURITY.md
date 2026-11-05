# AI Model Security

## Default posture

- AI is **disabled by default** (`dr.mays.ai.enabled=false`).
- The default analyzer is the deterministic mock; no model is contacted.
- The Ollama adapter source remains local (`127.0.0.1:11434`), but the legacy raw path is fail-closed: enabling AI returns `APPROVAL_REQUIRED` without a call. A future integration must use preview/approval.
- No automatic model download/install; no external provider.
- No public exposure; the API remains loopback-only and unauthenticated.

## Allowed model input

- Existing finding IDs, severity, resource canonical identity, title, description.
- Existing evidence source/observed/expected/details (minimized).
- Existing verified relationship type/endpoints/evidence.
- Discovery completeness and explicitly listed uncertainties.

## Forbidden input

- AWS credentials, tokens, secret contents.
- S3 object contents.
- Unfiltered full account inventories.
- Raw logs or prompts containing secrets.
- Instructions embedded in resource names/tags: metadata is untrusted data, never model instructions.

## Hard controls

- Prompt cap: 32,000 characters.
- No tool calling, shell, AWS, HTTP, filesystem or mutation actions.
- Structured response validation:
  - evidence references must exist;
  - resource identities must come from the request;
  - relationships must already be verified;
  - no new findings;
  - invalid output is rejected.
- Model failure/timeouts fall back to explicit status; deterministic diagnosis remains available.
- Provider types are isolated to `dr-mays-ai`; the API uses only the contract.

## Privacy-first provider controls

- ORCH-01 adds server-side preview/approval before any remote provider invocation.
- Remote provider payloads are `SanitizedExpertiseRequest` only: aliases and allowlisted fields, never raw domain objects.
- Approval is bound to exact payload hash + provider + model + expertise + policy, single-use and TTL-limited.
- Mock provider is the only enabled provider. Ollama/OpenCode execution is disabled; OpenCode live is BLOCKED.
- BYOK references are logical only. No API key in REST, URLs, logs, Git or prompts; no environment/auth-file scanning. Pattern-based secret checks are not exhaustive.
- Pseudonymization is reversible by the local alias map; no anonymity claim.

## ORCH-03 provider authorization hardening

- `ProviderInvocationAuthorization` is server-minted with a package-private constructor; only the privacy gate can create it.
- The router requires an authorization bound to canonical payload hash, provider, model, expertise, policy version and expiry; single-use is enforced.
- Exact mock class exemption only; model allowlist is exact (`mock` / `deterministic-v1`).
- Default privacy policy is `privacy-v2` with 8 allowed fields; relationships are omitted unless explicitly allowed.
- Credential resolution/OpenCode live execution remain disabled/blocked.

## Prompt injection

Prompts explicitly state that resource metadata is untrusted and must not be followed as instructions. Only validated fields are serialized; free-form tags are not included in the prompt builder.

## Logging

Do not log raw prompts or raw model responses. Safe status/metadata may be logged. No secret scan result is a substitute for a security review.

## Real inference

No real model/provider run has executed. Enabling a real provider requires a separate approval covering provider, model, payload preview, credential reference and retention. OpenCode live execution remains BLOCKED pending OS-level isolation.
