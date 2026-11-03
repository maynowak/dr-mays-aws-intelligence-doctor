# Quality Gates

Every change must pass the same local and CI gates. `./mvnw -B clean verify` is the single acceptance command.

## Gate order

| Order | Gate | Tool | Failure behavior |
| ---: | --- | --- | --- |
| 1 | Java/Maven baseline | Enforcer 3.7.0 | fails on Java outside `[25,26)` or Maven outside `[3.9,4.0)` |
| 2 | Formatting | Spotless 3.11.0 / GJF 2.0.0 | `spotless:check` fails on unformatted Java |
| 3 | Static analysis | Checkstyle 14.4.0 | any violation fails the build |
| 4 | Unit tests | JUnit 6.0.3 / Surefire 3.6.1 | any failure or error fails the build |
| 5 | Coverage | JaCoCo 0.8.16 | core/API line < 85 % or branch < 75 % fails the build |
| 6 | Reactor build | Maven | any module failure fails the build |

Spotless is applied locally with `./mvnw spotless:apply`; CI only checks, never rewrites files.

## Version pinning

Versions are pinned in the root POM: Spotless `3.11.0`, Google Java Format `2.0.0`, Checkstyle `14.4.0`, Checkstyle Maven plugin `3.6.0`, JaCoCo `0.8.16`, Surefire `3.6.1`, Enforcer `3.7.0`, Java `25`, Maven `3.9.16` through the wrapper.

## CI verification

`.github/workflows/build.yml`:

- runs on pushes to `main`/`feature/**`, pull requests to `main` and manual dispatch;
- uses Temurin Java 25 and the Maven wrapper;
- has `contents: read`, no AWS credentials, no deployment;
- pins actions to full commit SHAs;
- runs `./mvnw -B clean verify`, which includes all gates;
- uploads Surefire, JaCoCo and Checkstyle reports.

A CI claim is valid only after inspecting an actual successful run. Local PASS does not imply CI PASS.

## Definition of Done

A step is GREEN only when all of the following are true:

- [ ] Implementation is complete and in scope.
- [ ] `./mvnw spotless:check` passes.
- [ ] `./mvnw checkstyle:check` reports zero violations.
- [ ] `./mvnw -B clean verify` passes.
- [ ] All tests pass; minimum test volume is met.
- [ ] Core/API line/branch coverage gates pass.
- [ ] Module boundaries are unchanged or intentionally documented.
- [ ] `docs/development`, `docs/architecture` and relevant ADRs are updated.
- [ ] Execution log and `docs/AI_AUDITINDEX.md` are current.
- [ ] `git diff --check` is clean and no secrets are tracked.
- [ ] Review is complete and a clean commit exists.
- [ ] Working tree is clean.

Blocked or unavailable checks must be reported as `BLOCKED` or `NOT VERIFIED`; never report an unexecuted check as PASS.
