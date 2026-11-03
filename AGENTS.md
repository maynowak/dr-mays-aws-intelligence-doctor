# AGENTS.md

## Project status

- **Core domain + local API implemented; AWS/AI not started.** The Java 25 reactor, immutable domain, deterministic engine and Spring Boot 4 REST API exist and pass 103 local tests (63 core + 40 API). AWS discovery, AI, Docker and deployments remain targets — never claim them as implemented.
- Upstream: `https://github.com/maynowak/dr-mays-aws-intelligence-doctor` (`main` is the upstream baseline; DR-MAYS-02 work is on `feature/dr-mays-spring-rest-api-02`). No push or merge is authorized for DR-MAYS-02.
- License is GPL-3.0 (`LICENSE`). Do not relicense without explicit confirmation.
- Roadmap IDs use `DR-MAYS-<AREA>-<NN>` (phases 01–08). MVP gate is after phase 04: offline demo, inspectable checks, reproducible JSON diagnosis.

## Build and test commands

- **Always use the wrapper** (`./mvnw`); the system Maven is 3.8.7 and the Enforcer rejects it. The wrapper pins Maven 3.9.16 and downloads it on first use.
- Full verification (acceptance gate): `./mvnw -B clean verify` — currently 103 tests, Spotless, Checkstyle and JaCoCo (85 % line / 75 % branch for core and API).
- Formatting: `./mvnw spotless:apply` to fix, `./mvnw spotless:check` to verify. Never auto-format in CI.
- Static analysis: `./mvnw checkstyle:check` (0 violations required).
- Coverage reports: `dr-mays-core/target/site/jacoco/` and `dr-mays-api/target/site/jacoco/`.
- Single test class: `./mvnw -pl dr-mays-core -am -Dtest=SeverityTest -Dsurefire.failIfNoSpecifiedTests=false test`
- API tests: `./mvnw -pl dr-mays-api -am test`
- Start API: `./mvnw -B -DskipTests package && java -jar dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar`
- Spring Boot 4 uses Jackson 3 (`tools.jackson.databind`), not Jackson 2; annotations remain `com.fasterxml.jackson.annotation`.
- `-am` is required for `-pl dr-mays-core`: `dr-mays-common` is not installed in the local repository, so `-pl` alone fails dependency resolution.
- Compile/package without tests: `./mvnw -B -DskipTests package`
- JDK 25 is mandatory (`maven.compiler.release=25`, Enforcer bound to `[25,26)`). No AWS credentials are needed for build or tests.
- Empty-JAR warnings from modules without production classes are expected; they are not build failures.

## Module boundaries

- Reactor: `dr-mays-common`, `dr-mays-core`, `dr-mays-aws`, `dr-mays-api`, `dr-mays-ai`, `dr-mays-worker`, `dr-mays-infrastructure`. Base package: `io.github.maynowak.drmays`.
- Dependency direction: `core -> common`; `aws`, `api`, `ai`, `worker -> core`; `infrastructure` isolated. Do not let `core` import AWS SDK, Spring AI or CDK types.
- Core packages: `resource`, `evidence`, `finding`, `rule` (+ `rule.builtin`), `diagnosis`, `validation`. Rules must be deterministic and side-effect free; findings require evidence for the same resource.
- API packages: `api`, `api.config`, `api.v1`. DTOs are separate from domain types; `DiagnosisService` maps between them. The API is loopback-only, unauthenticated and not for public deployment.
- The API must use the existing `DiagnosisEngine`; do not duplicate diagnosis logic in controllers.
- AWS SDK, Spring AI and CDK are version-managed but intentionally **not** on any module classpath yet (phases 04, 06, 07). Do not add them without the corresponding phase.
- Add internal module versions in the root `dependencyManagement`; add new modules to `<modules>` and the internal dependency list.
- Pinned versions and rationale: `docs/development/dependency-baseline.md` and `docs/decisions/ADR-0001..0006`. Verify any upgrade with `./mvnw clean verify`.

## CI

- `.github/workflows/build.yml` runs Java 25 (Temurin), Maven caching and `./mvnw -B clean verify`; no AWS credentials or deployment.
- The workflow confirms JaCoCo XML and Checkstyle reports exist, then uploads Surefire, JaCoCo and Checkstyle reports as `quality-reports`.
- Proven runs: `41609325812` (commit `7c7f775`) and `41609592971` (commit `45cf93d`) both succeeded.
- The DR-MAYS-02 branch has **not** been pushed, so no CI run exists for its commits. Never report a new CI pass without an actual run.

## Audit workflow (from `docs/AI_AUDITLOG.md`)

- The audit is **READ-ONLY**; only execution logs and the index may be written.
- Read the spec first. Record only verified facts — never invent findings, checks, or Terraform results.
- Update the log continuously after each major section; chat history is not a recovery source.
- Log path: `docs/reports/[NO. OF TASK ++]-[SUBWORKING NO.]-[TASK]-EXECUTION_LOG.md`; index in `docs/AI_AUDITINDEX.md`.
- Include: status, date/time, Git branch/HEAD, scope, completed sections, findings, evidence/file references, `GREEN`/`YELLOW`/`ORANGE`/`RED`/`GRAY` classification, Terraform checks actually run and results, Git status, files changed (or explicit "none"), open questions, risks, next actions, resume point.
- Every subagent prompt must require the 16-point report defined in `docs/AI_AUDITLOG.md`; the orchestrator files it under `docs/reports/` and indexes it.
