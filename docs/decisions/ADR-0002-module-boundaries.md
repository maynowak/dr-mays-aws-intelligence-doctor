# ADR-0002: Module Boundaries and Dependency Direction

- Status: Accepted
- Date: 2026-11-03

## Context

The system must keep diagnosis independent of both AWS and AI providers so rules remain deterministic and testable. A single-module start would make those boundaries harder to enforce later.

## Decision

Use seven Maven modules with these dependency edges:

```text
common <- core <- aws
                 <- api
                 <- ai
                 <- worker

infrastructure (isolated for now)
```

Rules:

- `core` owns the domain model, findings, evidence, relations and rule engine.
- `core` must not import AWS SDK, Spring AI or infrastructure types.
- `aws` contains read-only discovery adapters and translates AWS responses into core types.
- `ai` receives summarized findings and returns explanations; it never inspects AWS.
- `api` exposes reports and scan orchestration; it does not talk to AWS directly.
- `worker` is reserved for scheduled/Lambda execution and composes existing modules later.
- `infrastructure` contains CDK code and must not become a runtime dependency of the domain.

## Consequences

- Enforce the dependency direction in review; Maven does not prevent a module from adding an internal dependency.
- Phase-specific libraries (AWS SDK, Spring AI, CDK) stay out of module classpaths until their phase needs them.
- Offline fixtures can exercise `core` without AWS credentials or model access.
