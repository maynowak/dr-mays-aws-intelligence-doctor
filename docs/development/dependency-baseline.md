# Dependency Baseline

Verified on 2026-11-03 against Maven Central and vendor documentation.

| Component | Version | Verification |
| --- | --- | --- |
| Java | 25.0.4.1 (OpenJDK) | `java -version`, `javac -version` |
| Maven | 3.9.16 | Maven Wrapper; `./mvnw -version` |
| Spring Boot | 4.1.2 | Maven Central; supports Java 17–27 per Spring Boot system requirements |
| Spring AI | 2.0.2 | Maven Central; supports Spring Boot 4.0.x/4.1.x |
| Spring AI Ollama starter | 2.0.2 | Resolved in `dr-mays-ai`; disabled by default |
| Ollama CLI | 0.32.1 | Installed locally; **no model present**, real inference NOT VERIFIED |
| AWS SDK for Java | BOM 2.57.8 | Maven Central; Java 8+ per AWS documentation |
| AWS SDK modules | EC2/S3/STS 2.57.8 | Resolved in `dr-mays-aws` only; mocked in tests |
| AWS CDK | 2.275.0 | Maven Central; Java 8+ per AWS CDK documentation |
| Maven Enforcer | 3.7.0 | Maven Central |
| Maven Compiler Plugin | 3.15.0 | Spring Boot 4.1.2 parent |
| Maven Surefire Plugin | 3.6.1 | Maven Central |
| JUnit Jupiter | 6.0.3 | Managed by Spring Boot 4.1.2; resolved in test scope |
| AssertJ | 3.27.7 | Managed by Spring Boot 4.1.2; resolved in test scope |
| Mockito | 5.23.0 | Managed by Spring Boot 4.1.2; resolved in test scope |
| Jackson databind | 3.1.7 | Managed by Spring Boot 4.1.2; package `tools.jackson.databind` |
| Spring Boot starters | webmvc/validation/actuator 4.1.2 | Maven Central; resolved in `dr-mays-api` |
| Spotless Maven Plugin | 3.11.0 | Maven Central; formatting gate |
| Google Java Format | 2.0.0 | Maven Central; Spotless Java formatter |
| Checkstyle | 14.4.0 | Maven Central; project ruleset |
| Maven Checkstyle Plugin | 3.6.0 | Maven Central |
| JaCoCo | 0.8.16 | Maven Central; Java 25 bytecode verified |

## Resolved artifacts

These artifacts were fetched without being added to a module classpath:

```bash
./mvnw -q dependency:get -Dartifact=software.amazon.awssdk:s3:2.57.8 -Dtransitive=false
./mvnw -q dependency:get -Dartifact=software.amazon.awscdk:aws-cdk-lib:2.275.0 -Dtransitive=false
./mvnw -q dependency:get -Dartifact=org.springframework.ai:spring-ai-bom:2.0.2:pom -Dtransitive=false
```

All three resolved successfully. `dependency:tree` confirms the only compile dependencies in the reactor are the internal module edges.

## Deferred to later phases

- Additional AWS SDK service clients beyond EC2/S3/STS: future approved discovery phases.
- Spring AI starters and provider adapters: phase 06.
- AWS CDK and Lambda runtime libraries: phase 07.

## Reproduction

```bash
./mvnw -B clean verify
```
