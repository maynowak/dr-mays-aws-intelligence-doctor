# Testing Standards

## Stack

- JUnit Jupiter 6.0.3
- AssertJ 3.27.7
- Mockito 5.23.0 (available; prefer real objects for domain tests)
- Maven Surefire 3.6.1
- JaCoCo 0.8.16
- Spring Boot 4.1.2 webmvc test slice and full-server tests

Tests run on Java 25 and must not require AWS credentials, network access or a model provider.

## Test structure

- Unit tests live next to the domain code in `dr-mays-core/src/test/java`.
- API tests live in `dr-mays-api/src/test/java`; MVC slice tests cover happy paths, full-context tests cover the exception advice, a random-port integration test exercises a real server, and fixture contract tests load `examples/fixtures` from the test classpath.
- `dr-mays-aws` tests use mocked EC2/S3/STS SDK clients; no credentials or network are required.
- `dr-mays-core` DNA tests cover evidence-only edges, referenced nodes, partial discovery, cycles, bounded traversal, components, account/region separation and deterministic ordering.
- `dr-mays-ai` tests cover contracts, mock explanations, evidence validation, prompt limits/injection guidance, malformed model output, unavailable/timeout behavior and deterministic output. Real Ollama inference is not required for CI.
- Docker integration is a separate shell smoke test (`./scripts/docker-demo.sh`), not a JUnit test; report it separately.
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

For `dr-mays-core` and `dr-mays-api` production code:

- **Line coverage: at least 85 %.**
- **Branch coverage: at least 75 %.**

The JaCoCo gate runs during `verify`. Reports are generated at:

- `dr-mays-core/target/site/jacoco/index.html`
- `dr-mays-core/target/site/jacoco/jacoco.xml`
- `dr-mays-api/target/site/jacoco/index.html`
- `dr-mays-api/target/site/jacoco/jacoco.xml`
- `dr-mays-aws/target/site/jacoco/index.html`
- `dr-mays-aws/target/site/jacoco/jacoco.xml`
- `dr-mays-ai/target/site/jacoco/index.html`
- `dr-mays-ai/target/site/jacoco/jacoco.xml`

Modules without production code do not have a coverage gate and must not fail for missing coverage.

Coverage must not be improved by meaningless tests, exclusions, deleting productive paths or weakening existing assertions. A justified exclusion requires a written reason in the change report.

## Minimum test volume

DR-MAYS-01 requires at least 40 passing tests, with at least 30 new meaningful tests over DR-MAYS-00. DR-MAYS-02 requires at least 25 new meaningful API tests. DR-MAYS-03 requires at least 12 new fixture/demo tests. DR-MAYS-04 requires at least 30 new AWS discovery tests. DR-MAYS-05 requires at least 35 new DNA tests. DR-MAYS-06 requires at least 35 new AI/API tests. The current suite has 497 tests: 97 core, 118 API, 64 AWS and 218 AI.

## Commands

```bash
# Full verification including coverage
./mvnw -B clean verify

# Focused core tests; -am builds the uninstalled sibling module
./mvnw -pl dr-mays-core -am test

# Focused API tests
./mvnw -pl dr-mays-api -am test

# Docker/offline smoke (separate from JUnit)
./scripts/docker-demo.sh

# Single test class
./mvnw -pl dr-mays-core -am -Dtest=DiagnosisEngineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Test evidence

- Surefire text/XML: `dr-mays-core/target/surefire-reports/`, `dr-mays-api/target/surefire-reports/` and `dr-mays-aws/target/surefire-reports/`
- JaCoCo HTML/XML: `dr-mays-core/target/site/jacoco/`, `dr-mays-api/target/site/jacoco/` and `dr-mays-aws/target/site/jacoco/`
- CI uploads these reports as artifacts.
