# 🩺 Dr. Mays — AWS Intelligence Infrastructure Doctor

> **We check your infrastructure — right down to its DNA!** 🧬
>
> *Understand the infrastructure. Detect the risks. Explain the diagnosis.*

**Project status:** 🟡 Domain foundation implemented / AWS discovery not started<br>
**Initial plan:** 3 November 2026<br>
**Project ID:** `DR-MAYS`<br>
**Type:** Independent, Java-first portfolio and reference project

> [!IMPORTANT]
> The Java 25 Maven foundation and the core diagnostic domain are implemented and verified locally. AWS discovery, REST API, AI, Docker and deployments described below remain **targets**, not completed features. No AWS resources are provisioned by this README.

## 🎯 Vision

**Dr. Mays** is a planned, read-only AWS infrastructure diagnostics application. It will inspect explicitly authorized AWS environments, produce reproducible technical findings, identify *verifiable* relationships between resources, and optionally explain the results with AI.

The key idea is **Infrastructure DNA**: infrastructure is more than a list of resources. A Lambda function uses an IAM role; a security group affects access to an instance; resources and permissions form relationships that matter when diagnosing risks.

**Technical evidence first. AI-assisted explanation second. No autonomous remediation.**

## 🔬 Planned capabilities

| Capability | Initial scope |
| --- | --- |
| **AWS discovery** | Read-only inventory of selected S3, IAM, EC2 security group and Lambda metadata |
| **Rule engine** | Deterministic checks with stable rule IDs, severity, evidence and recommendations |
| **Infrastructure DNA** | Small graph of resource nodes and *verified* relationships |
| **Diagnosis report** | Machine-readable JSON with findings, affected resources and priorities |
| **AI interpretation** | Optional summarized findings via a replaceable model adapter; not the source of truth |
| **Local demo** | Docker-based demonstration with fixtures and **no AWS account required** |
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
├── dr-mays-aws/            # AWS SDK v2 discovery adapters (later)
├── dr-mays-api/            # Spring Boot REST API (later)
├── dr-mays-ai/             # Optional model adapters (later)
├── dr-mays-worker/         # Scheduled or Lambda execution (later)
├── dr-mays-infrastructure/ # AWS CDK in Java (later)
├── docker/                 # Local demo environment (later)
├── examples/               # Example scans and reports (later)
├── docs/                   # Architecture, decisions, security, reports
├── scripts/                # Development helpers (later)
└── .github/workflows/      # CI pipeline
```

Only the core domain foundation and its tests are implemented today. Other module and support directories are intentionally reserved for their phases.

## 🗺️ Development roadmap

| Phase | Milestone | Target / acceptance evidence | Status |
| --- | --- | --- | --- |
| 00 | `DR-MAYS-00` | Java 25 Maven reactor, seven modules, tests, CI workflow, audit baseline | ✅ Complete (local verify) |
| 01 | `DR-MAYS-JAVA-FOUNDATION-01` | Java 25 domain model, Maven build, unit tests, ADR | ✅ Complete (local verify, 63 tests) |
| 02 | `DR-MAYS-SPRING-API-02` | REST contract, validation, mock scans, API tests | ⬜ Planned |
| 03 | `DR-MAYS-LOCAL-DOCKER-03` | Docker demo, fixture scans, reproducible example JSON | ⬜ Planned |
| 04 | `DR-MAYS-AWS-DISCOVERY-04` | Authorized S3/IAM/SG/Lambda discovery, rules, JSON report | ⬜ Planned |
| 05 | `DR-MAYS-DNA-GRAPH-05` | Verified resource links with provenance and graph tests | ⬜ Planned |
| 06 | `DR-MAYS-AI-REPORTS-06` | Opt-in model adapters, redaction, fallback, JSON/PDF report | ⬜ Planned |
| 07 | `DR-MAYS-CLOUD-DOCTOR-07` | Java Lambda, CDK synth/diff, approval-gated deploy | ⬜ Planned |
| 08 | `DR-MAYS-PORTFOLIO-READINESS-08` | CI, security review, observability, docs and demo | ⬜ Planned |

**MVP gate:** After phase 04, a reviewer should be able to run an offline demo, inspect the checks and reproduce a JSON diagnosis. The DNA graph, AI and AWS-hosted execution are subsequent increments.

## 🚀 Getting started

**Domain foundation available; AWS discovery and the offline demo are not implemented yet.** JDK 25 is required. Maven is pinned through the wrapper, so no global Maven 3.9 installation is needed.

```bash
# Full reactor verification (compile + 63 tests)
./mvnw -B clean verify

# Run a single test class; -am also builds the uninstalled sibling module
./mvnw -pl dr-mays-core -am -Dtest=SeverityTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The build and domain tests require no AWS credentials. The offline demo, API and scan commands will be added when those phases are implemented.

Planned run modes:

1. **Offline demo:** Load sanitized fixtures and run rules without AWS credentials or model access.
2. **Authorized local scan:** Use the standard AWS credential provider chain (for example, a selected `AWS_PROFILE`) and a deliberately narrow read-only IAM policy.
3. **Cloud Doctor:** Deploy only after reviewing the CDK diff, IAM permissions and estimated costs.

## 🔐 Security and privacy principles

- **Read-only by design:** Scanner code never invokes mutating AWS APIs. It does not execute fixes.
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
