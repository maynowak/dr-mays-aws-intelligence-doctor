# AI Model Security

## Default posture

- AI is **disabled by default** (`dr.mays.ai.enabled=false`).
- The default analyzer is the deterministic mock; no model is contacted.
- Ollama is optional, local (`127.0.0.1:11434`) and only when explicitly enabled.
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

## Prompt injection

Prompts explicitly state that resource metadata is untrusted and must not be followed as instructions. Only validated fields are serialized; free-form tags are not included in the prompt builder.

## Logging

Do not log raw prompts or raw model responses. Safe status/metadata may be logged. No secret scan result is a substitute for a security review.

## Real inference

A real Ollama run is **NOT VERIFIED** because no local model is installed. Enabling it requires explicit opt-in, a local model, and a separate review. No automatic install/download is permitted.
