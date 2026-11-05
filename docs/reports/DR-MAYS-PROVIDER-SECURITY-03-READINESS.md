# DR-MAYS-PROVIDER-SECURITY-03 — Readiness

- Date: 2026-11-04
- Branch: `feature/dr-mays-provider-security-03`
- Baseline/main: `3ec0e3b3d5063e8620e0b1f622c06a5280f2eabf`
- Status: **GREEN — LOCAL SECURITY HARDENING COMPLETE**
- GitHub CI: **NOT VERIFIED** (no push authorized)
- Real model/provider/AWS calls: **0**
- Credential resolution / OpenCode live: **DISABLED / BLOCKED**

## 1. Result

The provider boundary is now enforced in the router:

- `PrivacyGate.consume` mints a package-private `ProviderInvocationAuthorization` after approval/privacy success.
- Only `ProviderRouter.route(ModelInvocationRequest, ProviderInvocationAuthorization)` exists; the unauthenticated route is gone.
- Authorization binds exact canonical payload hash, provider, model, expertise, policy version and expiry; router enforces single-use and payload integrity.
- Mock exemption is exact-class based; registry reserves ID `mock`.
- Exact model allowlist (`mock` / `deterministic-v1`).
- Privacy default is minimal/versioned `privacy-v2` (8 fields), relationships omitted, excluded fields validated.
- Credential and OpenCode policies are disabled/blocked; no resolver or process runner is active.

## 2. Test and quality evidence

- `./mvnw -B clean verify`: **BUILD SUCCESS**, 8/8 modules.
- **455 tests**, 0 failures, 0 errors, 0 skipped.
  - Core 86, API 102, AWS 64, AI 203.
- Coverage:
  - Core **99.36 % line / 95.65 % branch**
  - API **97.83 % / 79.59 %**
  - AWS **95.25 % / 83.93 %**
  - AI **94.56 % / 79.13 %**
- Spotless PASS; Checkstyle 0; JaCoCo PASS.
- Docker demo: **PASS** (privacy preview/approval/explain, mock intelligence, DNA, invalid input, container security, clean stop).
- Logs: `/tmp/opencode/ps03-final-verify.log`, `/tmp/opencode/ps03-final-docker.log`.

## 3. Security review

Two independent read-only reviews:

- `DR-MAYS-PROVIDER-SECURITY-03-REVIEW-A-AUTHORIZATION.md`
- `DR-MAYS-PROVIDER-SECURITY-03-REVIEW-B-CREDENTIAL-CLI.md`

No HIGH/CRITICAL finding. Review A found a MEDIUM payload-hash trust gap; fixed by `PayloadIntegrity` canonical JSON/SHA-256 verification and a substitution test. Review B found an incomplete future FILE/KEYRING denylist; reserved names were partially hardened and the remaining gap is documented for a future resolver.

## 4. Residual risks / preconditions

1. `PrivacyGate.register` still accepts a boolean approval requirement; current engine derives it from the exact mock policy, but an in-process caller could construct a different gate call. REST cannot reach it; remote enablement should move this policy into the gate.
2. FILE/KEYRING denylists should be expanded before any real resolver (`.ssh`, `.aws`, `.netrc`, `id_rsa`, `oauth.json`, etc.).
3. Credential kind/reference is not yet bound into the authorization tuple; BYOK requires this.
4. Registry membership is currently equivalent to enablement; future remote providers need an explicit separate enable set.
5. OpenCode live execution is operationally disabled, not structurally impossible: public constructors/runner interface exist, sandbox controls are declarative, timeout/output caps are not enforced by a runner.
6. Privacy remains pattern-based pseudonymization, not anonymity; topology/counts/timing can permit re-identification.
7. Approval is a local unauthenticated loopback trust gate, in-memory/single-instance.
8. Real provider/model/BYOK/OpenCode/AWS/CI remain unverified.

## 5. Git

- Contract commit: `9b5b6fe`
- Implementation commit: `d8cbb7d`
- Hash-hardening commit: `2933aee`
- Final docs commit follows.
- Working tree clean after commit; no push/merge/deployment.

## 6. Decision

**GREEN — LOCAL SECURITY HARDENING COMPLETE.** No live provider is authorized. Production, real model inference, BYOK, OpenCode live, AWS live and public API remain **BLOCKED** pending separate approvals and E2E evidence.
