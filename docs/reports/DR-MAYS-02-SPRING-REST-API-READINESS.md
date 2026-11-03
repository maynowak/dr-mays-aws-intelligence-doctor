# DR-MAYS-02 — Spring Boot 4 REST API — Readiness Report

- Date: 2026-11-03
- Repository: `/home/dci-student/projects/dr-mays-aws-intelligence-doctor`
- Branch: `feature/dr-mays-spring-rest-api-02`
- Base commit: `daa80505ce247ae43c338838b666713293419243`
- Local status: **GREEN**
- CI status: **NOT VERIFIED** — no push was permitted, so no GitHub Actions run exists for this branch.
- Acceptance command: `./mvnw -B clean verify`

## 1. Implemented endpoints

| Method | Endpoint | Result |
| --- | --- | --- |
| POST | `/api/v1/diagnoses` | Runs existing `DiagnosisEngine` over a synthetic snapshot; returns findings, outcomes and evidence |
| GET | `/api/v1/rules` | Lists registered rules in rule-ID order |
| GET | `/actuator/health` | Actuator health; only exposed management endpoint |

## 2. Request and response contracts

- Versioned wire package: `io.github.maynowak.drmays.api.v1`.
- Request: `DiagnosisRequest` → `ResourceRequest` → `ResourceIdentityRequest`.
- Response: `DiagnosisResponse` with `FindingResponse`, `EvidenceResponse`, `OutcomeResponse`, `ResourceResponse`.
- Rules: `RulesResponse` with `RuleResponse`.
- Domain types are not serialized directly; `DiagnosisService` maps both directions.
- Limits: 100 resources, 50 tags/attributes per resource, bounded identity/key/value lengths.
- Unknown JSON properties are rejected.
- OpenAPI 3.1 contract: `docs/api/openapi.yaml`; usage/examples: `docs/api/README.md`.

## 3. Error handling

- RFC 9457 `application/problem+json` for all handled errors.
- Validation failures return 400 with an `errors` map.
- Malformed/unknown JSON returns 400 without parser details.
- Duplicate identities and invalid resource types return 400.
- Unsupported media type returns 415; unsupported method returns 405; unknown routes return 404.
- Unexpected failures return a generic 500; stack traces and raw exception messages are never exposed.
- `ApiExceptionHandler` is ordered above Spring Boot's built-in ProblemDetails handler so explicit responses win.

## 4. Spring Boot configuration

- Spring Boot 4.1.2, Java 25.
- `spring-boot-starter-webmvc`, `validation`, `actuator`; Jackson 3 is the Boot-managed JSON implementation.
- `server.address: 127.0.0.1`, `server.port: 8080`.
- `management.endpoints.web.exposure.include: health`; health details disabled.
- `spring.mvc.problemdetails.enabled: true`; unknown JSON properties fail.
- No credentials, AWS configuration, database or model provider settings.
- Executable jar via `spring-boot-maven-plugin`.

## 5. Runtime verification

The packaged jar was started on `127.0.0.1:18080` and called over HTTP:

```text
GET /actuator/health -> 200 {"groups":["liveness","readiness"],"status":"UP"}
GET /api/v1/rules     -> 200, two rules (NET-PUBLIC-001, TAG-OWNER-001)
POST /api/v1/diagnoses (network + public ingress, no owner)
                      -> 200, findingCount=2, findings [NET-PUBLIC-001, TAG-OWNER-001]
```

The process shut down gracefully after verification. No AWS or model call was made.

## 6. Tests and coverage

| Module | Tests | Line coverage | Branch coverage |
| --- | ---: | ---: | ---: |
| `dr-mays-core` | 63 | 100.00 % (210/210) | 98.08 % (51/52) |
| `dr-mays-api` | 40 | 97.83 % (135/138) | 100.00 % (19/19) |
| **Reactor total** | **103** | — | — |

- API tests include a Spring context test, MVC slice tests, full-context error tests, JSON/validation tests and a real random-port server test.
- API minimum: 25 new tests; delivered 40.
- Both modules enforce JaCoCo 85 % line / 75 % branch; no exclusions were added.
- Existing 63 core tests still pass.

## 7. Versions and dependencies

| Component | Version |
| --- | --- |
| Java | OpenJDK 25.0.4.1 |
| Maven | 3.9.16 via `./mvnw` |
| Spring Boot | 4.1.2 |
| Spring Boot WebMVC/Validation/Actuator | 4.1.2 |
| Jackson databind | 3.1.7 |
| JUnit Jupiter | 6.0.3 |
| Spotless / GJF | 3.11.0 / 2.0.0 |
| Checkstyle | 14.4.0 |
| JaCoCo | 0.8.16 |

`dr-mays-core` remains free of Spring, AWS SDK, Spring AI and CDK dependencies.

## 8. Git commits

| Step | Commit | Message |
| --- | --- | --- |
| 02A | `afa39d3` | `feat: bootstrap Spring Boot 4 REST API` |
| 02B | `6863c12` | `feat: define versioned REST API contract` |
| 02C | `b1a2003` | `feat: expose deterministic diagnosis API` |
| 02D | `138c994` | `feat: add validated ProblemDetail error handling` |
| 02E | `f87b6a7` | `test: verify health and loopback configuration` |
| 02F | `47d57b9` | `test: enforce API coverage gates` |
| 02G | current HEAD | Documentation, readiness and audit close-out |

No push, merge, reset or clean was performed.

## 9. CI status

- Workflow still runs `./mvnw -B clean verify` with Java 25, SHA-pinned actions, `contents: read` and no credentials.
- The branch has **not** been pushed; CI is **NOT VERIFIED** for DR-MAYS-02.
- Previous proven runs belong to DR-MAYS-00-CI only.

## 10. Open questions and risks

- The API is unauthenticated and loopback-only; public deployment requires authentication, TLS, rate limiting and a security review.
- Synthetic rules are not a complete AWS security analysis.
- Jackson 3 package changes may surprise code written for Jackson 2; use `tools.jackson.databind`.
- Spring Boot's built-in ProblemDetails handler can override custom advice if ordering is removed.
- A future push is required to obtain real CI evidence.

## 11. Next phase recommendation

Proceed to **DR-MAYS-03** (offline Docker demo/fixtures) or add a JSON report adapter and fixture loader. Keep the API local-only and preserve the DTO/domain boundary.

## 12. Recovery point

- Branch: `feature/dr-mays-spring-rest-api-02`
- Base: `daa80505ce247ae43c338838b666713293419243`
- Last gate commit: `47d57b9`
- Final commit: current HEAD (`git log -1`)
- Resume with `git status` and `./mvnw -B clean verify`.
