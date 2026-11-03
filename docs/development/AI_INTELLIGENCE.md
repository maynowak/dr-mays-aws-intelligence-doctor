# AI Intelligence

The AI layer explains deterministic findings, evidence and verified DNA relationships. It never creates technical findings, evidence, resources or relationships.

## Architecture

```text
POST /api/v1/intelligence/explain
  -> DiagnosisService (deterministic findings)
  -> InfrastructureGraphBuilder (verified edges)
  -> IntelligenceRequest (findings + evidence + edges + completeness)
  -> IntelligenceAnalyzer
       default: MockIntelligenceAnalyzer (offline, deterministic)
       opt-in: OllamaIntelligenceAnalyzer -> Spring AI ChatClient -> local Ollama
  -> IntelligenceResultValidator
  -> versioned DTOs
```

- Core engine remains authoritative.
- Default analyzer is the deterministic mock; Docker/CI require no model or Internet.
- Spring AI 2.0.2 and the Ollama starter are confined to `dr-mays-ai`; the API imports only the `IntelligenceAnalyzer` contract.
- Ollama is opt-in via `dr.mays.ai.enabled=true`; no connection at startup, no model download, no tools.

## Contracts

| Type | Purpose |
| --- | --- |
| `IntelligenceAnalyzer` | Analysis contract |
| `IntelligenceRequest` | Verified findings, edges and completeness |
| `IntelligenceResult` | Status, summary, explanations, recommendations, evidence refs, edges, uncertainties |
| `FindingExplanation` | Explanation tied to an existing finding |
| `Recommendation` | Suggested action, separate from findings |
| `EvidenceReference` | Existing evidence only |
| `ModelExecutionStatus` | `SUCCESS`, `DISABLED`, `UNAVAILABLE`, `FAILED`, `INVALID_RESPONSE` |
| `ModelMetadata` | Provider/model/version |

## Mock analyzer

The mock:
- uses only request findings/evidence/relationships;
- creates one explanation and one evidence reference per finding;
- preserves verified relationships exactly;
- marks PARTIAL discovery and empty findings as uncertainties;
- is deterministic and offline.

## Spring AI / Ollama adapter

- Uses `ChatClient` through the narrow `ChatCompletion` transport.
- Model response is parsed into a bounded internal shape and validated.
- Unknown findings, evidence, resources or relationships are rejected.
- Malformed JSON/severity → `INVALID_RESPONSE`; connection failure → `UNAVAILABLE`; blank output → `UNAVAILABLE`.
- No tool calling is registered.

## Prompts

- Versioned resource: `dr-mays-ai/src/main/resources/prompts/intelligence-v1.txt`.
- Maximum prompt size: 32,000 characters.
- Only minimized finding/evidence/relationship data is included; tags/names are untrusted and not instructions.
- Prompt explicitly forbids inventing resources, evidence, findings or relationships and forbids reachability claims from public ingress.

## Validation

- Every explanation must match an existing finding.
- Every evidence reference must match an existing finding evidence source.
- Every returned relationship must already exist in the request.
- No duplicate explanations, unknown resources or invented findings.
- Failed validation yields `INVALID_RESPONSE`; technical diagnosis remains unchanged.

## API

`POST /api/v1/intelligence/explain` takes synthetic resources plus optional `partialDiscovery`; the server derives findings/DNA first. Versioned DTOs, Bean Validation, 100-resource limit and RFC 9457 errors apply. OpenAPI is updated.

## Real inference status

**NOT VERIFIED.** Ollama CLI is installed, but no local model is present. No model was downloaded. A real inference run requires a separate explicit activation and local model.

## Privacy

No credentials, secrets, object contents or full inventories are sent. Data is minimized to findings/evidence/relationships. No raw prompts/responses are logged. See `docs/security/AI_MODEL_SECURITY.md`.
