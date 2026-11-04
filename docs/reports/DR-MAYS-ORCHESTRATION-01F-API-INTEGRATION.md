# ORCH-01F — Controlled API Integration

## 1. Current status

`COMPLETE — ADDITIVE PREVIEW/APPROVAL/EXPLAIN API IMPLEMENTED.` Existing endpoints remain available.

## 2. Audit date/time

- Implementation: 2026-11-04T08:28–08:31+01:00
- API tests: 2026-11-04T08:30:18+01:00
- API verify: 2026-11-04T08:31:29+01:00

## 3. Git branch and HEAD

- Branch: `feature/dr-mays-privacy-provider-orchestration-01`
- Expertise commit: `c3153c3`
- API commit: pending after this report

## 4. Audit scope

Additive REST integration for privacy preview, approval and mock-only expertise execution. Preserve diagnosis, DNA and existing intelligence endpoints.

## 5. Completed work steps

- Added `IntelligenceInputFactory` as the single server-side request-construction path.
- Refactored `IntelligenceService` to reuse the factory.
- Added `ExpertiseService` and `ExpertiseController` with `/previews`, `/approvals`, `/explain`.
- Added versioned preview/approval/explain DTOs, field decisions, privacy validation and receipt.
- Added `PrivacyFirstExpertiseEngine` as a Spring bean.
- Added 12 API tests; API module now runs 96 tests.
- `./mvnw -pl dr-mays-api -am verify`: all coverage checks met, Spotless/Checkstyle PASS.

## 6. Actual findings

- Preview never calls a provider and exposes the exact sanitized payload/hash.
- Approval is server-side and tied to a preview ID; execution accepts no resource payload.
- Mock provider works without approval; remote providers would require a token.
- Unknown/expired previews return explicit `APPROVAL_REQUIRED` rather than invoking anything.
- Unknown providers are rejected; malformed/empty requests remain 400.
- Existing `/api/v1/intelligence/explain` still returns the mock success response.
- Raw account/VPC identifiers are absent from the preview payload.
- API and AI coverage gates pass.

## 7. Evidence/file references

- `dr-mays-api/src/main/java/io/github/maynowak/drmays/api/v1/Expertise*`
- `dr-mays-api/src/main/java/io/github/maynowak/drmays/api/v1/IntelligenceInputFactory.java`
- `dr-mays-ai/src/main/java/io/github/maynowak/drmays/ai/IntelligenceConfiguration.java`
- `/tmp/opencode/orch01-api-tests.log`
- `/tmp/opencode/orch01-api-verify.log`

## 8. Classification

`GREEN (local)` for additive mock-only API. Real provider transmission remains NOT VERIFIED/BLOCKED.

## 9. Terraform checks and results

N/A — none executed.

## 10. Git status

API source/tests and configuration pending commit.

## 11. Files changed

- Added input factory, expertise service/controller and DTOs.
- Refactored existing intelligence service to shared input construction.
- Added 12 API tests.
- Added this report.

## 12. Explicit no-change confirmation

No external provider call, model call, API key use, AWS call, push, merge or deployment.

## 13. Open questions

- OpenAPI documentation and demo script need updating.
- Real approval UI/token storage remains future work.
- Remote provider endpoint/credential resolution remains disabled.

## 14. Risks

- Preview payload is returned locally and may be logged by a caller; docs must warn.
- In-memory approval state is lost on restart and single-instance only.
- The new endpoints remain unauthenticated and loopback-only.

## 15. Recommended next actions

1. Update OpenAPI, demos and security/architecture docs.
2. Run full reactor verify.
3. Run Docker regression.
4. Perform final security review and audit.

## 16. Current resume point

ORCH-01G Quality Gates. API module 96 tests, coverage PASS; no live provider.
