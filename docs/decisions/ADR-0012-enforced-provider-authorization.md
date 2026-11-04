# ADR-0012: Enforced Provider Authorization and Minimal Privacy Default

- Status: Accepted
- Date: 2026-11-04
- Scope: `dr-mays-ai`, `dr-mays-api`
- Task: DR-MAYS-PROVIDER-SECURITY-03

## Context

Discovery confirmed that the current mock-only path is fail-closed, but provider execution is protected mainly by the expertise engine. `ProviderRouter` has no authorization input, model IDs are syntax-only, the default privacy policy allows every field, and credential/OpenCode controls are mostly latent. Any future enablement of a non-mock provider without closing these gaps could create an approval bypass.

## Decision

1. **Authorization is minted below the router boundary.** `PrivacyGate.consume` creates `ProviderInvocationAuthorization` only after a successful approval and privacy check. The authorization is a final class with a package-private constructor in `ai.privacy`; it binds provider, model, expertise, policy version, exact payload hash, expiry and a nonce. It is not a public record/DTO and no REST field can carry it.
2. **Only one router entry point exists.** `ProviderRouter.route(ModelInvocationRequest, ProviderInvocationAuthorization)` validates provider registration/allowlist, model allowlist, capabilities, payload hash, policy version, authorization expiry, binding, single-use and credential policy before calling `ModelProvider.invoke`. The old unauthenticated route is removed.
3. **Mock exception is type-based and explicit.** `ProviderApprovalPolicy` exempts only the exact `MockModelProvider` class; unknown providers require approval. `ProviderRegistry` rejects a provider registering id `mock` unless it is the exact mock implementation. Descriptor flags cannot self-grant the exception.
4. **Model allowlist is exact.** `ProviderModelPolicy.mockOnly()` allows only `mock` / `deterministic-v1`; no wildcards, no fallback, no syntax-only authorization. Preview rejects disallowed models; router rechecks.
5. **Privacy default is minimal and versioned.** `privacy-v2` allows only the fields actually needed by the current mock/expertise flow: rule ID, severity, resource alias, evidence source/observed/expected, completeness and uncertainties. Title, description, rule version, resource type, region class and relationships are excluded by default. Relationship entries are omitted, not placeholder-encoded, when relationship fields are denied. Policy version changes invalidate earlier approvals.
6. **Deny-by-default for future fields.** `PrivacyPolicy.defaultPolicy()` uses an explicit set, `decisions()` is derived from `PrivacyField.values()`, and the validator rejects populated excluded fields. Adding an enum constant cannot automatically expand provider egress.
7. **Credentials remain disabled.** `CredentialResolutionPolicy.disabled()` and `DisabledCredentialResolver` provide a fail-closed contract. No environment/file/keychain/AWS/OpenCode credential is read. Router requires the policy; current non-mock resolution remains impossible.
8. **OpenCode live remains blocked.** `OpenCodeSandboxPolicy` requires all technical isolation controls; the adapter remains disabled and no production runner is introduced. A policy object cannot be satisfied by CLI permissions alone.
9. **No external activation.** No provider/model/credential/CLI setting added by this ADR enables live execution. Real provider E2E, BYOK backend and OpenCode canary remain separate approvals.

## Consequences

- A direct in-process router call without an authorization is rejected; a forged/expired/reused/mismatched authorization is rejected.
- Existing mock preview/approval/explain remains functional but uses the minimal policy and produces different payload hashes.
- Policy/approval tests must be updated for `privacy-v2`; no golden hashes are relied on.
- Model IDs must be explicitly listed; arbitrary strings are rejected at preview.
- Credential and OpenCode code is safer to extend, but live capability is still not present.

## Rejected alternatives

- Boolean `approved=true` or client-supplied authorization DTO.
- Relying only on engine-side checks (direct router/provider calls remain possible).
- Descriptor-flag mock exemption (forgeable).
- Wildcard model allowlists or syntax-only model checks.
- `EnumSet.allOf(PrivacyField.class)` defaults.
- Implementing a real credential resolver or CLI process runner in this task.
