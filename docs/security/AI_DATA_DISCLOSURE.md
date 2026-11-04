# AI Data Disclosure

Status: no external data transfer is enabled. The only executable provider is the local deterministic mock.

## Data categories and treatment

| Category | Examples | Provider treatment |
| --- | --- | --- |
| Account identifier | AWS account ID | Replaced with `acct_` alias |
| Resource identity | ARN, SG/VPC/subnet/instance ID, S3 name | Replaced with `res_`/`sg_`/`vpc_`/`arn_` alias |
| Region | `eu-west-1` | Excluded by default in `privacy-v2` |
| Tags/attributes | owner, application, topology metadata | Excluded |
| Evidence details | raw attribute map | Excluded |
| Email | owner/contact | Redacted/rejected |
| Hostname/IP | DNS names, IPv4/IPv6 | Redacted/rejected |
| Secrets | API keys, tokens, private keys | Rejected; payload invalid |
| Rule/finding semantics | rule ID, severity, title, description | Sanitized allowlisted text |
| DNA relationships | type and alias endpoints | Excluded by default; omitted from payload |

## What the preview shows

- Exact sanitized JSON that would be sent.
- Payload hash, policy version, provider/model/expertise.
- Field decisions and privacy violations.
- Expiry and whether approval is required.

Preview does not call a provider. The expertise explain endpoint accepts only the preview ID, so a changed client payload cannot be sent. The legacy intelligence endpoint returns `APPROVAL_REQUIRED` when external AI is enabled.

## What is never disclosed

- Raw domain objects or full infrastructure snapshots.
- Account/resource/VPC/SG identifiers.
- Tags, attributes, evidence details or inventories.
- Credentials, API keys, tokens or secret references.
- Raw prompts/responses in logs; only safe status/metadata may be logged.

## Minimization, masking, pseudonymization, anonymization

- **Minimization:** fewer fields and records; implemented by the allowlist.
- **Masking:** not used as the primary model-facing control.
- **Pseudonymization:** HMAC aliases with per-preview salt; reversible only with the local mapping.
- **Anonymization:** not claimed and not proven. Topology, counts, timing and rare combinations remain quasi-identifiers.

## Approval binding

Approval is server-side and binds the exact payload hash, provider, model, expertise and policy version. It is single-use and expires. A changed payload, wrong token, reuse or expiry fails closed.

## Residual risks

- Provider retention/training and legal basis require separate review.
- Local preview/report may contain resolved identifiers because the local operator already owns them.
- The loopback API is unauthenticated; any local process could request a preview.
- In-memory approval state is not durable across restarts.
- Network metadata (source IP, timing) is outside the payload and not controlled here.

## Current verification

- No real provider call, credential use, API key or external transfer occurred.
- Secret/IP/hostname checks are pattern-based; they reduce risk but do not prove absence of all sensitive values.
- Mock-only preview/approval/explain and sanitized payload tests pass (455 total project tests).
- Complete anonymity is not claimed.
