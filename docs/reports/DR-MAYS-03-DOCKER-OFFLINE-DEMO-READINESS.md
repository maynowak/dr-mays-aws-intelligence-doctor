# DR-MAYS-03 — Docker Development & Offline Demo — Readiness Report

- Date: 2026-11-03
- Repository: `/home/dci-student/projects/dr-mays-aws-intelligence-doctor`
- Branch: `feature/dr-mays-docker-offline-demo-03`
- Base commit: `7374e5db112681ce2fa07cafe08496ea63fc9f85`
- Local status: **GREEN**
- CI status: **NOT VERIFIED** — no push permitted.
- Acceptance command: `./mvnw -B clean verify`

## 1. Docker images

| Stage | Image | Digest |
| --- | --- | --- |
| Build | `eclipse-temurin:25-jdk-noble` | `sha256:95e350ad1eb738f943e141640f7e3d797e483359a3b33c3864255bfd1ad71f8a` |
| Runtime | `eclipse-temurin:25-jre-noble` | `sha256:a98128a57de944a72bd31fbc79b898edd5a5864d3652071d5bbd2160ab7d7dce` |

Image: `dr-mays-api:local`. Maven 3.9.16 is used only in the build stage.

## 2. Docker build and run

- `docker compose config` passed.
- `docker compose build` succeeded; all modules built inside the JDK stage.
- `./scripts/docker-demo.sh` built, started, exercised and stopped the container successfully.
- Container published only `127.0.0.1:18080:8080`.
- Container was stopped after verification; no registry push occurred.

## 3. Runtime security

| Control | Observed |
| --- | --- |
| User | `10001:10001` |
| Root filesystem | read-only |
| Writable path | `/tmp` tmpfs (64 MB, `nosuid,nodev`) |
| Capabilities | all dropped |
| Privilege escalation | `no-new-privileges:true` |
| Init / SIGTERM | `init: true`, graceful shutdown |
| Extra services | none |
| AWS credentials | none |

## 4. Offline demo scenarios

| Scenario | Expected |
| --- | --- |
| Healthy storage | 0 findings; `NOT_APPLICABLE` + `NO_FINDING` |
| Missing owner | 1 × `TAG-OWNER-001` |
| Public ingress | 1 × `NET-PUBLIC-001` |
| Multiple findings | 2 findings, 4 outcomes |
| Invalid input | HTTP 400 ProblemDetail, no stack trace |

The demo also checks health, rules, deterministic repeated JSON and a non-zero exit on failure.

## 5. Actual Docker smoke results

```text
health: 200, status UP
rules: NET-PUBLIC-001, TAG-OWNER-001
healthy: 0 findings
missing-owner: 1 finding (TAG-OWNER-001)
public-ingress: 1 finding (NET-PUBLIC-001)
multiple-findings: 2 findings (NET-PUBLIC-001, TAG-OWNER-001)
determinism: byte-identical repeat
invalid-input: HTTP 400, title "Request validation failed"
security: User=10001:10001, ReadonlyRootfs=true, CapDrop=[ALL], no-new-privileges:true
DEMO PASSED
```

## 6. Tests and coverage

| Module | Tests | Line | Branch |
| --- | ---: | ---: | ---: |
| `dr-mays-core` | 63 | 100.00 % (210/210) | 98.08 % (51/52) |
| `dr-mays-api` | 52 | 97.83 % (135/138) | 100.00 % (19/19) |
| **Total** | **115** | — | — |

- 12 new fixture/demo contract tests were added.
- All 103 prior tests still pass.
- Spotless, Checkstyle and full reactor `verify` pass.
- No coverage exclusions were added.

## 7. Git commits

| Step | Commit | Message |
| --- | --- | --- |
| 03A | `e08b335` | `docs: start DR-MAYS-03 docker offline demo` |
| 03B | `32c2aba` | `build: add pinned multi-stage Java 25 image` |
| 03C | `5d0e5ea` | `build: add local-only Docker Compose demo` |
| 03D | `6c6ef88` | `feat: add offline infrastructure fixtures` |
| 03E | `ffcfe09` | `feat: add offline demo runner` |
| 03F | `edcb353` | `test: add offline fixture contract coverage` |
| 03G | current HEAD | Documentation, readiness and audit close-out |

No push, merge, reset or clean was performed.

## 8. CI status

CI is **NOT VERIFIED** for this branch. The existing workflow runs `./mvnw -B clean verify` with Java 25, SHA-pinned actions, read-only permissions and no credentials; a future authorized push is required to obtain real CI evidence. Docker checks are intentionally local and separate.

## 9. Risks and open points

- The runtime image has no healthcheck tool; health is external by design.
- The host must provide `curl` and `python3` for the demo.
- Public deployment would require authentication, TLS, rate limiting and review.
- The synthetic rules are not a complete AWS security analysis.
- Base-image digest updates require a rebuild and re-verification.
- Docker is local-only; no registry or CI image build exists.

## 10. Next phase recommendation

Proceed to **DR-MAYS-04** (read-only AWS discovery) or first add a fixture catalog/JSON report adapter. Keep the local demo and default loopback binding intact.

## 11. Recovery point

- Branch: `feature/dr-mays-docker-offline-demo-03`
- Base: `7374e5db112681ce2fa07cafe08496ea63fc9f85`
- Last gate commit: `edcb353`
- Final commit: current HEAD (`git log -1`)
- Resume with `git status`, `./mvnw -B clean verify`, and `./scripts/docker-demo.sh`.
