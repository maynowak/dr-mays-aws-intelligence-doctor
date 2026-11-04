# Provider Security

Status: local mock-only hardening; live providers remain blocked.

## Authorization chain

```text
preview -> PrivacyProcessor(privacy-v2) -> PrivacyGate.register
approval -> PrivacyGate.approve -> single-use token
explain -> PrivacyGate.consume
              -> ProviderInvocationAuthorization (package-private mint)
router -> only route(request, authorization)
       -> registered/enabled provider
       -> exact model allowlist
       -> tools=false capability
       -> canonical JSON + SHA-256 + policy/provider/model/expertise binding
       -> expiry + single-use nonce
       -> disabled credential policy
       -> provider.invoke once
```

`ProviderInvocationAuthorization` is not a public record and has no public constructor. REST DTOs cannot carry it; unknown JSON fields are rejected. The previous unauthenticated `ProviderRouter.route(request)` overload no longer exists.

## Gate-owned approval

`PrivacyGate.register` has no approval boolean. The gate evaluates a server-owned `ApprovalRequirement`; the default gate is strict and production uses exact-mock `ProviderApprovalPolicy`. Approval basis is carried on `ApprovedInvocation` and the server-minted authorization, so the receipt reflects the gate decision.

## Explicit enablement

`ProviderEnablement` separates registration from execution. `mockOnly()` is the default; explicit `of(registry, ids)` rejects unknown/wildcard/unregistered IDs. Registration never enables a provider, and `enabledByDefault` is not an enablement authority.

## Mock exception

Only the exact final `MockModelProvider` class is exempt from a human approval token. `ProviderApprovalPolicy` derives this from the concrete class, not `requiresApproval` or `kind` descriptor flags. `ProviderRegistry` rejects ID `mock` for any other implementation. The mock still requires a server-minted authorization at the router.

## Model allowlist

`ProviderModelPolicy.mockOnly()` permits exactly `mock` / `deterministic-v1`. There are no wildcards, prefixes, fallback models or discovery. Preview rejects disallowed models; the router rechecks.

## Payload integrity

`PayloadIntegrity` serializes the typed sanitized payload, hashes the exact JSON with SHA-256 and verifies the authorization hash and canonical JSON before invocation. Changing bytes or the typed payload fails closed.

## Credential policy

`CredentialResolutionPolicy.disabled()` + `DisabledCredentialResolver` are the defaults. No environment, file, keychain, AWS or OpenCode credential is read. Any future resolver needs a new ADR, approval binding/version model and tests.

## Failure guarantees

No provider call for missing/expired/reused/mismatched authorization, unknown/disabled provider, disallowed model, tools capability, payload substitution, policy mismatch, credential policy failure or blocked OpenCode sandbox. Errors are safe codes; no secret/payload values are returned.

## Residuals

- In-process callers are not a full security boundary; approval remains a loopback/local trust gate.
- Pattern-based privacy sanitization is not a semantic anonymity proof.
- OpenCode live execution and real BYOK remain blocked.
