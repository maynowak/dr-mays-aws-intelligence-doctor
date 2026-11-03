# Coding Standards

These standards apply to every Dr. Mays module and every contributor or agent.

## Java conventions

- **Language:** Java 25, no preview or experimental features. The build enforces the Java version.
- **Formatting:** Google Java Format 2.0.0 through Spotless 3.11.0. Java uses 2-space indentation, UTF-8, LF line endings and a final newline. Do not hand-format around the formatter.
- **Imports:** no wildcard imports, no unused imports. Spotless removes unused imports; Checkstyle rejects star imports, redundant imports and illegal JDK-internal imports.
- **Names:** packages are lowercase; types are `UpperCamelCase`; methods and fields are `lowerCamelCase`; constants are `UPPER_SNAKE_CASE`; test methods describe behavior.
- **Immutability:** domain values are records with unmodifiable collections copied in compact constructors. Do not expose mutable internal state.
- **Local variables:** use `var` only when the initializer makes the type obvious.
- **Null handling:** reject null required values immediately with `Objects.requireNonNull`; reject blank text with `DomainChecks.requireText`.
- **No `System.out`/`System.err`:** use structured logging when a logging dependency is introduced; Checkstyle blocks direct console printing.
- **Javadocs:** public domain contracts and non-obvious methods must explain intent, invariants and parameters. Record Javadocs include `@param` for every component. Do not restate the method name.

## Maven modules

| Module | Responsibility | Compile dependencies |
| --- | --- | --- |
| `dr-mays-common` | Shared primitives | none |
| `dr-mays-core` | Domain, findings, evidence, rules, engine | `common` |
| `dr-mays-aws` | Read-only AWS discovery adapters | `core` |
| `dr-mays-api` | Spring Boot REST API | `core` |
| `dr-mays-ai` | Optional model adapters | `core` |
| `dr-mays-worker` | Scheduled/Lambda execution | `core` |
| `dr-mays-infrastructure` | AWS CDK definitions | none initially |

## Error handling

- **Invalid arguments:** `IllegalArgumentException` for malformed values; `NullPointerException` via `Objects.requireNonNull` for absent required values.
- **Broken internal contracts:** `IllegalStateException`, for example when a rule returns an outcome for another rule or resource.
- **No silent failures:** never swallow exceptions or return a fabricated result.
- **No exceptions for normal diagnosis outcomes:** use `NoFinding` and `NotApplicable`; exceptions are reserved for programming or input errors.
- **Messages:** include the field, rule or resource that caused the failure; do not include secrets or raw credentials.
- **No `Throwable`/`Error` catches:** Checkstyle and `IllegalCatch`-style review reject broad catches.

## Domain invariants

- `ResourceIdentity` fields are non-blank; record equality defines identity.
- `InfrastructureResource` tags and attributes are immutable copies.
- Every `DiagnosticFinding` carries evidence for the same resource and a validated `RuleId` and `Severity`.
- `DiagnosticContext` resources are sorted by canonical ID and duplicate identities are rejected.
- Outcomes are exactly one of `FindingDetected`, `NoFinding` or `NotApplicable`.
- The engine runs every rule for every resource in deterministic order and rejects misattributed outcomes.
- No timestamps, randomness or hash-order dependence in the domain.

## Architecture constraints

- `dr-mays-core` must not depend on AWS SDK, Spring AI, CDK, Spring Boot or any cloud runtime.
- AWS, API, AI and worker modules may depend on `core`, never the reverse.
- Rules are deterministic and side-effect free; they must not mutate resources or call external systems.
- The domain must be executable without AWS credentials or model access.

## Commands

```bash
./mvnw spotless:apply   # apply formatting
./mvnw spotless:check   # verify formatting
./mvnw -B clean verify  # full quality gate
```

Run `spotless:apply` before committing. CI never auto-formats; it only checks.
