# ADR-0007: Container Security and Offline Demo

- Status: Accepted
- Date: 2026-11-03
- Scope: `docker/`, `compose.yaml`, `examples/fixtures`, `scripts/`

## Context

DR-MAYS-03 needs a reproducible local demo without public exposure, AWS credentials or extra services. The runtime should be small and constrained, while health checks should not require adding tools to the image.

## Decision

- Use digest-pinned Temurin 25 JDK/JRE Noble images.
- Build with the repository Maven wrapper 3.9.16 and skip tests in the image build; `./mvnw -B clean verify` remains the authoritative test gate.
- Run as UID/GID `10001` with a read-only root filesystem, `/tmp` tmpfs, all capabilities dropped, `no-new-privileges` and `init: true`.
- Publish only `127.0.0.1:18080:8080`; use a `docker` Spring profile to bind `0.0.0.0` inside the container.
- Keep the default application profile at `127.0.0.1`.
- Use a host-side shell runner for health, rules, fixtures, determinism and invalid-input checks instead of adding `curl`/`wget` to the runtime image.
- Store fixtures as synthetic JSON using the versioned API contract; use `000000000000` as the fake account ID.

## Consequences

- The runtime is non-root and minimal, with a small attack surface.
- Docker Compose has no in-container healthcheck tool; orchestration health must come from an external client.
- The demo requires `curl` and `python3` on the host.
- A public deployment would still need authentication, TLS, rate limiting and a security review.
- Image digests must be updated deliberately and re-verified with a real build.
