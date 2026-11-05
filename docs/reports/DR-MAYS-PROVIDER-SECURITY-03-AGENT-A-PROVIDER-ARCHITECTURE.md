# PROVIDER-SECURITY-03 — Provider Authorization Discovery

## 1. Status

`COMPLETE — READ-ONLY DISCOVERY.` No code changed.

## 2. Date/time

2026-11-04T14:59:05+01:00; session `ses_e5926f3b7ffeTGGWJPmiAt3xV0`.

## 3. Branch/HEAD

`feature/dr-mays-provider-security-03` at `3ec0e3b` (main baseline).

## 4. Scope

Provider contracts, router, registry, engine, approval gate and tests.

## 5. Completed steps

Mapped all paths to `ModelProvider.invoke`, inspected descriptor enforcement, model handling, gate registration and existing negative tests.

## 6. Actual findings

- HIGH latent: `ProviderRouter` has no approval/authorization input; enforcement exists only in the engine.
- HIGH latent: `ModelProvider.invoke` and registry lookup are public; direct in-process calls bypass all controls.
- MEDIUM: mock exemption trusts descriptor flags; an impostor descriptor could claim mock.
- MEDIUM: `PrivacyGate.register(..., boolean approvalRequired)` is caller-controlled.
- MEDIUM: no model allowlist; arbitrary syntactically valid model IDs reach metadata/CLI.
- LOW: `ApprovedInvocation` is a public record; single-use is gate-only.
- LOW: router limits come from provider descriptor.
- Current REST path is mock-only, so no live exploit.

## 7. Evidence

`ProviderRouter.java`, `ProviderRegistry.java`, `ModelProviderDescriptor.java`, `PrivacyFirstExpertiseEngine.java`, `PrivacyGate.java`, `ApprovedInvocation.java`; tests.

## 8. Classification

`YELLOW` current path; latent provider authorization design gap. No RED/ORANGE live issue.

## 9. Terraform

N/A.

## 10. Git status

Clean; no files changed.

## 11. Files changed

None.

## 12. No-change confirmation

No file/model/provider/AWS/credential/network/build operation.

## 13. Open questions

Authorization type placement; mock exact-class exemption; model allowlist defaults; registry API visibility.

## 14. Risks

Enabling any non-mock provider before router-level authorization would turn latent gaps into a live bypass.

## 15. Recommended next actions

Server-minted authorization object, router-only authorized route, exact mock policy, model allowlist, registry id invariant.

## 16. Resume point

Contract freeze/implementation at `3ec0e3b`.
