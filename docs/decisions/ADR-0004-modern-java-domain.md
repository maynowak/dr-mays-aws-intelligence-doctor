# ADR-0004: Modern Java Domain Foundation

- Status: Accepted
- Date: 2026-11-03
- Scope: `dr-mays-core`

## Context

The core module currently contains only `Severity`. DR-MAYS-01 needs a stable, testable domain vocabulary before AWS adapters, REST or AI modules are built. The model must stay free of AWS SDK, Spring AI, CDK and cloud runtime types.

## Decision

Use immutable Java records for value objects and a small sealed result hierarchy for rule outcomes.

### Package layout

```text
io.github.maynowak.drmays.core
├── diagnosis   DiagnosisEngine, DiagnosisResult
├── evidence    Evidence
├── finding     DiagnosticFinding, Severity
├── resource    ResourceIdentity, InfrastructureResource
├── rule        DiagnosticContext, DiagnosticOutcome, DiagnosticRule, RuleId
│   └── builtin MissingOwnerTagRule, PublicIngressRule
└── validation  DomainChecks
```

### Model contracts

- `ResourceIdentity(provider, accountId, region, resourceType, resourceId)` is the stable identity. All fields are required text; equality is record equality; `canonicalId()` provides a deterministic sort key.
- `InfrastructureResource(identity, tags, attributes)` stores immutable string maps. Tags and attributes are deliberately schemaless at the domain boundary; AWS adapters will map their metadata into them.
- `Evidence(resource, source, observed, expected, details)` carries traceable technical proof. Its resource must match the finding.
- `DiagnosticFinding(ruleId, ruleVersion, severity, resource, title, description, evidence, recommendation)` reuses `Severity` and requires all fields; evidence and finding resource identities must match.
- `RuleId(String value)` validates a stable uppercase identifier such as `TAG-OWNER-001`.
- `DiagnosticRule` exposes `id()`, `description()`, a default `version()` of `1.0`, and `evaluate(resource, context)`. It must be deterministic and side-effect free.
- `DiagnosticOutcome` is sealed:
  - `FindingDetected` wraps a finding.
  - `NoFinding` records a reason.
  - `NotApplicable` records a reason.
  All variants expose `ruleId()` and `resource()`; `summary()` uses exhaustive pattern matching.
- `DiagnosticContext` owns an immutable, identity-sorted resource list and rejects duplicate identities.
- `DiagnosisEngine` accepts rules, rejects duplicate rule IDs, sorts rules by ID, evaluates each rule for each resource, validates outcome attribution, and returns a result.
- `DiagnosisResult` holds outcomes in deterministic order (resource canonical ID, then rule ID) and derives findings by pattern matching.

### Determinism

No timestamps, random values, hash-dependent ordering or external state are part of the domain. Given the same resources and rules, the same outcomes and findings are produced in the same order.

## Consequences

- The domain is fully testable without AWS credentials, Spring or a model provider.
- Adding AWS metadata requires mapping, not changing core contracts.
- Rules are easily replaceable and composable; the engine remains a small loop.
- A future rule needing cross-resource analysis can read the immutable `DiagnosticContext`.
- Schema-less attributes postpone AWS-specific typing, at the cost of runtime key conventions in synthetic rules.
