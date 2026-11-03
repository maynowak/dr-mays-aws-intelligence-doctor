# AGENTS.md

## Project status

- **Core + API + Docker + simulated AWS + DNA + AI intelligence (mock/default) implemented; live AWS and real model inference not started.** The reactor passes 263 local tests (86 core + 84 API + 64 AWS + 29 AI). No live AWS scan or real Ollama inference has run — never claim one.
- Upstream: `https://github.com/maynowak/dr-mays-aws-intelligence-doctor` (`main` is the upstream baseline; DR-MAYS-06 is on `feature/dr-mays-spring-ai-intelligence-06`, PR #1 open). Merge is not authorized without explicit approval.
- License is GPL-3.0 (`LICENSE`). Do not relicense without explicit confirmation.
- Roadmap IDs use `DR-MAYS-<AREA>-<NN>` (phases 01–08). MVP gate is after phase 04: offline demo, inspectable checks, reproducible JSON diagnosis.

## Build and test commands

- **Always use the wrapper** (`./mvnw`); the system Maven is 3.8.7 and the Enforcer rejects it. The wrapper pins Maven 3.9.16 and downloads it on first use.
- Full verification (acceptance gate): `./mvnw -B clean verify` — currently 263 tests, Spotless, Checkstyle and JaCoCo (85 % line / 75 % branch for core, API, AWS and AI).
- Formatting: `./mvnw spotless:apply` to fix, `./mvnw spotless:check` to verify. Never auto-format in CI.
- Static analysis: `./mvnw checkstyle:check` (0 violations required).
- Coverage reports: `dr-mays-core/target/site/jacoco/`, `dr-mays-api/target/site/jacoco/`, `dr-mays-aws/target/site/jacoco/`, `dr-mays-ai/target/site/jacoco/`.
- Single test class: `./mvnw -pl dr-mays-core -am -Dtest=SeverityTest -Dsurefire.failIfNoSpecifiedTests=false test`
- API tests: `./mvnw -pl dr-mays-api -am test`
- Graph/API tests: `./mvnw -pl dr-mays-api -am -Dtest='Graph*Test,Dna*Test' -Dsurefire.failIfNoSpecifiedTests=false test`
- AI tests: `./mvnw -pl dr-mays-ai -am test`
- AWS discovery tests (mocked, no credentials): `./mvnw -pl dr-mays-aws -am test`
- Start API: `./mvnw -B -DskipTests package && java -jar dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar`
- Docker demo: `./scripts/docker-demo.sh` (builds, starts, runs fixtures, stops)
- Host demo against a running API: `DR_MAYS_API_URL=http://127.0.0.1:8080 ./scripts/demo.sh`
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
- Docker: `compose.yaml` publishes only `127.0.0.1:18080`, runs UID/GID `10001`, read-only root with `/tmp` tmpfs, all capabilities dropped, `no-new-privileges`, no extra services. Image base digests are pinned in `docker/Dockerfile`; do not add curl/wget to the runtime image—health is external.
- AWS discovery: SDK types live only in `dr-mays-aws`; core/api must stay AWS-free. No Spring wiring, no startup clients, no default profile, no live calls. `DiscoveryScope` requires explicit profile/account/regions/services/maxResources; STS mismatch fails closed. See `docs/development/AWS_DISCOVERY.md` and `docs/security/AWS_SCANNER_IAM.md`.
- DNA graph: core package `dna` reuses `ResourceIdentity`/`Evidence`; edges only from explicit `vpc.id` or `ingress.referenced.securityGroupIds`; no inferred reachability. Referenced nodes are not inventoried. `GraphAnalyzer` traversal max radius is 10. API endpoint is `POST /api/v1/dna/graphs`; findings carry verified outgoing `relationships`. See `docs/development/INFRASTRUCTURE_DNA.md` and ADR-0009.
- AI: contracts/mock/Spring AI adapter in `dr-mays-ai`; default is deterministic `MockIntelligenceAnalyzer`, Ollama only with `dr.mays.ai.enabled=true`. No tools, no startup connection, no model download. Validate all output against request evidence/relationships; reject unknown resources/evidence/edges. API endpoint `POST /api/v1/intelligence/explain`. See `docs/development/AI_INTELLIGENCE.md`, `docs/security/AI_MODEL_SECURITY.md`, ADR-0010.
- Fixtures live in `examples/fixtures` and are copied to the API test classpath by `dr-mays-api/pom.xml`. Keep them aligned with `docs/api/openapi.yaml`.
- Spring AI is isolated to `dr-mays-ai`; CDK is version-managed but intentionally **not** on any module classpath yet (phase 07). Do not add CDK without the corresponding phase.
- Add internal module versions in the root `dependencyManagement`; add new modules to `<modules>` and the internal dependency list.
- Pinned versions and rationale: `docs/development/dependency-baseline.md` and `docs/decisions/ADR-0001..0010`. Verify any upgrade with `./mvnw clean verify`.

## CI

- `.github/workflows/build.yml` runs Java 25 (Temurin), Maven caching and `./mvnw -B clean verify`; no AWS credentials or deployment.
- The workflow confirms JaCoCo XML and Checkstyle reports exist, then uploads Surefire, JaCoCo and Checkstyle reports as `quality-reports`.
- Proven runs: `41674237406` (push) and `41674526277` (pull request) on `b8c92d1` both succeeded.
- PR #1 is open, `MERGEABLE`/`CLEAN`; no merge has been performed. Live AWS and real Ollama inference remain NOT VERIFIED.

## Audit workflow (from `docs/AI_AUDITLOG.md`)

- The audit is **READ-ONLY**; only execution logs and the index may be written.
- Read the spec first. Record only verified facts — never invent findings, checks, or Terraform results.
- Update the log continuously after each major section; chat history is not a recovery source.
- Log path: `docs/reports/[NO. OF TASK ++]-[SUBWORKING NO.]-[TASK]-EXECUTION_LOG.md`; index in `docs/AI_AUDITINDEX.md`.
- Include: status, date/time, Git branch/HEAD, scope, completed sections, findings, evidence/file references, `GREEN`/`YELLOW`/`ORANGE`/`RED`/`GRAY` classification, Terraform checks actually run and results, Git status, files changed (or explicit "none"), open questions, risks, next actions, resume point.
- Every subagent prompt must require the 16-point report defined in `docs/AI_AUDITLOG.md`; the orchestrator files it under `docs/reports/` and indexes it.
