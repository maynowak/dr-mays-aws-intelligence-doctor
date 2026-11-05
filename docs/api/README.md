# Dr. Mays REST API

Local Spring Boot 4.1.2 API over the deterministic `dr-mays-core` engine. It accepts synthetic infrastructure snapshots and returns findings plus every rule/resource outcome.

The API is **local-only by default** (`127.0.0.1:8080`), has **no authentication**, and must not be exposed publicly. It performs no AWS or model-provider calls and uses no database.

## Start

```bash
./mvnw -B -DskipTests package
java -jar dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar
```

Override the port only if needed:

```bash
java -jar dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar --server.port=18080
```

The bind address remains `127.0.0.1` unless explicitly changed.

## Docker

```bash
docker compose up -d
curl -s http://127.0.0.1:18080/actuator/health
docker compose down --remove-orphans
```

The container uses the `docker` Spring profile to bind `0.0.0.0:8080` internally, but the host publishes only `127.0.0.1:18080`. It runs non-root, read-only, with all capabilities dropped. See `docs/development/DOCKER_DEVELOPMENT.md`.

## Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/diagnoses` | Run deterministic rules over a synthetic snapshot |
| POST | `/api/v1/dna/graphs` | Build the offline Infrastructure DNA graph |
| POST | `/api/v1/intelligence/explain` | Explain findings/evidence/relationships with mock AI (raw external path fail-closed) |
| POST | `/api/v1/expertise/previews` | Build a sanitized provider payload preview; no provider call |
| POST | `/api/v1/expertise/approvals` | Approve an exact preview payload; returns a single-use token |
| POST | `/api/v1/expertise/explain` | Execute a stored mock-only preview with optional token |
| POST | `/api/v1/reports` | Build a deterministic JSON Doctor Report |
| POST | `/api/v1/reports/markdown` | Build a deterministic Markdown Doctor Report |
| GET | `/api/v1/rules` | List registered rules in rule-ID order |
| GET | `/actuator/health` | Health endpoint; only Actuator endpoint exposed |

Full contract: [`openapi.yaml`](openapi.yaml).

## Example request

```bash
curl -s -X POST http://127.0.0.1:8080/api/v1/diagnoses \
  -H 'Content-Type: application/json' \
  -d '{
    "resources": [{
      "identity": {
        "provider": "aws",
        "accountId": "123456789012",
        "region": "eu-central-1",
        "resourceType": "network",
        "resourceId": "net-1"
      },
      "tags": {},
      "attributes": {"ingress.public": "true"}
    }]
  }'
```

Response structure:

```json
{
  "findings": [
    {
      "ruleId": "NET-PUBLIC-001",
      "ruleVersion": "1.0",
      "severity": "HIGH",
      "resource": {
        "provider": "aws",
        "accountId": "123456789012",
        "region": "eu-central-1",
        "resourceType": "network",
        "resourceId": "net-1"
      },
      "title": "Network resource allows public ingress",
      "description": "The resource declares public ingress.",
      "evidence": {
        "source": "resource-attribute",
        "observed": "ingress.public=true",
        "expected": "ingress.public=false",
        "details": {"attribute": "ingress.public", "value": "true"}
      },
      "recommendation": "Restrict ingress to explicitly trusted sources."
    }
  ],
  "outcomes": [
    {
      "ruleId": "NET-PUBLIC-001",
      "resource": {"provider": "aws", "accountId": "123456789012", "region": "eu-central-1", "resourceType": "network", "resourceId": "net-1"},
      "type": "FINDING"
    }
  ],
  "findingCount": 1
}
```

The example above is illustrative for one resource; the actual response also includes `TAG-OWNER-001` when the owner tag is missing.

## Limits and errors

- Maximum 100 resources per request.
- Maximum 50 tags and 50 attributes per resource.
- Bounded identity, key and value lengths.
- Unknown JSON properties, malformed JSON, duplicate identities and invalid resource types are rejected.
- Errors use RFC 9457 `application/problem+json`; validation failures include an `errors` map.
- Internal stack traces and raw exception messages are never returned.

## Tests

```bash
./mvnw -B clean verify
```

The API has MVC slice tests, full-context error tests and a real random-port server test. JaCoCo enforces 85 % line / 75 % branch coverage; current API coverage is 98.13 % line and 78.69 % branch. Reports: `dr-mays-api/target/site/jacoco/`.
