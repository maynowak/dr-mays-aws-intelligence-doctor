# DR-MAYS-PROVIDER-SECURITY-03-CI — Readiness

## Status

`LOCAL PRE-PUSH READINESS: GREEN`
`GITHUB CI: NOT VERIFIED (pre-push)`
`PULL REQUEST: NOT CREATED (pre-push)`
`MAIN MERGE: NOT AUTHORIZED`

## Git baseline

- Branch: `feature/dr-mays-provider-security-03`
- Pre-audit HEAD: `5b1fe6af04b7031021855af5ae63fb021883b866`
- Main: `3ec0e3b3d5063e8620e0b1f622c06a5280f2eabf`
- Feature commits before audit commit: 8
- Remote feature branch: absent before push
- Open PRs: none
- Working tree: clean before audit commit

## Quality evidence

| Gate | Result |
|---|---|
| Maven reactor | BUILD SUCCESS, 8/8 modules |
| Tests | 470 total, 0 failures, 0 errors, 0 skipped |
| Core | 86 tests, 99.36 % line / 95.65 % branch |
| API | 102 tests, 97.83 % / 79.59 % |
| AWS | 64 tests, 95.25 % / 83.93 % |
| AI | 218 tests, 93.70 % / 76.89 % |
| Spotless | PASS |
| Checkstyle | 0 violations |
| JaCoCo | PASS |
| Docker demo | PASS |
| Targeted authorization/replay tests | PASS |

Evidence: `/tmp/opencode/ps03ci-verify.log`, `/tmp/opencode/ps03ci-docker.log`.

## Public repository security gate

- No real credentials, private keys, tokens, `.env` values, `.aws`/`.ssh` material found in tracked files or added history.
- `.env.example` contains empty placeholders only.
- Test-only synthetic secret-like strings are used to test privacy redaction; they are not live credentials.
- No real account IDs beyond documented synthetic values (`000000000000`, `111111111111`, `123456789012`) and no personal data added by this branch.
- Workflow uses `contents: read`, SHA-pinned actions, `persist-credentials: false`, no secrets, no AWS/model/deployment steps.

## Trust-boundary regression

- `ApprovedInvocation` and `ProviderInvocationAuthorization`: final classes, package-private constructors, redacted `toString`.
- Only `PrivacyGate` mints authorization; router has one `route` entry point and one `ModelProvider.invoke` caller.
- Credential binding is checked in the request/router path; rotation/version/reference/fingerprint mismatches fail before invoke.
- Replay nonce map is TTL-pruned; capacity fails closed with `AUTHORIZATION_STORE_FULL`; synchronized consumption gives at most one success.
- Mock-only enablement, exact `MockModelProvider` exemption, model allowlist, payload integrity and privacy-v2 remain intact.
- FILE/KEYRING and credential resolution remain disabled; OpenCode has no production runner.

## Known residual risks (PR blockers for live providers)

- In-memory, single-instance approval/replay state; restart clears it.
- Wall-clock TTL; no monotonic skew guard.
- No authenticated approver identity.
- Credential fingerprint is not backed by a real authority.
- Provider invocation timeout not enforced; router monitor held across invoke.
- OpenCode sandbox controls are declarative; live execution blocked.
- Pattern-based pseudonymization, not guaranteed anonymity.
- Public REST is loopback-only and unauthenticated.
- In-process code can compose permissive components; not a REST-reachable boundary.

## Blocked capabilities

Real credentials/BYOK resolution, real model/provider inference, OpenCode live execution, AWS live discovery, deployment, public REST exposure, multi-instance approval, production authentication.

## Decision

Proceed with controlled feature-branch push, exact-SHA GitHub CI verification, PR creation and PR CI verification. Do not merge or deploy.

Actual CI run IDs, PR URL and conclusions are recorded in the PR body/comment after the push; they are not claimed in this pre-push commit.
