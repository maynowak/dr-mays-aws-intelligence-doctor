# ADR-0005: Mandatory Engineering Quality Gates

- Status: Accepted
- Date: 2026-11-03
- Scope: all Maven modules

## Context

DR-MAYS-01 established a working domain but had no automated formatting, static analysis or coverage enforcement. Future phases need consistent, reviewable quality gates that run identically locally and in CI.

## Decision

- Use **Spotless 3.11.0** with **Google Java Format 2.0.0** for Java formatting, unused-import removal, trailing-whitespace removal and final newlines. Bind `spotless:check` to `verify`.
- Use **Checkstyle 14.4.0** with a small project-owned ruleset at `config/checkstyle/checkstyle.xml`. It enforces naming, imports, braces, visibility, basic bug patterns, public-type Javadocs and no direct console printing. Bind `checkstyle:check` to `verify`.
- Use **JaCoCo 0.8.16** only in `dr-mays-core`, where productive domain code exists. Enforce 85 % line and 75 % branch coverage at `verify`; generate HTML and XML reports. Other modules have no coverage gate.
- Use the existing **Java 25 + Maven 3.9.16 wrapper** and `./mvnw -B clean verify` as the single acceptance command.
- Keep GitHub Actions read-only and SHA-pinned; CI runs the same verify command and uploads reports. CI never auto-formats.

## Rationale

- GJF provides a widely used, deterministic Java style; Spotless makes it reproducible and removes import noise.
- Checkstyle is intentionally lean to avoid duplicating formatter concerns or importing a large foreign ruleset unverified.
- Core-only JaCoCo thresholds avoid false failures in modules that intentionally have no production classes.
- A single verify command prevents drift between local and CI evidence.

## Consequences

- Java indentation is 2 spaces (GJF canonical); `.editorconfig` was aligned accordingly.
- A clean build requires all quality gates; a formatting or Checkstyle violation blocks the build.
- Coverage thresholds are enforced only where domain code exists; adding production classes to another module requires adding a gate there.
- Tool upgrades must be re-verified with a full clean build and documented in the dependency baseline.
