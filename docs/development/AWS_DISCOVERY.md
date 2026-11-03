# AWS Discovery Foundation

DR-MAYS-04 adds a simulated, read-only AWS discovery layer in `dr-mays-aws`. It is **not wired into Spring Boot, the REST API or the Docker demo**, and it performs no live AWS calls.

## Safety model

- No real AWS calls in DR-MAYS-04.
- No credentials required for build or tests.
- No default profile; `default` is rejected by `DiscoveryScope` and `AwsClientConfig`.
- Explicit scope: profile, 12-digit expected account, allowed regions, allowed services, max resources.
- STS identity verification happens before any EC2/S3 call. Mismatch, missing identity, SDK failure or invalid scope fails closed.
- SDK clients are created only on demand by `AwsClientFactory`, never as Spring beans.
- No S3 object contents, ACL/policy mutation, versioning change, secret read or IAM change.

## Packages

| Package | Purpose |
| --- | --- |
| `aws.discovery` | Scope, request, result, failures, status, adapter |
| `aws.client` | Explicit profile/region client factory with bounded timeout/retries |
| `aws.identity` | STS caller verification, fail-closed exception |
| `aws.mapping` | EC2/S3 SDK models to core resources |
| `aws.ec2` | Read-only security-group discovery |
| `aws.s3` | Read-only bucket metadata discovery |
| `aws.failure` | SDK exception to safe failure code mapping |

## Contracts

```text
DiscoveryScope(profile, expectedAccountId, regions, services, maxResources)
DiscoveryRequest(scope)
DiscoveryResult(resources, failures, status)
  COMPLETE -> no failures
  PARTIAL  -> resources + failures
  FAILED   -> no usable resources
```

`AwsDiscoveryAdapter` verifies identity first, then runs registered EC2/S3 discoveries. It sorts services, enforces the global resource limit and deduplicates identities.

## EC2 mapping

Security groups become core resources with:

- identity type `network`, provider `aws`, synthetic/test account and region
- `owner` tag when present (reused by `TAG-OWNER-001`)
- `ingress.public=true` when any IPv4 `0.0.0.0/0` or IPv6 `::/0` rule exists (reused by `NET-PUBLIC-001`)
- deterministic counts/protocols/port ranges/group-reference counts
- `vpc.id` when present

No parallel public-ingress rule is added.

## S3 mapping

Only metadata is read:

- bucket list (account-global)
- bucket location; buckets outside allowed regions are skipped
- tags (missing tag set is empty; AccessDenied omits the bucket and records a failure)
- versioning status
- public-access-block configuration (missing configuration is explicitly `configured=false`)
- no object listing or object content access

Unknown/denied metadata never becomes a healthy resource.

## Resilience

- AccessDenied, throttling, timeout, endpoint/network, service and unknown errors map to stable `DiscoveryFailureCode`s.
- Pagination is bounded and guards repeated tokens.
- Resource limits produce `PARTIAL` with `RESOURCE_LIMIT`.
- Duplicate identities produce `DUPLICATE_RESOURCE`.
- SDK calls use 15 s call / 5 s attempt timeouts and at most 3 attempts (2 retries).
- Raw SDK messages and payloads are not exposed.

## Tests

`./mvnw -B clean verify` uses mocked EC2/S3/STS clients; no credentials or network are required. The AWS module enforces 85 % line / 75 % branch coverage and currently reports 95.35 % / 84.55 %.

## Live scan is not implemented

DR-MAYS-04 prepares the boundary only. A first live scan needs a separate explicit approval for profile, account, region, services, resource cap and least-privilege IAM. See `docs/security/AWS_SCANNER_IAM.md`.
