# AWS Scanner IAM — Least Privilege Concept

This is a **future live-scan** policy concept. DR-MAYS-04 does not execute it and does not resolve credentials.

## Required permissions (read-only)

| Service | Actions | Scope |
| --- | --- | --- |
| STS | `sts:GetCallerIdentity` | account identity check |
| EC2 | `ec2:DescribeSecurityGroups` | approved regions only |
| S3 | `s3:ListAllMyBuckets` | account bucket inventory |
| S3 | `s3:GetBucketLocation` | approved buckets |
| S3 | `s3:GetBucketTagging` | approved buckets |
| S3 | `s3:GetBucketVersioning` | approved buckets |
| S3 | `s3:GetBucketPublicAccessBlock` | approved buckets |

## Explicitly excluded

- All write/mutate actions (`Put*`, `Delete*`, `Create*`, `Modify*`, `Authorize*`, `Revoke*`).
- S3 object reads/writes (`s3:GetObject`, `s3:PutObject`, `s3:ListBucket` is not required for the current metadata calls).
- Secrets Manager, SSM Parameter Store and KMS decryption.
- IAM user/role/policy changes.
- Organizations, account creation or cross-account enumeration.
- CloudTrail, GuardDuty, Security Hub or other data sources not used by the current adapters.

## Guardrails

- Explicit named profile; never the `default` profile.
- Expected 12-digit account ID checked with STS before discovery; mismatch fails closed.
- Region allow-list and service allow-list from `DiscoveryScope`.
- Hard `maxResources` limit.
- No automatic credential discovery or fallback provider.
- No public API endpoint exposes discovery in this phase.
- The policy should be attached to a dedicated read-only role/profile, not an administrator identity.

## Example policy shape (illustrative, not deployed)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["sts:GetCallerIdentity"],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["ec2:DescribeSecurityGroups"],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListAllMyBuckets",
        "s3:GetBucketLocation",
        "s3:GetBucketTagging",
        "s3:GetBucketVersioning",
        "s3:GetBucketPublicAccessBlock"
      ],
      "Resource": "*"
    }
  ]
}
```

Tighten S3 bucket actions to approved bucket ARNs and add region conditions where the chosen IAM model supports them. The final policy requires security review before any live scan.
