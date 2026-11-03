# ADR-0008: AWS Discovery Boundary

- Status: Accepted
- Date: 2026-11-03
- Scope: `dr-mays-aws`

## Context

Dr. Mays needs AWS discovery that feeds the existing domain without polluting it with SDK types. DR-MAYS-04 is simulated-only: no real AWS calls, credentials or deployments. A later live scan requires a separate explicit approval for profile, account, regions and services.

## Decision

Use technology-neutral discovery contracts in `dr-mays-aws` and keep all AWS SDK types inside that module.

### Contracts

- `DiscoveryScope` is the approved boundary: explicit non-default profile, expected 12-digit account ID, allowed regions, allowed services and a hard resource limit.
- `DiscoveryRequest` wraps a scope.
- `DiscoveryResult` contains sorted resources, structured failures and a derived status: `COMPLETE`, `PARTIAL` or `FAILED`.
- `DiscoveryFailure` records service, failure code and a safe message; no raw SDK payloads or secrets.
- `InfrastructureDiscovery` is the service-level contract. `AwsDiscoveryAdapter` composes it with fail-closed identity verification.

### AWS adapters

- `AwsClientFactory` creates EC2/S3/STS clients only on demand with an explicit named profile and bounded retry/timeout settings. It never uses `DefaultCredentialsProvider` and is not a Spring bean.
- `AwsIdentityVerifier`/`StsAwsIdentityVerifier` checks STS `GetCallerIdentity` against the expected account before any discovery call. Mismatch, missing profile/region/account or SDK failure stops the scan.
- `AwsResourceMapper` maps EC2 security groups and S3 bucket metadata into `InfrastructureResource`.
- EC2 mapping emits `ingress.public=true` when any IPv4 `0.0.0.0/0` or IPv6 `::/0` rule exists, plus deterministic summaries of rule counts, protocols/ports and group references. It reuses `NET-PUBLIC-001`; no parallel rule is added.
- S3 mapping emits versioning and public-access-block metadata only when calls succeed. Tags are treated as empty only for the explicit “no tag set” response. AccessDenied/timeout on required metadata omits the bucket and records a failure; unknown data is never treated as healthy.
- S3 listing is account-global; bucket region is resolved and filtered by the allowed regions. No object contents, ACL/policy mutation, versioning changes or secret reads are performed.

### Resilience

- Results distinguish COMPLETE (no failures), PARTIAL (failures and resources) and FAILED (failures and no resources).
- AccessDenied, throttling, timeout, endpoint/network and unknown failures are mapped to stable codes.
- Pagination is bounded by the resource limit and a page guard. Resource limits produce a PARTIAL result with a `RESOURCE_LIMIT` failure.
- Duplicate identities and out-of-scope resources are handled explicitly, not silently dropped.
- SDK clients use a small bounded retry policy and API-call timeout; the discovery layer does not retry unboundedly.

## Consequences

- Core stays AWS-free.
- Offline tests use mocked SDK clients; no credentials or network are required.
- The first live scan cannot accidentally use the `default` profile or an unexpected account.
- Public S3 metadata may be incomplete; such resources are omitted rather than misclassified.
- Live IAM permissions remain a separate, least-privilege deliverable for the first approved scan.
