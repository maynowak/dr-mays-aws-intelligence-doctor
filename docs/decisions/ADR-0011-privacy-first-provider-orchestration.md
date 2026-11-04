# ADR-0011: Privacy-First Provider Orchestration

- Status: Accepted
- Date: 2026-11-04
- Scope: `dr-mays-ai`, `dr-mays-api`
- Task: DR-MAYS-ORCHESTRATION-01

## Context

The existing AI layer sends minimized findings, evidence and verified DNA edges to a model adapter. It does not have an enforced privacy projection, a provider registry, server-side preview/approval, or a BYOK credential-reference contract. Discovery also confirmed that the current prompt can contain raw account, resource, VPC and security-group identifiers, and that OpenCode CLI defaults are unsuitable for live restricted execution.

The deterministic diagnosis engine must remain authoritative. No external provider may receive raw domain objects or unapproved infrastructure snapshots.

## Decision

1. **No new Maven module for ORCH-01.** Privacy, provider, approval and expertise components live in `dr-mays-ai` packages. `dr-mays-core` remains free of provider, Spring AI, OpenCode and credential types. A new provider-specific module is a later, separately approved step if runtime isolation requires it.
2. **Typed privacy boundary.** Remote providers accept only `SanitizedExpertiseRequest`. It contains aliases and allowlisted fields; it never contains `IntelligenceRequest`, `DiagnosticFinding`, `Evidence`, tags, attributes, account IDs, raw resource IDs, ARNs, emails, IPs or secrets.
3. **Server-side preview and approval.** A preview is generated from the exact serialized payload. Approval binds the payload hash plus provider ID, model ID, expertise ID and policy version. The payload is stored sealed server-side. Execution accepts only a preview ID plus approval token, never a client-supplied payload.
4. **Fail closed.** Missing, expired, mismatched, reused or invalid approval; invalid privacy payload; unknown provider/model/expertise; or missing credential reference prevents any provider call. There is no provider fallback and no retry with changed content.
5. **Mock-first.** The only enabled provider in this phase is the deterministic mock. Ollama and OpenCode descriptors exist but remain disabled. No real model call, model download or external data transfer occurs.
6. **OpenCode is not the regular inference path.** A disabled command-builder/mock adapter may exist to prove argv safety, sanitized environment and bounded execution. Live CLI execution is BLOCKED until OS-level isolation, deny-all tools, egress control, session cleanup and exit/output semantics are verified.
7. **BYOK references, not secrets.** Credentials are represented by logical references (`NONE`, `ENV`, `FILE`, `KEYRING`) with provider/model binding. No API key is accepted in REST bodies, URLs, logs, Git or the provider payload. No existing OpenCode/AWS credential is read or reused. Secret resolution is disabled/simulated in this phase.
8. **Reuse existing AI contracts.** `IntelligenceRequest`, `IntelligenceResult`, `FindingExplanation`, `Recommendation`, `EvidenceReference` and `IntelligenceResultValidator` remain canonical. A second diagnosis engine is forbidden.
9. **Additive API.** Existing diagnosis, DNA and `/api/v1/intelligence/explain` behavior stays intact. New expertise preview/approval/explain endpoints are mock-only and versioned. Preview never triggers a provider call.
10. **No anonymity claim.** The design minimizes and pseudonymizes; it does not prove complete anonymization. Residual re-identification and provider retention risks must be documented.

## Consequences

- The privacy boundary is testable without network access.
- Approval state is in-memory and suitable only for the loopback, single-instance API.
- No production provider is enabled; provider readiness, real BYOK and OpenCode remain separate verification gates.
- New code increases AI- and API-module coverage obligations and the minimum test count.
- Documentation must state clearly that ORCH-01 is a local, mock-verified privacy architecture, not a production data-transfer approval.

## Rejected alternatives

- Putting privacy/provider types in `dr-mays-core`: breaks the core boundary.
- Creating a new Maven module now: adds Docker/CI/reactor complexity without isolation benefit for mock-only scope.
- Client-supplied `approved=true` or client-computed hashes: forgeable and not bound to sealed bytes.
- Raw `IntelligenceRequest` to remote providers: violates the privacy invariant.
- Live OpenCode CLI as default: default allow-all tools, host shell authority, session persistence and unproven isolation.
- Storing API keys in request DTOs or environment scanning: leaks and bypasses explicit credential references.
