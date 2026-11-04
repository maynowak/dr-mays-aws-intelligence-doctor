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
       legacy enabled path: PrivacyGatedLegacyAnalyzer -> APPROVAL_REQUIRED (no raw call)
  -> IntelligenceResultValidator
  -> versioned DTOs
```

- Core engine remains authoritative.
- Default analyzer is the deterministic mock; Docker/CI require no model or Internet.
- Spring AI 2.0.2 and the Ollama starter are confined to `dr-mays-ai`; the API imports AI contracts and the privacy-first engine, not provider internals.
- ORCH-01 fail-closes the legacy raw path: with `dr.mays.ai.enabled=true`, `/api/v1/intelligence/explain` returns `APPROVAL_REQUIRED` and makes no provider call. The Ollama adapter class remains isolated for a future approved integration. No connection at startup, no model download, no tools.

## Contracts

| Type | Purpose |
| --- | --- |
| `IntelligenceAnalyzer` | Analysis contract |
| `IntelligenceRequest` | Verified findings, edges and completeness |
| `IntelligenceResult` | Status, summary, explanations, recommendations, evidence refs, edges, uncertainties |
| `FindingExplanation` | Explanation tied to an existing finding |
| `Recommendation` | Suggested action, separate from findings |
| `EvidenceReference` | Existing evidence only |
| `ModelExecutionStatus` | `SUCCESS`, `DISABLED`, `APPROVAL_REQUIRED`, `UNAVAILABLE`, `FAILED`, `INVALID_RESPONSE` |
| `ModelMetadata` | Provider/model/version |

## Mock analyzer

The mock:
- uses only request findings/evidence/relationships;
- creates one explanation and one evidence reference per finding;
- preserves verified relationships exactly;
- marks PARTIAL discovery and empty findings as uncertainties;
- is deterministic and offline.

## Spring AI / Ollama adapter (retained, not wired to legacy raw path)

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

**NOT VERIFIED.** No real model call, provider transfer or local model inference has run. The ORCH-01 mock-only provider path is verified; real Ollama/OpenCode/BYOK execution requires separate approval and verification.

## Privacy-first orchestration (ORCH-01)

```text
POST /api/v1/expertise/previews -> PrivacyProcessor -> PrivacyGate preview
POST /api/v1/expertise/approvals -> single-use token
POST /api/v1/expertise/explain -> sealed preview -> ProviderRouter -> mock
  -> ExpertiseParser -> alias resolution -> IntelligenceResultValidator
  -> ExpertiseValidator -> local report
```

- Remote providers would receive only `SanitizedExpertiseRequest` (aliases, allowlisted fields); never raw domain objects.
- Preview never calls a provider; explain accepts only a preview ID and optional token.
- Approval binds exact payload hash + provider + model + expertise + policy and is single-use/TTL-limited.
- Mock provider is enabled; Ollama/OpenCode descriptors are disabled. OpenCode live execution is BLOCKED.
- No API key is accepted, read or logged. Credential references are logical and resolution is disabled.
- Pseudonymization is not anonymization; see `docs/security/AI_DATA_DISCLOSURE.md`.

New contracts live in `ai.privacy`, `ai.provider` and `ai.expertise`. The legacy `/api/v1/intelligence/explain` remains mock-capable but is fail-closed when external AI is enabled; external use must go through preview/approval.

## Privacy

No credentials, secrets, object contents or full inventories are sent. Data is minimized to findings/evidence/relationships. No raw prompts/responses are logged. See `docs/security/AI_MODEL_SECURITY.md`.
