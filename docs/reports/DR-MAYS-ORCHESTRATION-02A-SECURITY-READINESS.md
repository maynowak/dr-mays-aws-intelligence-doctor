# DR-MAYS-ORCHESTRATION-02A — Security & Integration Readiness

- Date: 2026-11-04
- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- Baseline HEAD: `87e73adcbeb8bcba25d8dca3eba7772d0a12e125`
- Implementation HEAD: `6cbc728`
- Status: **READY FOR CI REVIEW (local)**
- CI: **NOT VERIFIED** — no GitHub run, no push
- Live providers/OpenCode/BYOK: **BLOCKED / DISABLED**
- Real model calls / external transfers / AWS calls: **0**

## 1. Decision

02A found and closed concrete latent payload-boundary leaks before any real provider could be enabled. The local security and quality gates pass. The branch is ready for an authorized CI/PR review, not for merge, deployment, real provider activation or public exposure.

## 2. Independent review results

Three read-only reviews were performed:

| Review | Initial result | After fixes |
| --- | --- | --- |
| REST entry points & approval | YELLOW, no bypass; relationship-evidence residual | GREEN (mock path); evidence projection fixed |
| Privacy validation | ORANGE, latent relationship/ARN/secret/URL/hostname defects | GREEN (mock path); defects fixed, residuals documented |
| Credential & CLI boundaries | YELLOW, no active exposure; latent router/CLI/doc gaps | GREEN (mock path); docs/credential refs hardened |

Reports:

- `docs/reports/DR-MAYS-ORCHESTRATION-02A-REST-APPROVAL-REVIEW.md`
- `docs/reports/DR-MAYS-ORCHESTRATION-02A-PRIVACY-REVIEW.md`
- `docs/reports/DR-MAYS-ORCHESTRATION-02A-CREDENTIAL-CLI-REVIEW.md`

## 3. Findings fixed

1. **Relationship evidence bypass (high latent):** arbitrary values in `vpc.id` and `ingress.referenced.securityGroupIds` are now serialized as typed aliases; safe literals are preserved. Schema keys are preserved (no `[host].securityGroupIds` corruption).
2. **ARN partitions:** `arn:[a-z0-9-]+:` now covers `aws-iso`, `aws-iso-b`, `aws-eusc`, etc.
3. **Expanded secret detection:** AWS secret-access-key assignments, JWT, SSH keys, GitHub/GitLab/Google/Stripe patterns and generic password/token assignments are rejected; relationship evidence is scanned too.
4. **URLs:** HTTP(S) URLs are redacted/rejected.
5. **Identifier coverage:** additional AWS ID prefixes (`igw`, `eni`, `rtb`, `ami`, `snap`, `nat`, `tgw`, `vpce`, `dbi`, `fs`, `lt`, `eip`, etc.), underscore-preceded IPv4, no-TLD email and IPv6 zone forms are covered.
6. **Legacy path:** property-enabled `/intelligence/explain` is covered by a Spring integration test and returns `APPROVAL_REQUIRED` without a provider call.
7. **Approval:** concurrent consumption test verifies exactly one execution; single-use/replay/TTL/cross-preview binding remain intact.
8. **Unknown JSON:** API test verifies unknown request fields are rejected.
9. **Credential references:** FILE/KEYRING names must be simple logical names, NONE must be literal, and `toString` redacts the reference.
10. **Legacy raw adapter classes:** `OllamaIntelligenceAnalyzer`, `PromptBuilder` and `SpringAiChatCompletion` are package-private and not production-wired.
11. **Documentation:** stale Ollama opt-in, status enum, legacy scope and test counts corrected.

## 4. Verification evidence

- `./mvnw -B clean verify`: **BUILD SUCCESS**, 8/8 modules.
- Tests: **409**, 0 failures, 0 errors, 0 skipped.
  - Core 86
  - API 99
  - AWS 64
  - AI 160
- Coverage:
  - Core **99.36 % line / 95.65 % branch**
  - API **97.83 % / 79.59 %**
  - AWS **95.25 % / 83.93 %**
  - AI **94.47 % / 79.09 %**
- Spotless PASS; Checkstyle 0 violations; JaCoCo PASS; Maven reactor PASS.
- `./scripts/docker-demo.sh`: **PASS**
  - Health/rules/four fixtures/determinism/DNA/mock intelligence/privacy preview-approval-explain/invalid input
  - UID 10001, read-only rootfs, CapDrop ALL, no-new-privileges, loopback publish, clean stop
- Logs: `/tmp/opencode/orch02a-final-verify.log`, `/tmp/opencode/orch02a-final-docker.log`

## 5. Privacy and approval status

- Provider payload remains `SanitizedExpertiseRequest` only.
- Preview does not call a provider; it displays exact sanitized JSON/hash and decisions.
- Approval binds exact hash + provider + model + expertise + policy, single-use, TTL-limited.
- Wrong token, cross-preview token, replay, expiry, invalid privacy state, unknown preview/provider and oversized payload fail closed.
- HMAC aliases are per-preview and memory-only; no anonymity claim.
- Local preview/report still resolves identifiers for the local operator; loopback API remains unauthenticated.

## 6. Credential / CLI status

- No API key acceptance, resolution or logging.
- No OpenCode auth-file read, no AWS credential access, no environment secret scan.
- No production OpenCode process runner; live flag false; no shell/argv injection.
- Provider router/engine remains mock-only; no external provider reachable.
- No tool calling enabled.
- Router-level `requiresApproval` enforcement remains a precondition before any non-mock allowlist; current engine gate and mock-only registry make it unreachable.

## 7. Residual risks / preconditions

1. Single-label/underscore hostnames and some uncommon identifier formats remain pattern-limited.
2. `PrivacyPolicy.defaultPolicy()` allows all supported fields; operator-side minimization is future work.
3. Local approval is a two-step UX/audit gate, not authenticated human authorization; loopback only.
4. In-memory approval store is single-instance and restart-sensitive; global caps can be exhausted locally.
5. Router-level approval must be enforced before enabling a non-mock provider.
6. Model allowlisting, credential fingerprinting/binding and real secret backend need separate approval.
7. OpenCode live execution requires OS-level isolation, deny-all tools, egress control, session cleanup and a canary.
8. No complete anonymization proof; topology/counts/timing can permit re-identification.
9. CI and real provider E2E remain NOT VERIFIED.

## 8. Git and audit

- Baseline: `87e73ad`
- Implementation commit: `6cbc728` (`fix(ai): close ORCH-02A privacy bypasses`)
- Final audit commit follows this report.
- No push, merge, deployment, AWS call, real model call or credential use.
- Working tree clean at implementation commit.

## 9. Recommendation

**READY FOR CI REVIEW.** Authorize push/CI/PR separately if desired. Keep real providers, BYOK resolution, OpenCode live execution and public deployment disabled. Do not merge to `main` without explicit approval.
