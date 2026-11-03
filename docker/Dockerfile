# syntax=docker/dockerfile:1.7

# Build stage: Java 25 JDK and the repository's Maven 3.9.16 wrapper.
FROM eclipse-temurin:25-jdk-noble@sha256:95e350ad1eb738f943e141640f7e3d797e483359a3b33c3864255bfd1ad71f8a AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY dr-mays-common/pom.xml dr-mays-common/pom.xml
COPY dr-mays-core/pom.xml dr-mays-core/pom.xml
COPY dr-mays-aws/pom.xml dr-mays-aws/pom.xml
COPY dr-mays-api/pom.xml dr-mays-api/pom.xml
COPY dr-mays-ai/pom.xml dr-mays-ai/pom.xml
COPY dr-mays-worker/pom.xml dr-mays-worker/pom.xml
COPY dr-mays-infrastructure/pom.xml dr-mays-infrastructure/pom.xml
COPY dr-mays-common/src dr-mays-common/src
COPY dr-mays-core/src dr-mays-core/src
COPY dr-mays-aws/src dr-mays-aws/src
COPY dr-mays-api/src dr-mays-api/src
COPY dr-mays-ai/src dr-mays-ai/src
COPY dr-mays-worker/src dr-mays-worker/src
COPY dr-mays-infrastructure/src dr-mays-infrastructure/src

RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests package \
    && test -f dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar

# Runtime stage: JRE-only, non-root, no Maven or compiler tools.
FROM eclipse-temurin:25-jre-noble@sha256:a98128a57de944a72bd31fbc79b898edd5a5864d3652071d5bbd2160ab7d7dce AS runtime

RUN groupadd --system --gid 10001 drmays \
    && useradd --system --uid 10001 --gid drmays --home-dir /app --shell /usr/sbin/nologin drmays

WORKDIR /app
COPY --from=build --chown=drmays:drmays /workspace/dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar /app/app.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar", "--server.shutdown=graceful"]
