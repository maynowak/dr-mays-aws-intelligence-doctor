# Dependency Baseline

Verified on 2026-11-03 against Maven Central and vendor documentation.

| Component | Version | Verification |
| --- | --- | --- |
| Java | 25.0.4.1 (OpenJDK) | `java -version`, `javac -version` |
| Maven | 3.9.16 | Maven Wrapper; `./mvnw -version` |
| Spring Boot | 4.1.2 | Maven Central; supports Java 17–27 per Spring Boot system requirements |
| Spring AI | 2.0.2 | Maven Central; supports Spring Boot 4.0.x/4.1.x |
| AWS SDK for Java | BOM 2.57.8 | Maven Central; Java 8+ per AWS documentation |
| AWS CDK | 2.275.0 | Maven Central; Java 8+ per AWS CDK documentation |
| Maven Enforcer | 3.7.0 | Maven Central |
| Maven Compiler Plugin | 3.15.0 | Spring Boot 4.1.2 parent |
| Maven Surefire Plugin | 3.6.1 | Maven Central |

## Resolved artifacts

These artifacts were fetched without being added to a module classpath:

```bash
./mvnw -q dependency:get -Dartifact=software.amazon.awssdk:s3:2.57.8 -Dtransitive=false
./mvnw -q dependency:get -Dartifact=software.amazon.awscdk:aws-cdk-lib:2.275.0 -Dtransitive=false
./mvnw -q dependency:get -Dartifact=org.springframework.ai:spring-ai-bom:2.0.2:pom -Dtransitive=false
```

All three resolved successfully. `dependency:tree` confirms the only compile dependencies in the reactor are the internal module edges.

## Deferred to later phases

- AWS SDK service clients: phase 04.
- Spring AI starters and provider adapters: phase 06.
- AWS CDK and Lambda runtime libraries: phase 07.

## Reproduction

```bash
./mvnw -B clean verify
```
