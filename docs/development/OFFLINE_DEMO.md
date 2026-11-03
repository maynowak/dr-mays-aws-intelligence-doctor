# Offline Demo

The offline demo sends synthetic infrastructure snapshots to the local REST API. The existing Java `DiagnosisEngine` performs all diagnosis; the scripts only perform HTTP calls and assert the documented JSON contract.

No AWS credentials, model calls or database are required.

## Quick start

```bash
# Option A: run the API locally
./mvnw -B -DskipTests package
java -jar dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar
# In another terminal:
./scripts/demo.sh

# Option B: Docker (build, run, demo, stop)
./scripts/docker-demo.sh
```

`DR_MAYS_API_URL` defaults to `http://127.0.0.1:18080` for the Docker demo. Set it to `http://127.0.0.1:8080` when using the locally started jar.

## Scenarios

| Fixture | Synthetic content | Expected findings |
| --- | --- | --- |
| `healthy.json` | Storage resource with owner tag | 0 findings; `NOT_APPLICABLE` + `NO_FINDING` |
| `missing-owner.json` | Network without owner tag, private ingress | 1 × `TAG-OWNER-001` |
| `public-ingress.json` | Network with owner tag and public ingress | 1 × `NET-PUBLIC-001` |
| `multiple-findings.json` | Network with public ingress/no owner + healthy storage | 2 findings, 4 outcomes |
| `invalid-input.json` | Empty resource list | HTTP 400 ProblemDetail, no stack trace |

Fixtures use the synthetic account ID `000000000000`; they contain no real account data or credentials.

## What the runner checks

1. `/actuator/health` returns `UP`.
2. `/api/v1/rules` lists both expected rule IDs.
3. Each valid fixture returns the expected `findingCount` and rule ID order.
4. Findings and evidence are printed from the JSON response.
5. Repeating the multiple-findings request produces byte-identical JSON.
6. Invalid input returns HTTP 400 and no internal details.
7. Exit code is `0` only when every check passes; failures exit non-zero.

## Expected example

`public-ingress.json` returns a `NET-PUBLIC-001` finding with:

- severity `HIGH`
- evidence `ingress.public=true` observed, `ingress.public=false` expected
- resource `net-public` in synthetic account `000000000000`

## Files

- Fixtures: `examples/fixtures/`
- Host demo: `scripts/demo.sh`
- Docker smoke wrapper: `scripts/docker-demo.sh`
- API contract: `docs/api/openapi.yaml`
