# Privacy Policy Hardening

Status: `privacy-v2` local default.

## Default allowlist

Provider-facing payload permits exactly:

- `RULE_ID`
- `SEVERITY`
- `RESOURCE_ALIAS`
- `EVIDENCE_SOURCE`
- `EVIDENCE_OBSERVED`
- `EVIDENCE_EXPECTED`
- `COMPLETENESS`
- `UNCERTAINTIES`

Excluded by default: `RULE_VERSION`, `RESOURCE_TYPE`, `REGION_CLASS`, `TITLE`, `DESCRIPTION`, `RELATIONSHIP_TYPE`.

Relationships are omitted (not placeholder-encoded) when relationship fields are denied, so graph topology is not transmitted by default. Excluded finding fields use `[excluded]` to keep the payload shape stable.

## Deny-by-default

- `PrivacyPolicy.defaultPolicy()` uses an explicit set, not `EnumSet.allOf`.
- `PrivacyProcessor.decisions()` derives from `PrivacyField.values()`.
- `PrivacyValidator` rejects populated excluded fields.
- Adding a future enum constant cannot silently expand the default payload.
- Policy version `privacy-v2` participates in the payload hash, preview and approval binding; a policy change invalidates prior approvals.

## Quasi-identifiers

Even minimized payloads retain rule IDs, severity, counts, relationships (if later enabled), topology and timing. This is pseudonymization, not anonymization. Sorting uses aliases, not raw canonical IDs; topology is omitted by default.

## Tests

Default-minimization, excluded-field validation, decision completeness, relationship omission, policy-version hash change, alias consistency/order, short IDs, hostnames/IPs, emails, URLs, secrets, nested/unknown data and policy regression are covered in `dr-mays-ai/src/test/.../privacy/`.
