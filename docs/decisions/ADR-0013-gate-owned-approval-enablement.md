# ADR-0013: Gate-Owned Approval and Explicit Provider Enablement

- Status: Accepted
- Date: 2026-11-04
- Scope: `dr-mays-ai`
- Task: DR-MAYS-PROVIDER-SECURITY-03H

## Context

03 hardening left four trust-boundary residuals: caller-supplied approval boolean in `PrivacyGate.register`, no credential binding in authorization, registration implicitly enabling execution, and ambiguous FILE/KEYRING references.

## Decision

1. **Approval ownership moves into `PrivacyGate`.** `register` no longer accepts an approval boolean. The gate receives a server-owned `ApprovalRequirement`; the default/no-arg gate is strict, and production wires `ProviderApprovalPolicy.forRegistry` (exact `MockModelProvider` exemption). The gate records `approvalRequired` in `ApprovedInvocation` and `ProviderInvocationAuthorization`; the receipt derives from that gate decision.
2. **Registration != enablement.** `ProviderEnablement` is an immutable server-side snapshot. `mockOnly()` is the default; `of(registry, ids)` rejects unknown/malformed/wildcard IDs and mock spoofing. The engine uses enablement for routing and preview checks. `enabledByDefault` is not an enablement authority.
3. **Credential binding contract.** `ProviderCredentialBinding` carries a server-assigned binding ID, provider, logical reference, non-negative version and optional trusted fingerprint placeholder. It never contains a secret. Full authorization binding to a live resolver remains deferred because no resolver is enabled; this is explicitly documented.
4. **FILE/KEYRING disabled.** `ProviderCredentialReference` rejects FILE/KEYRING at construction until a secure resolver/containment contract is approved. ENV names remain explicitly prefixed; NONE is literal.
5. **No live activation.** Credential resolution and OpenCode live execution remain disabled/blocked. Mock remains the only enabled provider.

## Consequences

- A caller cannot disable approval through `register`.
- A registered provider is not executable unless explicitly enabled.
- Credential rotation/binding is represented but not authoritative until a real resolver exists.
- Tests must use server-owned policy/enablement; no unsafe compatibility overloads.
- Existing REST contracts are unchanged; default mock behavior remains.

## Rejected alternatives

- Boolean approval parameter or client-supplied approval.
- Registration as enablement.
- Descriptor flags for enablement/mock exemption.
- Retaining FILE/KEYRING with a denylist only.
- Claiming live credential binding without a resolver.
