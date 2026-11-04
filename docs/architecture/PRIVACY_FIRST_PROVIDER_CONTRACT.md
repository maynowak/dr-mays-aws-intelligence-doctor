# Privacy-First Provider Orchestration Contract

Status: frozen for ORCH-01 (2026-11-04)
Decision: ADR-0011
Scope: `dr-mays-ai`, `dr-mays-api`

## 1. Component map

```text
API resources
  -> existing DiagnosisService / DiagnosisEngine
  -> existing InfrastructureGraphBuilder
  -> IntelligenceRequest
  -> PrivacyProcessor (allowlist + aliases + validation)
  -> PrivacyGate (preview + approval, server-side, sealed payload)
  -> ProviderRouter
       -> MockModelProvider (enabled)
       -> Ollama descriptor (disabled)
       -> OpenCode CLI adapter (disabled; live BLOCKED)
  -> ExpertiseParser
  -> alias resolution
  -> IntelligenceResultValidator
  -> ExpertiseValidator
  -> ExpertiseReport / API DTOs
```

The diagnosis engine remains authoritative. The AI module never runs rules or creates findings/resources/relationships.

## 2. Module and package boundaries

`dr-mays-core` must not import Spring AI, provider, OpenCode, credential or privacy-adapter types.

New packages in `dr-mays-ai`:

- `ai.privacy` — classification, policy, allowlist, pseudonymization, sanitized payload, preview, approval gate.
- `ai.provider` — provider descriptors, capabilities, credential references, invocation contracts, registry/router, mock and disabled CLI adapter.
- `ai.expertise` — profiles, parser, report, validator, mock-only orchestration.

`dr-mays-api` imports only AI contracts/configuration and maps them to versioned DTOs.

## 3. Privacy contracts

| Type | Purpose |
| --- | --- |
| `PrivacyClassification` | `PUBLIC`, `INTERNAL`, `IDENTIFIER`, `QUASI_IDENTIFIER`, `SENSITIVE`, `SECRET` |
| `PrivacyField` | Explicit allowlist field names |
| `PrivacyPolicy` | Policy version, allowed fields, payload cap |
| `PrivacyViolation` | Safe code/field/message; never raw values |
| `PrivacyValidationResult` | Valid flag plus violations |
| `SanitizedFinding` | Alias-based finding view |
| `SanitizedRelationship` | Alias-based edge view |
| `SanitizedExpertiseRequest` | The only payload remote providers may receive |
| `AliasMapping` | Per-analysis HMAC alias map; local-only; never serialized |
| `PrivacyProcessingResult` | Sanitized payload, alias map, decisions, hash, validation |
| `PrivacyPreview` | Exact payload JSON/hash, provider/model/expertise, decisions, expiry |
| `PrivacyGate` | Server-side preview/approval/single-use execution state |
| `ApprovalToken` | Opaque, redacted, constant-time comparison |

### Payload allowlist

Allowed: rule ID/version, severity, resource type, coarse region class, resource alias, rule title/description after sanitization, evidence source/observed/expected after sanitization, relationship type/aliases/source/observed, completeness, sanitized uncertainties, counts.

Forbidden: account ID, raw resource ID, ARN, VPC/SG/host ID, tags, attributes, `Evidence.details`, emails, IPs, hostnames, secrets, URLs, free-form metadata, credentials.

### Pseudonymization

- HMAC-SHA-256 with a per-preview random salt.
- Alias format: `res_`, `acct_`, `sg_`, `vpc_`, `arn_` plus a truncated Base64URL digest.
- Same source identity → same alias within one preview; different previews are not linkable.
- Alias map is memory-only, non-serializable, never logged, cleared/released when the approval is consumed or expires.
- This is **pseudonymization**, not anonymization.

## 4. Preview, approval and execution

1. Preview builds the exact sanitized payload and canonical JSON/hash.
2. Preview response shows the payload, field decisions, validation, provider/model/expertise and expiry. It never calls a provider.
3. Approval is created server-side for a preview ID. It binds:
   `payloadHash + providerId + modelId + expertiseId + policyVersion`.
4. Execution by preview ID consumes the approval once. A changed provider/model/expertise, expired approval, unknown token or second use fails closed.
5. The sealed sanitized payload is sent; the client cannot substitute data.
6. Mock provider: approval is not required. Remote/CLI: approval is required.
7. TTL: preview and approval 5 minutes; bounded in-memory store; single instance only.

## 5. Provider contracts

| Type | Purpose |
| --- | --- |
| `ProviderKind` | `DETERMINISTIC_MOCK`, `LOCAL_MODEL`, `EXTERNAL_MODEL`, `CLI_AGENT` |
| `ProviderCapabilities` | JSON support, local-only flag, tools flag, payload/output caps |
| `ModelProviderDescriptor` | Provider identity, kind, capabilities, approval/credential requirements |
| `ProviderExecutionStatus` | `SUCCESS`, `DISABLED`, `UNAVAILABLE`, `FAILED`, `INVALID_RESPONSE`, `APPROVAL_REQUIRED`, `PRIVACY_REJECTED`, `NOT_ALLOWED`, `CREDENTIAL_UNAVAILABLE`, `TIMEOUT` |
| `ProviderCredentialReference` | Logical reference only; no secret |
| `ModelInvocationRequest` | Provider/model, sanitized payload, credential reference, timeout/output cap |
| `ModelInvocationResult` | Status, content, metadata, bounded lengths, safe error code |
| `ModelProvider` | Invocation contract |
| `ProviderRegistry` | Explicit provider catalog |
| `ProviderRouter` | Allowlist, credential precheck, no fallback |

### Provider defaults

- `mock`: enabled, local, no credential, no approval, no tools.
- `ollama`: disabled, local, no credential, approval required, no tools.
- `opencode`: disabled, CLI, approval + credential reference required, no tools declared; live execution BLOCKED.

### BYOK rules

- No API key in REST request, URL, query, log, Git, prompt or response.
- No automatic environment scan, no `AWS_*`/`OPENCODE_*` reuse, no auth-file read.
- Credential references are provider-scoped, validated by name/shape and resolved only after approval in a future phase.
- Current phase accepts only mock/no-credential execution; resolution is simulated/disabled.

## 6. OpenCode CLI adapter contract

Allowed in this phase:

- Disabled-by-default descriptor.
- Fixed argv list, no shell, no string concatenation.
- Explicit binary path, isolated HOME/config, sanitized environment.
- `/dev/null` or bounded stdin, bounded stdout/stderr, hard timeout.
- Output cap and structured failure handling.
- Simulated `CliProcessRunner` tests only.

Forbidden:

- Live process execution without OS-level isolation approval.
- Tool/MCP/plugin enablement.
- Project/home filesystem access.
- AWS/OpenCode credential inheritance.
- Session persistence with infrastructure data.
- Treating exit code 0 as sufficient if tools were rejected or output is truncated.

## 7. Status and failure model

Every failure returns an explicit status; deterministic diagnosis is never changed. Unknown provider/model/expertise, privacy validation failure, missing approval and credential issues prevent the call. Provider failure may be attempted once; no fallback or changed retry. Invalid model output becomes `INVALID_RESPONSE`. Provider timeout is `TIMEOUT`; connection failure is `UNAVAILABLE`.

## 8. REST boundary

Existing endpoints remain unchanged:

- `POST /api/v1/diagnoses`
- `POST /api/v1/dna/graphs`
- `POST /api/v1/intelligence/explain`

New additive mock-only endpoints:

- `POST /api/v1/expertise/previews`
- `POST /api/v1/expertise/approvals`
- `POST /api/v1/expertise/explain`

Preview never calls a provider. Explain accepts only `previewId` and optional approval token. API stays loopback-only and unauthenticated; no public exposure.

## 9. Privacy invariants

- No raw domain serialization to a provider.
- No unknown fields in the provider payload; only typed records.
- No secret or credential in any payload/log/report.
- No provider call without a valid allowlist entry and, for remote providers, valid approval.
- No approval reuse or changed-payload execution.
- No silent fallback to a different provider.
- No claim of complete anonymization.
- No real model call, API key or external transfer in ORCH-01.

## 10. Test plan

Privacy: account/SG/VPC/resource aliasing, emails, hostnames, IPs, credential patterns, injection metadata, unexpected/unsafe free text, deterministic aliases, invalid/tampered preview, changed payload, replay/expiry.

Provider: descriptors, capabilities, credential-reference validation, registry duplicates/unknowns, router allowlist/disabled/no-fallback, mock output, OpenCode argv/env/timeout/output cap/disabled execution.

Expertise: valid report, unknown finding/alias/evidence, invented relationship, duplicate explanation, empty/malformed response, partial discovery, provider failure/timeout, unknown metadata, deterministic mock.

API: preview without provider call, approval binding, explain by preview ID, invalid ID/token, existing endpoints unchanged.

Quality: at least 313 meaningful JUnit tests; Spotless; Checkstyle 0; JaCoCo 85/75; reactor; Docker regression; `git diff --check`; no secrets.

## 11. Integration order

1. Privacy engine.
2. Provider contracts and router.
3. Expertise/validation.
4. Approval-to-provider link.
5. REST/demo integration.
6. Security regression and documentation.
