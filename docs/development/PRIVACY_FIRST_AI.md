# Privacy-First AI

Status: local mock-only implementation; no external model transfer.

## Purpose

Dr. Mays explains deterministic findings, evidence and verified DNA relationships. The deterministic diagnosis engine remains authoritative. The privacy layer ensures that a provider, if later enabled, receives only a typed, minimized, pseudonymized payload.

## Pipeline

```text
POST /api/v1/expertise/previews
  -> existing DiagnosisService + InfrastructureGraphBuilder
  -> IntelligenceRequest
  -> PrivacyProcessor
       -> explicit field allowlist
       -> identifier/secret/free-text sanitization
       -> HMAC resource aliases
       -> payload validation
  -> PrivacyGate.register -> exact payload JSON/hash + preview
  -> POST /api/v1/expertise/approvals -> single-use token
  -> POST /api/v1/expertise/explain
       -> PrivacyGate.consume (sealed preview only)
       -> ProviderRouter -> MockModelProvider (enabled)
       -> parse -> alias resolution -> IntelligenceResultValidator -> ExpertiseValidator
       -> local report
```

Preview never invokes a provider. The expertise explain endpoint accepts only the preview ID and an optional approval token; clients cannot substitute a new resource payload. The legacy intelligence endpoint is mock-capable but fail-closed when external AI is enabled.

## Contracts

| Type | Role |
| --- | --- |
| `PrivacyPolicy` | Versioned field allowlist and payload cap |
| `SanitizedExpertiseRequest` | The only provider-facing request shape |
| `AliasMapping` | Per-preview HMAC pseudonyms and local reverse lookup |
| `PrivacyProcessingResult` | Sanitized payload, alias map, decisions, hash, validation |
| `PrivacyPreview` | Exact JSON, hash, provider/model/expertise, expiry, decisions |
| `PrivacyGate` | Server-side preview/approval/single-use consumption |
| `ExpertiseReport` | Facts, explanations, recommendations, assumptions, missing info, receipt |

## Allowed provider payload

- Rule ID/version, severity, coarse resource type, coarse region class.
- Resource alias only.
- Sanitized rule title/description.
- Evidence source and sanitized observed/expected values.
- Relationship type, aliases and sanitized evidence.
- Completeness, sanitized uncertainties.

## Never sent

- Account IDs, ARNs, raw resource IDs, SG/VPC/subnet/instance IDs.
- Tags, attributes, evidence details, full inventories.
- Emails, hostnames, IP addresses, URLs.
- API keys, tokens, credentials or secret material.
- Raw `IntelligenceRequest`, domain records or arbitrary maps.

## Pseudonymization, not anonymization

Resource aliases are HMAC-SHA-256 values generated with a per-preview random salt. The mapping stays local, in memory, and is never serialized. This is pseudonymization: the holder of the mapping can reverse it. It is not complete anonymization. Topology, counts, timing and rare combinations can still permit re-identification.

## Preview and approval

- Preview shows the exact sanitized payload and hash.
- Approval binds payload hash + provider ID + model ID + expertise ID + policy version.
- The payload is stored sealed server-side; execution cannot replace it.
- Approval is single-use and TTL-limited (5 minutes in the default gate).
- Missing, expired, mismatched, reused or privacy-invalid approval fails closed.
- Mock providers do not require approval; remote providers would.

## API

- `POST /api/v1/expertise/previews` — create preview, no provider call.
- `POST /api/v1/expertise/approvals` — approve an existing preview.
- `POST /api/v1/expertise/explain` — execute the stored preview.
- The existing `/api/v1/intelligence/explain` remains available for the mock. When external AI is enabled, it returns `APPROVAL_REQUIRED` without a raw provider call; external use must go through preview/approval.

All endpoints remain loopback-only, unauthenticated and unsuitable for public deployment.

## Validation and tests

- Raw identifiers/emails/hosts/IPs/secrets are rejected or aliased.
- Alias consistency is tested across findings and relationships.
- Preview tampering, wrong token, token reuse, expiry and invalid privacy state are tested.
- Unknown findings/evidence/relationships/aliases are rejected as `INVALID_RESPONSE`.
- Current suite: 411 tests; AI/API coverage gates pass.

## Limitations

- No real model or external provider has been exercised.
- No anonymity proof, GDPR/DPIA conclusion or production data-transfer approval.
- Approval state is in-memory and single-instance only.
- Free-text checks are pattern-based, not semantic proof.
