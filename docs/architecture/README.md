# Architecture Overview

Dr. Mays is a read-only AWS infrastructure diagnostics system. Technical evidence is produced by deterministic rules; AI may explain findings but never replaces evidence.

## Modules

| Module | Responsibility | Allowed dependencies |
| --- | --- | --- |
| `dr-mays-common` | Shared primitives with no AWS or AI types | None |
| `dr-mays-core` | Domain model, findings, evidence, relations, rule engine | `common` |
| `dr-mays-aws` | Read-only AWS SDK v2 discovery adapters | `core` |
| `dr-mays-api` | Spring Boot REST API | `core` |
| `dr-mays-ai` | Optional model adapters and explanations | `core` |
| `dr-mays-worker` | Future scheduled or Lambda execution | `core` |
| `dr-mays-infrastructure` | Future AWS CDK v2 definitions | None initially |

The dependency direction is always inward: AWS, API, AI and worker may depend on the domain, but the domain depends on neither AWS nor AI. `infrastructure` remains isolated until the deployment phase.

## Target flow

```text
AWS adapters or offline fixtures
        -> validated resource snapshots + verified links
        -> deterministic rule engine (authoritative findings)
        -> JSON diagnosis report
        -> optional AI explanation
```

## Principles

1. The domain engine works without AWS.
2. The domain engine works without AI models.
3. AWS access happens only through explicit adapters.
4. Findings are rule-based and carry stable evidence.
5. AI interprets findings; it does not create technical evidence.
6. AWS permissions follow least privilege.
7. Diagnosis never mutates inspected resources.
8. Modules remain independently testable.
