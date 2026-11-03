# Testing Standards

## Stack

- JUnit Jupiter 6.0.3
- AssertJ 3.27.7
- Mockito 5.23.0 (available; prefer real objects for domain tests)
- Maven Surefire 3.6.1
- JaCoCo 0.8.16

Tests run on Java 25 and must not require AWS credentials, network access or a model provider.

## Test structure

- Unit tests live next to the domain code in `dr-mays-core/src/test/java`.
- Test classes are package-private and named `<Type>Test`.
- Test methods describe the behavior and expected result in `lowerCamelCase`.
- One logical behavior per test; avoid assertions unrelated to the test name.
- Shared synthetic fixtures belong in `TestFixtures`; they must be deterministic and contain no secrets.

## Required coverage areas

Every domain change must consider:

- valid construction and canonical values;
- invalid values: null, blank, malformed, wrong evidence/resource attribution;
- immutability of maps and lists;
- rule applicability: finding, no finding, not applicable;
- engine behavior: empty resources, empty rules, duplicate rules, null outcomes, wrong rule/resource attribution;
- deterministic ordering independent of input order;
- sealed outcome handling and pattern matching.

Do not use dummy tests, duplicate tests with cosmetic changes, or parameterized cases that repeat the same input class. A test must verify a real business or technical behavior.

## Coverage requirements

For `dr-mays-core` production code:

- **Line coverage: at least 85 %.**
- **Branch coverage: at least 75 %.**

The JaCoCo gate runs during `verify`. Reports are generated at:

- `dr-mays-core/target/site/jacoco/index.html`
- `dr-mays-core/target/site/jacoco/jacoco.xml`

Modules without production code do not have a coverage gate and must not fail for missing coverage.

Coverage must not be improved by meaningless tests, exclusions, deleting productive paths or weakening existing assertions. A justified exclusion requires a written reason in the change report.

## Minimum test volume

DR-MAYS-01 requires at least 40 passing tests, with at least 30 new meaningful tests over DR-MAYS-00. The current suite has 63 tests: 59 new domain tests plus the four original `Severity` tests.

## Commands

```bash
# Full verification including coverage
./mvnw -B clean verify

# Focused core tests; -am builds the uninstalled sibling module
./mvnw -pl dr-mays-core -am test

# Single test class
./mvnw -pl dr-mays-core -am -Dtest=DiagnosisEngineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Test evidence

- Surefire text/XML: `dr-mays-core/target/surefire-reports/`
- JaCoCo HTML/XML: `dr-mays-core/target/site/jacoco/`
- CI uploads these reports as artifacts.
