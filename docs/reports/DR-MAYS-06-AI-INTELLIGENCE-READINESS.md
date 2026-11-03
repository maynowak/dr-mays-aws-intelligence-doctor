# DR-MAYS-06 — AI Intelligence Engine — Readiness Report

- Date: 2026-11-03
- Repository: `/home/dci-student/projects/dr-mays-aws-intelligence-doctor`
- Branch: `feature/dr-mays-spring-ai-intelligence-06`
- Base commit: `2825cac4a8ed592b30a571e7b626fc6de28f1739`
- Local status: **GREEN**
- Real local model inference: **NOT VERIFIED** (no Ollama model installed)
- External model calls: **NOT EXECUTED**
- CI status: **NOT VERIFIED** (no push)

## 1. AI architecture

The API derives findings and DNA edges server-side, builds an `IntelligenceRequest`, calls the configured analyzer, validates the structured result, and maps it to versioned DTOs. The deterministic diagnosis remains authoritative.

## 2. Versions

| Component | Version |
| --- | --- |
| Java | OpenJDK 25.0.4.1 |
| Maven | 3.9.16 |
| Spring Boot | 4.1.2 |
| Spring AI | 2.0.2 |
| Ollama CLI | 0.32.1 (no model installed) |

## 3. Adapters

- `MockIntelligenceAnalyzer` — default, deterministic, offline.
- `OllamaIntelligenceAnalyzer` — opt-in Spring AI/Ollama adapter using a narrow `ChatCompletion` transport.
- `SpringAiChatCompletion` — `ChatClient` integration; no tools.
- `IntelligenceConfiguration` — default mock; Ollama only with `dr.mays.ai.enabled=true`.

Provider isolation: Ollama/Spring AI classes are confined to `dr-mays-ai`; API imports only contracts.

## 4. Structured output and validation

- JSON output includes summary, explanations, recommendations, evidence references, verified relationships, uncertainties, metadata and status.
- Validator rejects unknown findings/resources/evidence, unverified relationships, duplicates and blank summaries.
- Malformed output → `INVALID_RESPONSE`; unavailable connection → `UNAVAILABLE`; blank output → `UNAVAILABLE`.
- AI cannot add technical findings or modify deterministic results.

## 5. Prompting and privacy

- Versioned prompt `prompts/intelligence-v1.txt`, max 32,000 chars.
- Only minimized findings/evidence/relationships are sent; tags and names are untrusted.
- Prompt injection is addressed in instructions; no secrets, object contents or credentials.
- No raw prompt/response logging.
- See `docs/development/AI_INTELLIGENCE.md` and `docs/security/AI_MODEL_SECURITY.md`.

## 6. API

`POST /api/v1/intelligence/explain` accepts synthetic resources + optional `partialDiscovery`, runs deterministic diagnosis/DNA, then the configured analyzer. Default is mock; Ollama requires explicit opt-in. Existing validation, 100-resource limit and ProblemDetail behavior apply. OpenAPI updated.

## 7. Tests and coverage

| Module | Tests | Line | Branch |
| --- | ---: | ---: | ---: |
| Core | 86 | 99.36 % | 95.65 % |
| API | 84 | 98.83 % | 83.87 % |
| AWS | 64 | 95.25 % | 83.93 % |
| AI | 29 | 93.58 % | 82.86 % |
| **Total** | **263** | — | — |

All 223 prior tests retained; 40 new tests (29 AI + 11 API).

## 8. Verification

- `./mvnw -B clean verify`: all 8 modules `SUCCESS`.
- Spotless PASS; Checkstyle 0 violations.
- Docker regression: health, rules, diagnosis fixtures, DNA graph, **mock intelligence**, invalid input, non-root/read-only/cap-drop, clean stop.
- No model or AWS call during Maven verify.

## 9. Git commits

| Step | Commit | Message |
| --- | --- | --- |
| 06A/B/C/E/F/G | `30838b0` | `feat: add AI intelligence contracts and mock analyzer` |
| 06D/H | `569a6c7` | `feat: add local AI intelligence REST endpoint` |
| 06I/J | current HEAD | Documentation, readiness and audit close-out |

No push, merge, deployment, external model call or AWS call was performed.

## 10. Open questions and risks

- Real Ollama inference was not executed; no model is installed. Model quality/latency is unverified.
- Spring AI/Ollama adapter is compiled and unit-tested with fake completions, not a live provider.
- Prompt-injection resistance is defense-in-depth, not a formal proof.
- AI recommendations remain advisory and are not merged into technical findings.
- CI has no run for this branch.

## 11. Next phase recommendation

DR-MAYS-07 can add the first explicitly approved live model verification with a local Ollama model and narrow data. Keep external providers disabled and continue validating all model output against deterministic evidence.

## 12. Recovery point

- Branch: `feature/dr-mays-spring-ai-intelligence-06`
- Base: `2825cac4a8ed592b30a571e7b626fc6de28f1739`
- Last implementation commit: `569a6c7`
- Final commit: current HEAD (`git log -1`)
- Resume with `git status` and `./mvnw -B clean verify`.
