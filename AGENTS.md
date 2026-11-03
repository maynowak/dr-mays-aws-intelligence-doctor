# AGENTS.md

## Project status

- **Foundation implemented; diagnosis not started.** The Java 25 Maven reactor, seven module POMs, test harness and CI workflow exist and pass locally. AWS discovery, rules, REST API, AI and deployments remain targets — never claim them as implemented.
- Upstream: `https://github.com/maynowak/dr-mays-aws-intelligence-doctor` (`main` is the upstream baseline; DR-MAYS-00 work is on `feature/dr-mays-initial-project-setup`). No push or merge is authorized by DR-MAYS-00.
- License is GPL-3.0 (`LICENSE`). Do not relicense without explicit confirmation.
- Roadmap IDs use `DR-MAYS-<AREA>-<NN>` (phases 01–08). MVP gate is after phase 04: offline demo, inspectable checks, reproducible JSON diagnosis.

## Build and test commands

- **Always use the wrapper** (`./mvnw`); the system Maven is 3.8.7 and the Enforcer rejects it. The wrapper pins Maven 3.9.16 and downloads it on first use.
- Full verification (acceptance gate): `./mvnw -B clean verify`
- Single test class: `./mvnw -pl dr-mays-core -Dtest=SeverityTest test`
- Compile/package without tests: `./mvnw -B -DskipTests package`
- JDK 25 is mandatory (`maven.compiler.release=25`, Enforcer bound to `[25,26)`). No AWS credentials are needed for build or tests.
- Empty-JAR warnings from modules without production classes are expected; they are not build failures.

## Module boundaries

- Reactor: `dr-mays-common`, `dr-mays-core`, `dr-mays-aws`, `dr-mays-api`, `dr-mays-ai`, `dr-mays-worker`, `dr-mays-infrastructure`. Base package: `io.github.maynowak.drmays`.
- Dependency direction: `core -> common`; `aws`, `api`, `ai`, `worker -> core`; `infrastructure` isolated. Do not let `core` import AWS SDK, Spring AI or CDK types.
- AWS SDK, Spring AI and CDK are version-managed but intentionally **not** on any module classpath yet (phases 04, 06, 07). Do not add them without the corresponding phase.
- Add internal module versions in the root `dependencyManagement`; add new modules to `<modules>` and the internal dependency list.
- Pinned versions and rationale: `docs/development/dependency-baseline.md` and `docs/decisions/ADR-0001..0003`. Verify any upgrade with `./mvnw clean verify`.

## CI

- `.github/workflows/build.yml` runs Java 25 (Temurin), Maven caching and `./mvnw -B clean verify`; no AWS credentials or deployment.
- The workflow has only been YAML-syntax-checked locally and has **not run on GitHub**. Never report CI as passing until an actual run exists.

## Audit workflow (from `docs/AI_AUDITLOG.md`)

- The audit is **READ-ONLY**; only execution logs and the index may be written.
- Read the spec first. Record only verified facts — never invent findings, checks, or Terraform results.
- Update the log continuously after each major section; chat history is not a recovery source.
- Log path: `docs/reports/[NO. OF TASK ++]-[SUBWORKING NO.]-[TASK]-EXECUTION_LOG.md`; index in `docs/AI_AUDITINDEX.md`.
- Include: status, date/time, Git branch/HEAD, scope, completed sections, findings, evidence/file references, `GREEN`/`YELLOW`/`ORANGE`/`RED`/`GRAY` classification, Terraform checks actually run and results, Git status, files changed (or explicit "none"), open questions, risks, next actions, resume point.
- Every subagent prompt must require the 16-point report defined in `docs/AI_AUDITLOG.md`; the orchestrator files it under `docs/reports/` and indexes it.
