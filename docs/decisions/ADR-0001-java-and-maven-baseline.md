# ADR-0001: Java 25 and Maven 3.9.16 Baseline

- Status: Accepted
- Date: 2026-11-03

## Context

The project targets Java 25 LTS and Maven 3.9.x. Spring Boot 4.1.2 supports Java 17 through 27. The workstation has Java 25.0.4.1 but Maven 3.8.7, below the requested baseline.

## Decision

- Compile and test with Java 25 (`maven.compiler.release=25`).
- Pin Maven 3.9.16 through the committed Maven Wrapper.
- Enforce Java `[25,26)` and Maven `[3.9.0,4.0.0)` with Maven Enforcer.
- Use Spring Boot 4.1.2 as the parent POM and pin plugin versions centrally.

## Consequences

- Builds require JDK 25; older JDKs fail fast with a clear Enforcer error.
- `./mvnw` downloads Maven 3.9.16 on first use, so local and CI builds use the same Maven version.
- Upgrading Java or Maven requires updating both this ADR and the Enforcer bounds.
