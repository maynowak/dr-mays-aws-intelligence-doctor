# 🩺 Dr. Mays — AWS Intelligence Infrastructure Doctor

> **We check your infrastructure — right down to its DNA!** 🧬
>
> *Understand the infrastructure. Detect the risks. Explain the diagnosis.*

**Project status:** 🟡 Core domain + REST API + Docker demo + AWS discovery foundation (simulated) / live AWS scan not started<br>
**Initial plan:** 3 November 2026<br>
**Project ID:** `DR-MAYS`<br>
**Type:** Independent, Java-first portfolio and reference project

> [!IMPORTANT]
> The Java 25 foundation, core diagnostic domain, local Spring Boot API, Docker offline demo and a simulated read-only AWS discovery layer are implemented and verified locally. **No live AWS scan has been executed.** AI and deployments remain targets. No AWS resources are provisioned by this README.

## 🎯 Vision

**Dr. Mays** is a planned, read-only AWS infrastructure diagnostics application. It will inspect explicitly authorized AWS environments, produce reproducible technical findings, identify *verifiable* relationships between resources, and optionally explain the results with AI.

The key idea is **Infrastructure DNA**: infrastructure is more than a list of resources. A Lambda function uses an IAM role; a security group affects access to an instance; resources and permissions form relationships that matter when diagnosing risks.

**Technical evidence first. AI-assisted explanation second. No autonomous remediation.**

## 🔬 Planned capabilities

| Capability | Initial scope |
| --- | --- |
| **AWS discovery** | Simulated foundation: EC2 security groups, S3 metadata, STS identity boundary; no live calls yet |
| **REST API** | Implemented locally: synthetic snapshot in, deterministic JSON findings/evidence out |
| **Rule engine** | Deterministic checks with stable rule IDs, severity, evidence and recommendations |
| **Infrastructure DNA** | Small graph of resource nodes and *verified* relationships |
| **Diagnosis report** | Machine-readable JSON with findings, affected resources and priorities |
| **AI interpretation** | Optional summarized findings via a replaceable model adapter; not the source of truth |
| **Local demo** | Implemented: Docker/offline fixtures with **no AWS account required** |
| **Cloud Doctor** | Later: Java AWS Lambda, optional EventBridge scheduling, infrastructure via AWS CDK in Java |

**Out of scope for the MVP:** automatic remediation, unrestricted account crawling, multi-account orchestration, cost forecasting and a complex web UI.

## 🧱 Implemented domain foundation (DR-MAYS-01)

`dr-mays-core` provides an AWS- and AI-independent diagnostic domain:

| Type | Purpose |
| --- | --- |
| `ResourceIdentity` | Provider-neutral resource identity with a deterministic canonical key |
| `InfrastructureResource` | Immutable resource with tags and schemaless attributes |
| `Evidence` | Traceable observed/expected proof for a finding |
| `DiagnosticFinding` | Rule ID, version, severity, evidence and recommendation |
| `DiagnosticRule` | Deterministic, side-effect-free rule contract |
| `DiagnosticOutcome` | Sealed finding / no-finding / not-applicable result |
| `DiagnosticContext` | Immutable, identity-sorted resource snapshot |
| `DiagnosisEngine` / `DiagnosisResult` | Ordered rule execution and findings |

Two synthetic demonstration rules ship with the foundation: `TAG-OWNER-001` checks for an owner tag and `NET-PUBLIC-001` checks a synthetic `ingress.public` attribute on `network` resources. They use local fixtures, require no AWS credentials and do not perform a complete AWS security analysis.

```java
var context = new DiagnosticContext(List.of(resource));
var engine = new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule()));
var result = engine.diagnose(context);
List<DiagnosticFinding> findings = result.findings();
```

## 🏗️ Architecture (target)

```text
                   Local demo / authorized AWS account
                                |
                                v
                      +--------------------+
                      | Spring Boot REST   |
                      | API · Java 25      |
                      +--------------------+
                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
             +-------------+       +------------------+
             | AWS SDK v2  |       | Fixture adapter  |
             | Discovery   |       | Offline demo     |
             +-------------+       +------------------+
                    |                       |
                    +-----------+-----------+
                                |
                                v
                      +--------------------+
                      | Resource snapshots |
                      | + verified links   |
                      +--------------------+
                                |
                                v
                      +--------------------+
                      | Java rule engine   |
                      | Findings + evidence|
                      +--------------------+
                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
             +--------------+      +-------------------+
             | JSON report  |      | Optional Spring AI|
             | (authoritative)     | explanation       |
             +--------------+      +-------------------+

        Later: AWS Lambda + EventBridge + AWS CDK (Java)
```

AI does **not** independently inspect AWS, invent evidence or modify resources. The rule engine and validated inventory remain authoritative.

## ☕ Planned technology stack

| Layer | Technology | Purpose |
| --- | --- | --- |
| Language | **Java 25 LTS** | Records, sealed types, pattern matching; virtual threads where appropriate |
| Backend | **Spring Boot 4** | REST API, validation, configuration, health checks |
| AWS integration | **AWS SDK for Java v2** | Explicitly scoped discovery adapters |
| Infrastructure as Code | **AWS CDK v2 (Java)** | Later cloud deployment, reviewable infrastructure changes |
| Serverless | **AWS Lambda (Java)** | Optional Cloud Doctor execution |
| Containers | **Docker / Docker Compose** | Reproducible local setup |
| AI | **Spring AI** | Optional structured model output and provider adapters |
| Testing | **JUnit, Mockito, Testcontainers** | Unit, contract and integration tests |
| Delivery and telemetry | **GitHub Actions, Micrometer / OpenTelemetry** | CI, logging, metrics and traces |

Exact dependency versions are pinned in the root POM; see `docs/development/dependency-baseline.md`.

## 🧬 Example diagnosis (illustrative)

```json
{
  "checkId": "SG-001",
  "severity": "HIGH",
  "resourceType": "AWS::EC2::SecurityGroup",
  "resourceId": "sg-example123",
  "finding": "Inbound SSH is allowed from 0.0.0.0/0",
  "evidence": {
    "protocol": "tcp",
    "port": 22,
    "cidr": "0.0.0.0/0"
  },
  "recommendation": "Verify the access requirement and restrict trusted source ranges.",
  "ruleVersion": "1.0"
}
```

This is **sample data**, not the result of a live AWS scan. The diagnosis would be derived from an explicit rule and its evidence. An optional AI summary is labeled as an interpretation.

## 📂 Repository structure

```text
dr-mays-aws-intelligence-doctor/
├── README.md
├── LICENSE
├── AGENTS.md
├── pom.xml
├── mvnw / mvnw.cmd
├── dr-mays-common/         # Shared primitives, no AWS/AI types
├── dr-mays-core/           # Implemented domain, findings, evidence, rules, engine
├── dr-mays-aws/            # Implemented read-only AWS discovery foundation (simulated)
├── dr-mays-api/            # Implemented Spring Boot REST API
├── dr-mays-ai/             # Optional model adapters (later)
├── dr-mays-worker/         # Scheduled or Lambda execution (later)
├── dr-mays-infrastructure/ # AWS CDK in Java (later)
├── docker/                 # Implemented multi-stage Docker image
├── examples/               # Implemented offline fixtures
├── docs/                   # Architecture, decisions, security, reports
├── scripts/                # Implemented offline/Docker demo runners
└── .github/workflows/      # CI pipeline
```

The core domain, local REST API and Docker offline demo are implemented today. Other module and support directories are intentionally reserved for their phases.

## 🗺️ Development roadmap

| Phase | Milestone | Target / acceptance evidence | Status |
| --- | --- | --- | --- |
| 00 | `DR-MAYS-00` | Java 25 Maven reactor, seven modules, tests, CI workflow, audit baseline | ✅ Complete (local verify) |
| 01 | `DR-MAYS-JAVA-FOUNDATION-01` | Java 25 domain model, Maven build, unit tests, ADR | ✅ Complete (local verify, 63 tests) |
| 02 | `DR-MAYS-SPRING-API-02` | REST contract, validation, mock scans, API tests | ✅ Complete (local verify, 103 tests) |
| 03 | `DR-MAYS-LOCAL-DOCKER-03` | Docker demo, fixture scans, reproducible example JSON | ✅ Complete (local verify + Docker smoke) |
| 04 | `DR-MAYS-AWS-DISCOVERY-04` | Authorized S3/IAM/SG/Lambda discovery, rules, JSON report | 🟡 Simulated foundation complete; live scan not executed |
| 05 | `DR-MAYS-DNA-GRAPH-05` | Verified resource links with provenance and graph tests | ⬜ Planned |
| 06 | `DR-MAYS-AI-REPORTS-06` | Opt-in model adapters, redaction, fallback, JSON/PDF report | ⬜ Planned |
| 07 | `DR-MAYS-CLOUD-DOCTOR-07` | Java Lambda, CDK synth/diff, approval-gated deploy | ⬜ Planned |
| 08 | `DR-MAYS-PORTFOLIO-READINESS-08` | CI, security review, observability, docs and demo | ⬜ Planned |

**MVP gate:** After phase 04, a reviewer should be able to run an offline demo, inspect the checks and reproduce a JSON diagnosis. The DNA graph, AI and AWS-hosted execution are subsequent increments.

## 🚀 Getting started

**Core domain, local REST API and Docker offline demo available; AWS discovery is not implemented yet.** JDK 25 is required. Maven is pinned through the wrapper, so no global Maven 3.9 installation is needed.

```bash
# Full reactor verification (compile + 115 tests + quality gates)
./mvnw -B clean verify

# Format and verify
./mvnw spotless:apply
./mvnw spotless:check
./mvnw checkstyle:check

# Run a single test class; -am also builds the uninstalled sibling module
./mvnw -pl dr-mays-core -am -Dtest=SeverityTest -Dsurefire.failIfNoSpecifiedTests=false test

# Start the local API (loopback only, no authentication)
./mvnw -B -DskipTests package
java -jar dr-mays-api/target/dr-mays-api-0.1.0-SNAPSHOT.jar

# Or run the complete Docker-backed offline demo
./scripts/docker-demo.sh
```

Endpoints: `POST /api/v1/diagnoses`, `GET /api/v1/rules`, `GET /actuator/health`. Contract and examples: `docs/api/README.md` and `docs/api/openapi.yaml`. Docker and fixtures: `docs/development/DOCKER_DEVELOPMENT.md` and `docs/development/OFFLINE_DEMO.md`.

Quality gates: Spotless/Google Java Format, Checkstyle, 179 tests, and JaCoCo with 85 % line / 75 % branch minimums. Current coverage: core 100 % / 98.08 %, API 97.83 % / 100 %, AWS discovery 95.35 % / 84.55 %. Reports are under each module's `target/site/jacoco/`.

The build and tests require no AWS credentials. The AWS discovery layer uses mocked SDK clients only; the first live scan requires a separate explicit approval. Details: `docs/development/AWS_DISCOVERY.md` and `docs/security/AWS_SCANNER_IAM.md`.

Planned run modes:

1. **Offline demo:** Load sanitized fixtures and run rules without AWS credentials or model access.
2. **Authorized local scan:** Use the standard AWS credential provider chain (for example, a selected `AWS_PROFILE`) and a deliberately narrow read-only IAM policy.
3. **Cloud Doctor:** Deploy only after reviewing the CDK diff, IAM permissions and estimated costs.

## 🔐 Security and privacy principles

- **Read-only by design:** Scanner code never invokes mutating AWS APIs. It does not execute fixes.
- **Local API only:** The REST API binds to `127.0.0.1` by default, has no authentication and must not be exposed publicly without a security review.
- **Docker demo only:** The container runs non-root with a read-only filesystem, all capabilities dropped and host publishing restricted to `127.0.0.1`; no registry push or public deployment is part of the demo.
- **AWS discovery is simulated:** DR-MAYS-04 uses mocked SDK clients. Live AWS access requires a separate approval for profile, account, region, services and resource cap; no default profile or write permissions are allowed.
- **Least privilege:** Explicit account, region, service and depth scope; no broad access simply for convenience. Note that some nominally read-only AWS APIs may expose sensitive data.
- **No secrets in source control:** No access keys, tokens, raw credentials or private configuration in the repository, logs, reports or model prompts.
- **Evidence-based results:** Every finding identifies a check, version, resource and supporting evidence. Every graph edge has a traceable source.
- **AI is optional and opt-in:** External model providers require explicit user approval and data minimization. Findings remain available without AI.
- **No automated infrastructure changes:** CDK deploy/destroy are separate, human-reviewed operations; never triggered by an analysis result.
- **Cost awareness:** Offline mode avoids AWS costs; cloud features are deliberately optional.

## 🧪 Engineering and quality gates

Each completed phase must include applicable **tests**, a brief **technical report**, updated **documentation** and a **clean Git commit** before work moves to the next phase. The foundation workflow has passed on GitHub Actions for commits `7c7f775` (run `41609325812`) and `45cf93d` (run `41609592971`). CI claims for newer commits require an actual run.

Planned evidence includes rule unit tests, REST contract tests, AWS adapter fixtures/mocks, Docker smoke tests, secret and dependency checks, and an offline end-to-end scan.

## 🤝 Contributions

The project begins as a focused portfolio reference. Contribution guidelines and issue templates can be added once the core architecture and licensing are established.

## 📄 License

Licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE) for the full text.

---

**Dr. Mays — AWS Intelligence Infrastructure Doctor**  
*We check your infrastructure — right down to its DNA!* 🩺🧬
