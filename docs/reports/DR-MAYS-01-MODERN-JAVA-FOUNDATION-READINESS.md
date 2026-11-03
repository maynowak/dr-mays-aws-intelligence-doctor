# DR-MAYS-01 — Modern Java Domain Foundation — Readiness Report

- Date: 2026-11-03
- Repository: `/home/dci-student/projects/dr-mays-aws-intelligence-doctor`
- Branch: `feature/dr-mays-modern-java-foundation-01`
- Base commit: `45cf93d50b8cd55118a38a562b5404dfe0fde5c1`
- Status: **GREEN (local)**
- Verification: `./mvnw -B clean verify` — all eight reactor projects `SUCCESS`
- Tests: **63 passed**, 0 failed, 0 errors, 0 skipped
- Java: OpenJDK 25.0.4.1
- Maven: 3.9.16 via `./mvnw`

## 1. Starting point

- The DR-MAYS-00-CI-01 completion (`45cf93d`) was verified clean before branching.
- `dr-mays-core` contained only `Severity` and its four tests.
- No AWS, Spring AI or CDK dependency was on the core compile classpath.
- The new branch was created from the verified completion; no reset, clean or history rewrite was used.

## 2. Implemented domain types

| Type | Location | Responsibility |
| --- | --- | --- |
| `ResourceIdentity` | `core.resource` | Provider-neutral identity with deterministic `canonicalId()` |
| `InfrastructureResource` | `core.resource` | Immutable tags/attributes; defensive copies |
| `Evidence` | `core.evidence` | Traceable source, observed/expected values and details |
| `DiagnosticFinding` | `core.finding` | Stable rule ID/version, severity, evidence and recommendation |
| `Severity` | `core.finding` | Existing ordered `INFO`–`CRITICAL` model, reused |
| `RuleId` | `core.rule` | Validated uppercase rule identifier |
| `DiagnosticRule` | `core.rule` | Deterministic, side-effect-free evaluation contract |
| `DiagnosticOutcome` | `core.rule` | Sealed `FindingDetected` / `NoFinding` / `NotApplicable` |
| `DiagnosticContext` | `core.rule` | Immutable, identity-sorted resource snapshot |
| `MissingOwnerTagRule` | `core.rule.builtin` | Demo rule `TAG-OWNER-001` |
| `PublicIngressRule` | `core.rule.builtin` | Demo rule `NET-PUBLIC-001` |
| `DiagnosisEngine` | `core.diagnosis` | Ordered rule execution and attribution validation |
| `DiagnosisResult` | `core.diagnosis` | Ordered outcomes, duplicate rejection, derived findings |
| `DomainChecks` | `core.validation` | Shared text/map validation helpers |

The two demonstration rules operate on synthetic fixtures only. They are not a complete AWS security analysis.

## 3. Architecture decisions

ADR-0004 records the design:

- Records for immutable value objects with compact-constructor validation.
- Defensive handling through unmodifiable, order-preserving maps.
- A sealed outcome hierarchy with exhaustive pattern matching for summaries.
- Explicit sorting: resources by canonical identity, rules by rule ID, outcomes by resource then rule.
- No timestamps, randomness, hash-order dependence or external state in the domain.
- Rules receive both the current resource and the full immutable context, allowing later cross-resource checks.

`DiagnosticFinding` and `FindingDetected` enforce that evidence, rule and resource attribution agree.

## 4. Rule processing

`DiagnosisEngine`:

1. Accepts a collection of rules.
2. Rejects null rules, duplicate IDs, blank descriptions and blank versions.
3. Sorts rules by rule ID.
4. Iterates identity-sorted resources from `DiagnosticContext`.
5. Evaluates every rule for every resource.
6. Rejects null outcomes and outcomes attributed to another rule or resource.
7. Returns a `DiagnosisResult` with deterministically sorted outcomes.

`DiagnosisResult.findings()` uses pattern matching over the sealed outcome type. Duplicate rule/resource outcome pairs are rejected.

## 5. Java 25 features used

- Records for all value objects.
- Sealed interface with three permitted outcome records.
- Exhaustive pattern-matching `switch` in `DiagnosticOutcome.summary()`.
- Pattern matching with `instanceof` in `DiagnosisResult.findings()`.
- `Stream.mapMulti` for deriving findings.
- Switch expressions in `PublicIngressRule`.
- `List.copyOf` and unmodifiable maps for immutability.
- No preview or experimental features.

## 6. Test coverage

| Test class | Tests |
| --- | ---: |
| `SeverityTest` | 4 |
| `DomainChecksTest` | 3 |
| `ResourceIdentityTest` | 4 |
| `InfrastructureResourceTest` | 4 |
| `EvidenceTest` | 3 |
| `RuleIdTest` | 4 |
| `DiagnosticFindingTest` | 4 |
| `DiagnosticContextTest` | 6 |
| `DiagnosticOutcomeTest` | 5 |
| `MissingOwnerTagRuleTest` | 4 |
| `PublicIngressRuleTest` | 6 |
| `DiagnosisResultTest` | 6 |
| `DiagnosisEngineTest` | 10 |
| **Total** | **63** |

Covered areas: valid and invalid record inputs, null/blank rejection, map immutability, evidence/resource consistency, identity sorting, duplicate identities, sealed outcome summaries, positive/negative/not-applicable rules, empty resources, empty rules, duplicate rule IDs, wrong outcome attribution and input-order-independent determinism.

## 7. Module dependencies

```text
dr-mays-core -> dr-mays-common (compile)
dr-mays-core -> java.base (bytecode)
```

- `dependency:tree -Dscope=compile` shows only `dr-mays-common`.
- `jdeps --multi-release 25` shows only `java.base`.
- No `software.amazon`, `org.springframework.ai` or `software.amazon.awscdk` imports exist in core main sources.
- The core test classpath contains Spring Boot's test starter only; it is test-scoped.

## 8. Verification evidence

```bash
./mvnw -B clean verify
# BUILD SUCCESS, 63 tests, 0 failures, 0 errors, 0 skipped

./mvnw -B -pl dr-mays-core -am dependency:tree -Dscope=compile
# core -> common only

jdeps --multi-release 25 -summary dr-mays-core/target/dr-mays-core-0.1.0-SNAPSHOT.jar
# dr-mays-core-0.1.0-SNAPSHOT.jar -> java.base
```

- `git diff --check` passed.
- Source inventory: 14 main Java files, 13 test classes.

## 9. Git commits

| Step | Commit | Message |
| --- | --- | --- |
| 01A | `81560f8` | `docs: define modern Java domain architecture` |
| 01B | `71aabcc` | `feat: add immutable diagnostic domain models` |
| 01C | `4022925` | `feat: add diagnostic rule contract and demo rules` |
| 01D | `3d1ca91` | `feat: add deterministic diagnosis engine` |
| 01E | `c691527` | `test: verify Java 25 domain foundation` |
| 01F | HEAD | Documentation, README/AGENTS reconciliation and this report |

No push, merge, reset or clean was performed.

## 10. Open points

- The branch has not been pushed; no GitHub Actions run exists for DR-MAYS-01 commits.
- The demo rules use synthetic `network`/`storage` attribute conventions, not AWS SDK types.
- Cross-resource rules are supported by `DiagnosticContext` but not yet demonstrated.
- JSON serialization and REST exposure are not part of this phase.
- The Maven single-module command requires `-am` until `dr-mays-common` is installed.

## 11. Risks

- Schema-less attributes can drift until AWS adapters define a mapping convention.
- Adding more rule versions requires discipline so `ruleVersion` remains meaningful.
- Deterministic ordering depends on `canonicalId()`; changing its field order would change sort order.
- CI for this branch remains unverified until a future authorized push.

## 12. Next phase recommendation

Proceed to **DR-MAYS-02** (Spring REST API) or first add a small offline fixture/demo layer:

1. Add a fixture loader that builds `DiagnosticContext` without AWS.
2. Add a JSON report adapter around `DiagnosisResult` (still without Spring).
3. Then expose the report through the API module.

Keep `dr-mays-core` free of AWS SDK, Spring AI and CDK dependencies.

## 13. Recovery point

- Branch: `feature/dr-mays-modern-java-foundation-01`
- Base: `45cf93d50b8cd55118a38a562b5404dfe0fde5c1`
- Last implementation commit: `c691527`
- Final commit: current HEAD (`git log -1`)
- To resume: `git status`, then `./mvnw -B clean verify`.
- Do not reset or clean; no such operation was used.
