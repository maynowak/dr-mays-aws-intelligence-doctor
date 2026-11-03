# 🩺 Dr. Mays — AWS Intelligence Infrastructure Doctor

> **We check your infrastructure — right down to its DNA!** 🧬
>
> *Understand the infrastructure. Detect the risks. Explain the diagnosis.*

**Project status:** 🟡 Concept / pre-implementation  
**Initial plan:** 3 November 2026  
**Project ID:** `DR-MAYS`  
**Type:** Independent, Java-first portfolio and reference project

> [!IMPORTANT]
> This repository is being planned. Features, modules, tests, CI pipelines and AWS deployments described below are **targets**, not claims of completed implementation. No AWS resources are provisioned by this README.

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

Exact dependency versions will be pinned and compatibility-tested during implementation.

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

## 📂 Proposed repository structure

```text
dr-mays-aws-intelligence-doctor/
├── README.md
├── pom.xml
├── compose.yaml
├── dr-mays-domain/     # Resource, Finding, Evidence, Relation, rules
├── dr-mays-aws/        # AWS SDK v2 discovery adapters
├── dr-mays-api/        # Spring Boot REST API
├── dr-mays-ai/         # Optional model adapters
├── dr-mays-infra/      # AWS CDK in Java (later)
├── dr-mays-demo/       # Offline fixtures and example scans
├── docs/               # Architecture decisions, reports, test evidence
└── .github/workflows/  # CI (later)
```

This structure is a **proposal**; directories will be introduced as their phases are implemented.

## 🗺️ Development roadmap

| Phase | Milestone | Target / acceptance evidence | Status |
| --- | --- | --- | --- |
| 01 | `DR-MAYS-JAVA-FOUNDATION-01` | Java 25 domain model, Maven build, unit tests, ADR | ⬜ Planned |
| 02 | `DR-MAYS-SPRING-API-02` | REST contract, validation, mock scans, API tests | ⬜ Planned |
| 03 | `DR-MAYS-LOCAL-DOCKER-03` | Docker demo, fixture scans, reproducible example JSON | ⬜ Planned |
| 04 | `DR-MAYS-AWS-DISCOVERY-04` | Authorized S3/IAM/SG/Lambda discovery, rules, JSON report | ⬜ Planned |
| 05 | `DR-MAYS-DNA-GRAPH-05` | Verified resource links with provenance and graph tests | ⬜ Planned |
| 06 | `DR-MAYS-AI-REPORTS-06` | Opt-in model adapters, redaction, fallback, JSON/PDF report | ⬜ Planned |
| 07 | `DR-MAYS-CLOUD-DOCTOR-07` | Java Lambda, CDK synth/diff, approval-gated deploy | ⬜ Planned |
| 08 | `DR-MAYS-PORTFOLIO-READINESS-08` | CI, security review, observability, docs and demo | ⬜ Planned |

**MVP gate:** After phase 04, a reviewer should be able to run an offline demo, inspect the checks and reproduce a JSON diagnosis. The DNA graph, AI and AWS-hosted execution are subsequent increments.

## 🚀 Getting started

**Not available yet:** The Java modules, Maven wrapper and Docker Compose environment have not been implemented. Setup and execution commands will be added only after they work in a clean environment.

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

Each completed phase must include applicable **tests**, a brief **technical report**, updated **documentation** and a **clean Git commit** before work moves to the next phase. CI claims will be added only after workflows actually run successfully.

Planned evidence includes rule unit tests, REST contract tests, AWS adapter fixtures/mocks, Docker smoke tests, secret and dependency checks, and an offline end-to-end scan.

## 🤝 Contributions

The project begins as a focused portfolio reference. Contribution guidelines and issue templates can be added once the core architecture and licensing are established.

## 📄 License

**To be selected before public code publication.** A `LICENSE` file and any required notices will be added explicitly. Until then, no open-source license is granted by this README alone.

---

**Dr. Mays — AWS Intelligence Infrastructure Doctor**  
*We check your infrastructure — right down to its DNA!* 🩺🧬
