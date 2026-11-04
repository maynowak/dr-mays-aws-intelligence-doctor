# ORCH-02A-03 — Privacy Validation Review

## 1. Current status

`COMPLETE — READ-ONLY PRIVACY REVIEW.` Initial classification `ORANGE` due latent payload-boundary leaks; demonstrated issues were fixed in 02A.

## 2. Audit date/time

- Review: 2026-11-04T11:20–11:26+01:00
- Agent session: `ses_e59ed8dd5fferVqknBPa36b3mV`

## 3. Git branch/HEAD observed

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- HEAD: `87e73ad` (match expected)

## 4. Scope

Privacy package, sanitized payload, API DTO validation, evidence/tag/attribute handling, HMAC aliases, free-text and re-identification risks.

## 5. Completed steps

- Traced every payload field from `IntelligenceRequest` to `SanitizedExpertiseRequest`.
- Inspected both built-in rules and `InfrastructureGraphBuilder` evidence paths.
- Executed read-only regex simulations for IDs/ARNs/secrets/hosts/URLs.
- Checked API unknown-field behavior, error handling, alias isolation and docs/OpenAPI claims.

## 6. Actual findings

- HIGH latent: arbitrary `vpc.id` / `ingress.referenced.securityGroupIds` values (e.g. `prod-vpc`, `igw-...`) survived sanitization and were serialized into relationship `observed`. **Fixed** by typed key/value evidence projection: known keys are preserved, values are aliased unless safe literals.
- MEDIUM: ARN partitions outside `aws`/`aws-us-gov`/`aws-cn` were not matched. **Fixed** with `arn:[a-z0-9-]+:`.
- HIGH latent: secret patterns were too narrow. **Fixed** by adding AWS secret key assignments, JWT, SSH keys, GitHub/GitLab/Google/Stripe patterns, generic password/token assignments; relationship evidence is now also scanned.
- MEDIUM: hostname pattern rewrote `ingress.referenced.securityGroupIds` to `[host].securityGroupIds`. **Fixed** with possessive hostname matching and key-preserving evidence serialization.
- MEDIUM: URLs were not redacted despite docs. **Fixed** with URL detection/rejection and `[url]` replacement.
- Residual: single-label/underscore hostnames, no-TLD emails, IPv6 zone fragments, policy-default breadth, `OllamaIntelligenceAnalyzer`/`PromptBuilder` still present, and local preview/report re-identification. Some are documented/attenuated; no complete anonymity claim.
- PASS: flat allowlisted payload, per-preview HMAC alias isolation, no tags/attributes/details serialized, approval binding, mock-only routing, static error messages.

## 7. Evidence/file references

- `dr-mays-ai/.../privacy/PrivacyProcessor.java`, `PrivacyValidator.java`, `SanitizedExpertiseRequest.java`, `AliasMapping.java`
- `dr-mays-core/.../dna/InfrastructureGraphBuilder.java`
- `dr-mays-ai/.../expertise/ExpertiseValidator.java`, `PrivacyFirstExpertiseEngine.java`
- `dr-mays-api/.../Expertise*`, `application.yml`
- Tests: `PrivacyProcessorTest`, `PrivacyValidatorTest`, `ExpertiseValidatorTest`

## 8. Classification

Initial `ORANGE`; after fixes and tests, **GREEN (local)** for the current mock-only path, with documented residual pattern/re-identification risks.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

Read-only; only the orchestrator's 02A log was untracked at review time.

## 11. Files changed

None by reviewer.

## 12. Explicit no-change confirmation

No file/model/API/AWS/credential call by reviewer; regex simulation only.

## 13. Open questions

- Whether policy should be operator-configurable/minimal by default.
- Whether single-label hostnames/no-TLD emails must be blocked before remote enablement.
- Whether legacy raw AI classes should be removed entirely.

## 14. Risks

- Future rules can introduce new free-text evidence shapes; deny-by-pattern remains limited.
- Topology/counts/timing permit correlation even with aliases.
- Documentation must continue to avoid absolute anonymity claims.

## 15. Recommended next actions

1. Keep the new evidence projection and expanded patterns.
2. Add policy configuration/minimal defaults before remote use.
3. Keep real providers disabled until residual risks are accepted.

## 16. Current resume point

Privacy review complete; fixes committed by orchestrator; no external transfer.
