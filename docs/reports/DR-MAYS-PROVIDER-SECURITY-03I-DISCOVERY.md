# DR-MAYS-PROVIDER-SECURITY-03I — Discovery

## 1. Status

`COMPLETE — discovery synthesized; implementation complete.` No live external operation.

## 2. Date/time

2026-11-04T15:49–17:30+01:00.

## 3. Branch/HEAD

`feature/dr-mays-provider-security-03`; baseline `57351fe`, implementation `cc6fe1c`.

## 4. Scope

Credential binding, authorization provenance, replay/TTL/concurrency.

## 5. Steps

Inspected gate/router/engine/request/authorization contracts, 03H reviews, tests and local build/Docker evidence; two independent 03I reviewers then rechecked the implementation.

## 6. Actual findings

- Credential binding needed to flow from preview through gate/auth into request/router; previously contract-only.
- `ApprovedInvocation` was a public record and could be forged in-process; now sealed final class with package-private constructor.
- Router FIFO nonce eviction could allow replay; replaced with TTL map and fail-closed capacity.
- Gate approval policy is server-owned; caller boolean removed.
- Enablement is explicit; registration is not enablement.
- FILE/KEYRING disabled; no real resolver.

## 7. Evidence

`PrivacyGate`, `ProviderInvocationAuthorization`, `ApprovedInvocation`, `PrivacyPreview`, `ProviderRouter`, `ModelInvocationRequest`, `ProviderEnablement`, `ProviderCredentialBinding`, tests; `ps03i-final-verify.log`, `ps03i-final-docker.log`.

## 8. Classification

`GREEN (local)` for mock-only path; live credential/provider remains blocked.

## 9. Terraform

N/A.

## 10. Git status

Implementation committed `cc6fe1c`; docs audit commit follows.

## 11. Files changed

Privacy/provider authorization, credential binding, replay state, tests, docs.

## 12. No-change confirmation

No real model/provider/AWS/credential/network operation.

## 13. Open questions

Persistent/multi-instance replay state, real resolver backend, authenticated approver identity.

## 14. Risks

In-memory restart window, local capacity DoS, pattern-based privacy, declarative sandbox.

## 15. Next actions

Keep mock-only; wire a trusted resolver only under separate approval; CI/PR separately.

## 16. Resume point

Final audit commit.
