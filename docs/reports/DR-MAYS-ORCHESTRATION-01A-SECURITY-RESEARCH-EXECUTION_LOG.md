# ORCH-01A — Agent C — Privacy & Security Research

## 1. Current status

`COMPLETE — READ-ONLY SECURITY RESEARCH.` No data leak observed; provider-enable path remains ORANGE until privacy controls exist.

## 2. Audit date/time

- Date: 2026-11-04
- Report: 2026-11-04T08:05:34+01:00
- Agent session: `ses_e5aa1afa4ffeGpRAdmcx9ltAeX`

## 3. Git branch/HEAD observed

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD: `36ef4e5a105d8a11f10bf202c7cc60e978540c3f`

## 4. Audit scope

Threat model for infrastructure metadata, account/resource identities, IPs, hostnames, emails, tags, app names, API keys, providers, prompt injection, external transmission, CLI execution and session/log storage. Distinguish minimization, masking, pseudonymization and anonymization.

## 5. Completed work steps

- Read governance, AI/security docs, ADR-0010 and AI code.
- Traced API → diagnosis → DNA → IntelligenceRequest → prompt → provider.
- Traced raw identifiers and evidence into prompt fields.
- Reviewed validation, logging, config, Docker and module boundaries.
- Verified Jackson unknown-property default with a local probe.
- Produced invariants, allowlist, approval design, alias design, secret references, fail-closed matrix and test matrix.

## 6. Actual findings

- Current prompt sends raw account IDs, region, resource IDs and VPC/SG IDs via canonical IDs and evidence strings.
- No preview/approval/hash/provider/model/credential binding exists.
- Prompt injection defense is prose-only; newline/control-character injection is possible.
- Free-text summary/explanations/recommendations are not fully validated; recommendations are unchecked.
- Data minimization is a property of current rules, not an enforced boundary; `Evidence.details` is currently excluded but future rules could leak.
- Local Ollama endpoint/model is configurable without an allowlist; no response-size cap.
- Current runtime has no process execution, no prompt logging, loopback API and safe Docker defaults.
- Pseudonymization is reversible and remains personal data; complete anonymization is not claimed.

## 7. Evidence/file references

- `dr-mays-ai/.../PromptBuilder.java`, `IntelligenceResultValidator.java`, `OllamaIntelligenceAnalyzer.java`, `IntelligenceConfiguration.java`.
- `dr-mays-core/.../ResourceIdentity.java`, `Evidence.java`, `InfrastructureGraphBuilder.java`.
- `dr-mays-api/.../IntelligenceService.java`, `ResourceIdentityRequest.java`, `application.yml`, `ApiExceptionHandler.java`.
- `docs/security/AI_MODEL_SECURITY.md`, `docs/development/AI_INTELLIGENCE.md`, ADR-0010.
- `/tmp/opencode/jackson-probe` — Jackson unknown-property default = false.

## 8. Classification

`ORANGE` for provider-enabled design; `YELLOW` for current default runtime; Docker/logging mostly GREEN; CLI GRAY (not implemented).

## 9. Terraform checks and results

N/A — none executed; no Terraform files exist.

## 10. Git status

Read-only; only the orchestrator's untracked log.

## 11. Files changed

None in the repository. Local probe under `/tmp/opencode/` only.

## 12. Explicit no-change confirmation

No repo file changed, no model/provider/AWS/network call, no credential file read, no secret printed, no Terraform/CDK/Maven run.

## 13. Open questions

- Remote provider scope.
- Approval location/state (server-side mandatory).
- Region treatment (drop/coarse/alias).
- Rule title/description retention.
- Credential backend.
- OpenCode sandbox.
- Model digest authority.
- Retention policy and multi-user host threat.
- GDPR/DPIA review.

## 14. Risks

- Identifier disclosure to providers.
- Approval replay/swap/bypass.
- Prompt injection via identifiers/attributes.
- Secret ingestion from future rules/tags.
- Endpoint redirection/confused deputy.
- Log/session leakage.
- Response-size DoS and residual re-identification.

## 15. Recommended next actions

1. Implement allowlist payload projection, HMAC pseudonymization, secret/PII scanner.
2. Add server-side preview/approval with payload hash + provider/model/credential binding, TTL, single-use.
3. Enforce endpoint allowlist and response bounds.
4. Add free-text/recommendation validation and injection-safe serialization.
5. Keep AI disabled by default; no provider call in tests.
6. Document residual risk; never claim anonymity.

## 16. Current resume point

Security research complete at `36ef4e5`. Start implementation at the `IntelligenceService`/privacy boundary; no code changed.
