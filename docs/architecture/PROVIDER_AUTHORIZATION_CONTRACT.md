# Provider Authorization Contract (PROVIDER-SECURITY-03)

Status: frozen 2026-11-04
Decision: ADR-0012
Scope: `dr-mays-ai`, `dr-mays-api`

## 1. Invocation path

```text
API preview -> PrivacyProcessor(privacy-v2) -> PrivacyGate.register
API approval -> PrivacyGate.approve -> opaque token
API explain -> PrivacyGate.consume
                    |
                    v
         ProviderInvocationAuthorization (server-minted,
         payload/provider/model/expertise/policy-bound,
         expiring, single-use, non-public constructor)
                    |
                    v
ProviderRouter.route(request, authorization)
  -> provider registered/allowlisted
  -> model allowlist match
  -> capabilities (no tools)
  -> payload hash/policy binding
  -> authorization binding/expiry/single-use
  -> credential policy (disabled)
  -> ModelProvider.invoke exactly once
```

No other `route` overload may exist. `ModelProvider.invoke` remains a provider SPI but must not be reachable through production API/configuration without the authorization path.

## 2. Authorization type

`ProviderInvocationAuthorization`:
- final class, package-private constructor in `ai.privacy`;
- fields: nonce, payload hash, provider ID, model ID, expertise ID, policy version, expiry;
- public read-only accessors only; no JSON/Jackson annotations;
- `toString()` redacted;
- `PrivacyGate` is the only mint site, after successful consume;
- router rejects null, wrong tuple, expired, or reused authorization.

## 3. Mock exception

`ProviderApprovalPolicy` derives exemption from exact `MockModelProvider` class, not descriptor flags. Unknown provider → approval required. Registry rejects id `mock` from any other implementation. Mock still requires a server-issued authorization at router level, but no human approval token.

## 4. Model allowlist

`ProviderModelPolicy`:
- exact provider → exact model IDs;
- default: `mock -> deterministic-v1`;
- no wildcard, no prefix matching, no syntax-only authorization;
- checked at preview (before registration) and router;
- unknown provider/model or missing entry → fail closed.

## 5. Privacy default

Policy version **`privacy-v2`** allows:
`RULE_ID`, `SEVERITY`, `RESOURCE_ALIAS`, `EVIDENCE_SOURCE`, `EVIDENCE_OBSERVED`, `EVIDENCE_EXPECTED`, `COMPLETENESS`, `UNCERTAINTIES`.

Excluded by default: `RULE_VERSION`, `RESOURCE_TYPE`, `REGION_CLASS`, `TITLE`, `DESCRIPTION`, `RELATIONSHIP_TYPE`.

- Relationships are omitted when `RELATIONSHIP_TYPE` is denied.
- Excluded fields use `[excluded]` placeholders (shape unchanged; payload version remains `privacy-payload/v1`).
- policy version is part of payload/hash/approval binding; changing it invalidates prior approvals.
- adding a `PrivacyField` must not expand `defaultPolicy()`.
- validator rejects non-placeholder values in excluded fields.

## 6. Credential policy

`CredentialResolutionPolicy.disabled()`:
- no resolution, no env/file/keyring/AWS/OpenCode access;
- non-mock providers requiring credentials remain unavailable;
- logical references only; redacted strings;
- a future resolver needs a separate ADR, binding/version model, and approval.

## 7. OpenCode policy

`OpenCodeSandboxPolicy` requires all controls (OS isolation, no project write, no AWS/OpenCode credentials, deny tools/MCP/plugins, egress, bounded runtime/memory/output, cleanup). `ready()` is false by default; live adapter remains disabled. CLI permission configuration alone is not OS isolation.

## 8. Failure model

No provider call for missing/invalid/expired/reused authorization, provider/model mismatch, model not allowlisted, tools capability, policy mismatch, payload hash mismatch, disabled provider, credential policy failure, or OpenCode sandbox not ready. Errors are safe codes; no secret/raw payload in messages/logs.

## 9. Test strategy

- Router: no authorization, forged/mismatched/expired/reused, concurrent consume, wrong provider/model/policy/hash, tools, unregistered/disabled, model allowlist, zero provider calls on every negative.
- Mock policy: exact class vs impostor, descriptor manipulation, registry id invariant.
- Privacy: minimal default set, excluded-field validation, decision completeness, version/hash change, relationship omission, alias ordering, new-field deny-by-default, hostname/IP/secret regression.
- Credentials: disabled resolver, reference mismatch/denylist, redaction, no env/file access.
- OpenCode: blocked policy, live flag cannot bypass missing controls, no process runner in production.
- API: disallowed model 400, no authorization field accepted, existing mock flow SUCCESS.
