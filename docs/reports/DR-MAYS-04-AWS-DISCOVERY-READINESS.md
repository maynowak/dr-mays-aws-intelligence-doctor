# DR-MAYS-04 — AWS Infrastructure Discovery Foundation — Readiness Report

- Date: 2026-11-03
- Repository: `/home/dci-student/projects/dr-mays-aws-intelligence-doctor`
- Branch: `feature/dr-mays-aws-discovery-04`
- Base commit: `21e5b4327ecf10002ec724eba95e2a902a8f3206`
- Local status: **GREEN**
- Live AWS scans: **NOT EXECUTED** — out of scope
- CI status: **NOT VERIFIED** at the time of this report; no push was made during implementation.

## 1. Architecture

`dr-mays-aws` now owns all AWS SDK types. The core and API remain free of AWS dependencies.

```text
DiscoveryScope / DiscoveryRequest
        -> AwsDiscoveryAdapter
             -> STS identity verification (fail closed)
             -> EC2 security-group discovery -> AwsResourceMapper -> core InfrastructureResource
             -> S3 metadata discovery        -> AwsResourceMapper -> core InfrastructureResource
        -> DiscoveryResult (COMPLETE / PARTIAL / FAILED, sorted resources + safe failures)
```

See `docs/development/AWS_DISCOVERY.md` and ADR-0008.

## 2. Implemented adapters and contracts

- `InfrastructureDiscovery`, `DiscoveryRequest`, `DiscoveryResult`, `DiscoveryFailure`
- `DiscoveryScope` with profile/account/region/service/resource-limit validation
- `AwsClientFactory` with explicit named profile, region, API/attempt timeouts and bounded retries
- `AwsIdentityVerifier` / `StsAwsIdentityVerifier`
- `AwsResourceMapper`
- `Ec2SecurityGroupDiscovery`
- `S3MetadataDiscovery`
- `AwsDiscoveryAdapter`
- `AwsFailureMapper`

## 3. Supported AWS services

| Service | Phase | Calls |
| --- | --- | --- |
| EC2 | implemented (mocked) | `DescribeSecurityGroups` |
| S3 | implemented (mocked) | `ListBuckets`, `GetBucketLocation`, `GetBucketTagging`, `GetBucketVersioning`, `GetPublicAccessBlock` |
| STS | implemented (mocked) | `GetCallerIdentity` |

No real request was made. No AWS credentials were resolved or required.

## 4. Security and identity

- Explicit scope requires non-default profile, 12-digit expected account, allowed regions, allowed services and max resources.
- STS account mismatch fails closed before any discovery call.
- Missing identity, invalid scope, SDK errors or unexpected verification errors return `FAILED`.
- No default credentials provider, no Spring bean, no startup client creation.
- S3 object data is never read; no write/mutate calls exist.
- IAM concept: `docs/security/AWS_SCANNER_IAM.md`.

## 5. Resilience and status semantics

- `COMPLETE`: no failures.
- `PARTIAL`: usable resources plus failures (e.g., one region denied, resource limit).
- `FAILED`: no usable resources.
- AccessDenied, throttling, timeout, endpoint/network, service and unknown failures have stable codes and safe messages.
- Pagination is bounded; repeated tokens fail with `MALFORMED_RESPONSE`.
- Resource limits and duplicate identities are explicit failures.
- Missing S3 tag set is empty tags; missing public-access-block is `configured=false`; denied/unknown required metadata omits the bucket and records a failure.

## 6. Tests and coverage

| Module | Tests | Line | Branch |
| --- | ---: | ---: | ---: |
| `dr-mays-core` | 63 | 100.00 % | 98.08 % |
| `dr-mays-api` | 52 | 97.83 % | 100.00 % |
| `dr-mays-aws` | 64 | 95.35 % | 84.55 % |
| **Total** | **179** | — | — |

- 64 new AWS tests; requirement was at least 30.
- All 115 prior tests still pass.
- Mocked EC2/S3/STS clients; no real AWS calls or credentials.
- Spotless, Checkstyle, JaCoCo and full reactor `verify` pass.

## 7. Docker regression

`./scripts/docker-demo.sh` rebuilt the image, started the container, passed health/rules/four fixtures/determinism/invalid-input checks, verified non-root/read-only/cap-drop settings and stopped the container.

## 8. Git commits

| Step | Commit | Message |
| --- | --- | --- |
| 04A | `96a3141` | `feat: define AWS discovery contracts` |
| 04B | `f93cd5e` | `feat: add explicit AWS client and failure mapping` |
| 04C | `c5a6419` | `feat: add EC2 security group discovery` |
| 04D | `55cad14` | `feat: add S3 metadata discovery` |
| 04E | `d76a160` | `feat: add fail-closed identity and discovery composition` |
| 04H | current HEAD | Documentation, IAM concept, readiness and audit close-out |

No push, merge, deployment or live AWS operation was performed during implementation.

## 9. Open questions and risks

- No live scan was performed; SDK request/response compatibility is proven only by mocks.
- S3 bucket listing is account-global; region filtering relies on `GetBucketLocation`.
- S3 permissions are often resource-scoped; the illustrative IAM policy needs tightening to approved bucket ARNs.
- The AWS module is not wired into Spring/API/Docker and has no scan endpoint.
- A future live scan requires explicit profile/account/region/service/resource-cap approval and a security review.
- CI has no run for this branch until an authorized push occurs.

## 10. Recommended next phase

Prepare a separately approved **live verification** with a dedicated read-only profile and account. Do not add a public scan endpoint until authentication, authorization, audit logging and strict scope validation are designed.

## 11. Recovery point

- Branch: `feature/dr-mays-aws-discovery-04`
- Base: `21e5b4327ecf10002ec724eba95e2a902a8f3206`
- Last implementation commit: `d76a160`
- Final commit: current HEAD (`git log -1`)
- Resume with `git status` and `./mvnw -B clean verify`.
