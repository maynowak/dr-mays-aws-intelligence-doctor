# ADR-0010: AI Intelligence Boundary

- Status: Accepted
- Date: 2026-11-03
- Scope: `dr-mays-ai`, `dr-mays-api`

## Context

Deterministic rules, evidence and verified DNA edges already provide the technical truth. DR-MAYS-06 may add optional explanations, but must not let a model create findings, evidence, resources or relationships.

## Decision

- Put technology-neutral contracts in `dr-mays-ai`: `IntelligenceAnalyzer`, `IntelligenceRequest`, `IntelligenceResult`, explanations, recommendations, evidence references, execution status and model metadata.
- The AI module depends on `dr-mays-core`; Spring AI/Ollama types live only in adapter classes. The API imports only the contract and default mock configuration.
- The API request contains synthetic resources; the server runs the existing diagnosis engine and DNA builder first. Clients cannot inject technical findings/evidence.
- **Default analyzer is the deterministic mock.** It uses only request findings/evidence/relationships and never adds findings.
- **Spring AI/Ollama is opt-in** via `dr.mays.ai.enabled=true` and provider/model properties. No auto connection at startup; no model download/install; no tools.
- Structured model output is validated against the request:
  - every explanation must reference an existing finding;
  - every evidence reference must match existing evidence;
  - every relationship must already be verified;
  - no unknown resources or additional findings;
  - invalid output becomes `INVALID_RESPONSE`/`FAILED`, never merged into technical results.
- Prompt templates are versioned resources under `prompts/`, capped at 32 KiB. Only minimized finding/evidence/relationship data is sent; tags/names are treated as untrusted data. No secrets/account credential data.
- AI failure/timeout/disabled status must not affect the deterministic diagnosis.
- No tool calling, shell/AWS/HTTP/filesystem actions, external providers or public deployment.

## Consequences

- The feature works offline with the mock and is CI-safe.
- Real model quality/E2E cannot be claimed without a locally installed model and explicit opt-in.
- Provider isolation is by package/class boundary in the single AI module; no Ollama imports in API/core.
- Validators are the most important safety layer and require thorough tests.
