# Docker Development

The demo image is local-only. It runs the existing Spring Boot API and never calls AWS, a model provider or a database.

## Images

| Stage | Image | Digest |
| --- | --- | --- |
| Build | `eclipse-temurin:25-jdk-noble` | `sha256:95e350ad1eb738f943e141640f7e3d797e483359a3b33c3864255bfd1ad71f8a` |
| Runtime | `eclipse-temurin:25-jre-noble` | `sha256:a98128a57de944a72bd31fbc79b898edd5a5864d3652071d5bbd2160ab7d7dce` |

The Dockerfile pins both multi-arch digests. The build stage uses the repository's Maven wrapper 3.9.16 and runs `./mvnw -B -DskipTests package`; Maven tests are executed separately by `./mvnw -B clean verify`. No Maven, JDK compiler or build artifacts are copied into the runtime image.

## Build and run

```bash
# Build only
docker compose build

# Build, start, run the offline demo and stop
./scripts/docker-demo.sh
```

Manual lifecycle:

```bash
docker compose up -d
curl -s http://127.0.0.1:18080/actuator/health
curl -s http://127.0.0.1:18080/api/v1/rules
docker compose down --remove-orphans
```

The container listens on `0.0.0.0:8080` only inside its network namespace. The host publishes `127.0.0.1:18080` exclusively; there is no `0.0.0.0` host mapping.

## Runtime security

| Control | Setting |
| --- | --- |
| User | `10001:10001` (non-root, `drmays`) |
| Root filesystem | read-only |
| Writable path | `/tmp` tmpfs, 64 MB, `nosuid,nodev` |
| Linux capabilities | all dropped |
| Privilege escalation | `no-new-privileges:true` |
| Init / signal forwarding | `init: true` + graceful shutdown |
| Host exposure | loopback only |
| Secrets | none |
| Extra services | none |

There is intentionally no `curl`/`wget` in the runtime image. Health is checked by the host-side demo/smoke script, which is sufficient for this local demo.

## Verification

```bash
docker compose config
docker compose build
./scripts/docker-demo.sh
```

The smoke script verifies health, rules, all four valid fixtures, deterministic repeated output and invalid-input handling, then stops the container. Docker checks are separate from JUnit and must be reported separately.

## Troubleshooting

- **Port already in use:** stop the conflicting process or change the host side of `127.0.0.1:18080:8080`.
- **Read-only filesystem failures:** add a narrowly scoped tmpfs only if a real runtime path requires it; do not disable the read-only root.
- **Model warnings during tests:** Mockito may self-attach on Java 25; this is a test-time warning, not a runtime failure.
- **No image build without network:** the Maven wrapper and dependencies are downloaded in the build stage.
