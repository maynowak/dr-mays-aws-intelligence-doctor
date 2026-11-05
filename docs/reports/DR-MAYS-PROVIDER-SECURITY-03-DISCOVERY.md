# PROVIDER-SECURITY-03 — Discovery Synthesis

## 1. Status

`COMPLETE — discovery synthesized; contract freeze next.`

## 2. Date/time

2026-11-04T15:20+01:00.

## 3. Branch/HEAD

`feature/dr-mays-provider-security-03` at `3ec0e3b`.

## 4. Scope

Synthesis of provider authorization, privacy policy and credential/OpenCode discovery.

## 5. Completed steps

Three independent read-only reports completed with 16-point audits; findings reconciled.

## 6. Actual findings

- No current live external provider path.
- Router lacks its own authorization proof; mock exemption trusts descriptor/caller.
- Model IDs are syntax-only.
- Default privacy policy allows all fields and lacks excluded-field validation.
- Credential resolver/CLI sandbox controls are latent, not active.
- OpenCode live remains blocked; binary pin exists only in docs.

## 7. Evidence

Three agent reports above; source/test/docs inspection.

## 8. Classification

`YELLOW` — hardening required before live provider activation.

## 9. Terraform

N/A.

## 10. Git status

Clean; no files changed.

## 11. Files changed

Added discovery reports and this synthesis.

## 12. No-change confirmation

No file/model/provider/AWS/credential/network operation.

## 13. Open questions

Final authorization type placement, minimal policy set, model allowlist representation, credential/sandbox contract depth.

## 14. Risks

Latent bypass, policy drift, ambient credentials, live CLI attachment.

## 15. Recommended next actions

Freeze ADR-0012, then implement authorization, policy, model allowlist, credential and sandbox readiness sequentially.

## 16. Resume point

Contract freeze.
