# Doctor Reports V1

## Purpose
Doctor Reports aggregate the existing deterministic diagnosis and Infrastructure DNA into a local, evidence-based technical report. The report layer is an aggregator; it is not a second diagnosis engine.

## Endpoints
- `POST /api/v1/reports` — JSON `doctor-report-v1`.
- `POST /api/v1/reports/markdown` — escaped Markdown rendered from the same report model.

Both require `application/json` with:
```json
{
  "partialDiscovery": false,
  "resources": []
}
```
`resources` is bounded to 100; `partialDiscovery` is required so an incomplete snapshot cannot silently be labelled complete.

## Data flow
```text
validated resources
  -> DiagnosisService.diagnoseSnapshot (one engine run)
  -> DiagnosisResult
  -> InfrastructureGraphBuilder (same resource list)
  -> GraphBuildResult
  -> DoctorReportAssembler
  -> JSON DTOs or Markdown renderer
```

## Guarantees
- Findings, evidence and recommendations come only from `DiagnosisResult`.
- Graph relationships come only from `GraphBuildResult`; referenced nodes are not inventoried and no reachability is inferred.
- All outcomes are reported, including `NO_FINDING` and `NOT_APPLICABLE`.
- `PARTIAL DISCOVERY — RESULTS DO NOT COVER UNKNOWN RESOURCES` is emitted for partial input.
- No AI/provider/credential/AWS call or persistence occurs.
- JSON is bounded to 1 MiB and Markdown to 256 KiB; overflow is a 413 ProblemDetail.
- Markdown escapes untrusted values and never emits raw HTML or input-derived links.
- Ordering is deterministic; no timestamps, random IDs or unordered maps.

## Limits
The report intentionally contains local resource identifiers, tags, attributes and evidence. Treat exported reports as sensitive. Live-data redaction is a separate requirement before connecting real AWS discovery.

## Out of scope
PDF/HTML export, authenticated report storage, AI commentary, multi-report history and public API exposure.
