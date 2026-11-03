# ADR-0003: Dependency Management Strategy

- Status: Accepted
- Date: 2026-11-03

## Context

The foundation needs reproducible versions without pulling AWS, CDK or AI libraries onto module classpaths before the phases that use them.

## Decision

- Inherit dependency and plugin management from `spring-boot-starter-parent:4.1.2`.
- Import `software.amazon.awssdk:bom:2.57.8` and `org.springframework.ai:spring-ai-bom:2.0.2`.
- Manage `software.amazon.awscdk:aws-cdk-lib:2.275.0` directly; it has no BOM.
- Manage internal module versions from `${project.version}`.
- Do not add AWS SDK, Spring AI or CDK dependencies to any module until phases 04–07 need them.
- Pin every plugin version used by the foundation.

## Consequences

- BOM imports prove version availability and keep future versions consistent without adding runtime code.
- The empty module JARs are intentional; no placeholder production classes are added.
- Dependency upgrades happen in one root POM and must be re-verified with `./mvnw clean verify`.
