# AGENTS.md

## Project status

- **Pre-implementation.** The README describes targets, not completed work: there is no `pom.xml`, Maven wrapper, module code, Docker setup, CI, or test suite yet. Never claim capabilities are implemented or invent build/run commands.
- Upstream: `https://github.com/maynowak/dr-mays-aws-intelligence-doctor` (branch `main`, GPL-3.0 `LICENSE`). Work happens on `feature/dr-mays-initial-project-setup`; no push or merge is authorized for DR-MAYS-00.
- The README's license section still says "to be selected", but `LICENSE` is GPL-3.0. The file is authoritative; do not relicense without explicit confirmation.
- When commands do get added, verify them in a clean environment first; the README deliberately omits them until they work.

## Target architecture (do not treat as existing)

- Maven multi-module, Java 25 + Spring Boot 4, AWS SDK v2, optional Spring AI; AWS CDK in Java and Lambda only in later phases.
- Planned modules: `dr-mays-domain` (resources, findings, evidence, relations, rules), `dr-mays-aws` (discovery adapters), `dr-mays-api` (REST), `dr-mays-ai` (model adapters), `dr-mays-infra` (CDK, later), `dr-mays-demo` (offline fixtures).
- Core invariants: read-only AWS access, no mutating APIs or remediation; deterministic rule engine + evidence are authoritative; AI is optional, opt-in explanation only.
- The offline demo must run without AWS credentials or model access.
- Roadmap items use IDs like `DR-MAYS-JAVA-FOUNDATION-01` (phases 01–08). MVP gate is after phase 04: offline demo, inspectable checks, reproducible JSON diagnosis.

## Audit workflow (from `docs/AI_AUDITLOG.md`)

- The audit is **READ-ONLY**; the only permitted writes are execution logs and their index.
- Read the spec before audit work. Record only verified facts — never invent findings, checks, or Terraform results.
- Update the log continuously after each major section; chat history is not a recovery source.
- Log path: `docs/reports/[NO. OF TASK ++]-[SUBWORKING NO.]-[TASK]-EXECUTION_LOG.md`
- Include: status, date/time, Git branch/HEAD, scope, completed sections, findings, evidence/file references, `GREEN`/`YELLOW`/`ORANGE`/`RED`/`GRAY` classification, Terraform checks actually run and results, Git status, files changed (or explicit "none"), open questions, risks, next actions, resume point.
- With no git repo initialized, state that for branch/HEAD instead of fabricating values.
- Every subagent prompt must require the 16-point report defined in `docs/AI_AUDITLOG.md`; the orchestrator files it under `docs/reports/` and indexes it.
- `AI_AUDITINDEX.md` is referenced but not present yet; do not assume it exists.
