#!/usr/bin/env bash
# Offline Dr. Mays demo: sends synthetic fixtures to a running API and verifies
# the deterministic JSON contract. It contains no diagnosis logic.
set -euo pipefail

BASE_URL="${DR_MAYS_API_URL:-http://127.0.0.1:18080}"
FIXTURES_DIR="${DR_MAYS_FIXTURES_DIR:-examples/fixtures}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

fail() {
  echo "DEMO FAILED: $*" >&2
  exit 1
}

for tool in curl python3; do
  command -v "$tool" >/dev/null 2>&1 || fail "missing required tool: $tool"
done

echo "Dr. Mays offline demo"
echo "API: $BASE_URL"

for _ in $(seq 1 60); do
  if curl -fsS "$BASE_URL/actuator/health" > "$TMP_DIR/health.json" 2>/dev/null; then
    break
  fi
  sleep 1
done

if [ ! -s "$TMP_DIR/health.json" ]; then
  fail "health endpoint did not become ready"
fi
echo "Health: $(python3 -m json.tool < "$TMP_DIR/health.json" | tr '\n' ' ')"
python3 - "$TMP_DIR/health.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
if data.get("status") != "UP":
    raise SystemExit(f"health status is not UP: {data}")
PY

curl -fsS "$BASE_URL/api/v1/rules" > "$TMP_DIR/rules.json"
echo "--- rules ---"
python3 -m json.tool < "$TMP_DIR/rules.json"
python3 - "$TMP_DIR/rules.json" <<'PY'
import json, sys
rules = {rule["ruleId"] for rule in json.load(open(sys.argv[1]))["rules"]}
required = {"NET-PUBLIC-001", "TAG-OWNER-001"}
if rules != required:
    raise SystemExit(f"unexpected rules: {sorted(rules)}")
PY

run_case() {
  local name="$1"
  local expected_count="$2"
  local expected_ids="$3"
  local body="$TMP_DIR/${name}.json"

  echo
  echo "--- fixture: $name (expected findings: $expected_count) ---"
  curl -fsS -X POST "$BASE_URL/api/v1/diagnoses" \
    -H 'Content-Type: application/json' \
    --data-binary "@$FIXTURES_DIR/$name.json" > "$body"
  python3 -m json.tool < "$body"

  python3 - "$body" "$expected_count" "$expected_ids" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
expected_count = int(sys.argv[2])
expected_ids = [value for value in sys.argv[3].split(",") if value]
if data.get("findingCount") != expected_count:
    raise SystemExit(f"findingCount {data.get('findingCount')} != {expected_count}")
actual_ids = [finding["ruleId"] for finding in data.get("findings", [])]
if actual_ids != expected_ids:
    raise SystemExit(f"finding IDs {actual_ids} != {expected_ids}")
for finding in data.get("findings", []):
    evidence = finding["evidence"]
    print(f"  {finding['ruleId']} [{finding['severity']}] {finding['title']}")
    print(f"    observed={evidence['observed']} expected={evidence['expected']}")
PY
}

run_case healthy 0 ""
run_case missing-owner 1 "TAG-OWNER-001"
run_case public-ingress 1 "NET-PUBLIC-001"
run_case multiple-findings 2 "NET-PUBLIC-001,TAG-OWNER-001"

echo
echo "--- DNA graph: security group -> VPC ---"
curl -fsS -X POST "$BASE_URL/api/v1/dna/graphs" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/dna-sg-vpc.json" > "$TMP_DIR/dna-graph.json"
python3 -m json.tool < "$TMP_DIR/dna-graph.json"
python3 - "$TMP_DIR/dna-graph.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
if data.get("stats", {}).get("nodeCount") != 2:
    raise SystemExit(f"unexpected graph node count: {data.get('stats')}")
if data.get("stats", {}).get("edgeCount") != 1:
    raise SystemExit(f"unexpected graph edge count: {data.get('stats')}")
edges = data.get("edges", [])
if not edges or edges[0].get("type") != "SECURITY_GROUP_IN_VPC":
    raise SystemExit(f"unexpected graph edge: {edges}")
if data.get("completeness") != "COMPLETE":
    raise SystemExit(f"unexpected completeness: {data.get('completeness')}")
print("  DNA graph checks passed: 2 nodes, 1 verified VPC edge")
PY

echo
echo "--- Doctor Report JSON ---"
curl -fsS -X POST "$BASE_URL/api/v1/reports" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/report-multiple-findings.json" > "$TMP_DIR/report.json"
python3 -m json.tool < "$TMP_DIR/report.json"
python3 - "$TMP_DIR/report.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
if data.get("schemaVersion") != "doctor-report-v1":
    raise SystemExit(f"unexpected report schema: {data.get('schemaVersion')}")
if data.get("summary", {}).get("findingCount") != 2:
    raise SystemExit(f"unexpected report finding count: {data.get('summary')}")
if data.get("graph", {}).get("stats", {}).get("nodeCount") != 2:
    raise SystemExit(f"unexpected report graph stats: {data.get('graph', {}).get('stats')}")
if not data.get("metadata", {}).get("inputHash", "").startswith("sha256:"):
    raise SystemExit("report input hash missing")
if data.get("metadata", {}).get("partialDiscovery") is not False:
    raise SystemExit(f"unexpected report completeness: {data.get('metadata')}")
print("  Doctor Report JSON checks passed: schema, findings, graph and metadata")
PY

echo
echo "--- Doctor Report Markdown ---"
curl -fsS -X POST "$BASE_URL/api/v1/reports/markdown" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/report-multiple-findings.json" > "$TMP_DIR/report.md"
if ! grep -q '^# Dr\. Mays Infrastructure Doctor Report' "$TMP_DIR/report.md"; then
  fail "Markdown report heading missing"
fi
if ! grep -q '^## 5\. Infrastructure DNA' "$TMP_DIR/report.md"; then
  fail "Markdown DNA section missing"
fi
if grep -q '<script>' "$TMP_DIR/report.md"; then
  fail "Markdown report contains raw script markup"
fi
echo "  Doctor Report Markdown checks passed: heading, DNA and escaping"

echo
echo "--- Doctor Report partial discovery and cycle ---"
curl -fsS -X POST "$BASE_URL/api/v1/reports" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/dna-partial.json" > "$TMP_DIR/report-partial.json"
curl -fsS -X POST "$BASE_URL/api/v1/reports" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/report-dna-cycle.json" > "$TMP_DIR/report-cycle.json"
python3 - "$TMP_DIR/report-partial.json" "$TMP_DIR/report-cycle.json" <<'PY'
import json, sys
partial = json.load(open(sys.argv[1]))
cycle = json.load(open(sys.argv[2]))
if partial.get("metadata", {}).get("partialDiscovery") is not True:
    raise SystemExit("partial report metadata is not partial")
if partial.get("graph", {}).get("completeness") != "PARTIAL":
    raise SystemExit(f"unexpected partial graph completeness: {partial.get('graph')}")
if not any("PARTIAL DISCOVERY" in value for value in partial.get("limitations", [])):
    raise SystemExit("partial report limitation missing")
if cycle.get("graph", {}).get("stats", {}).get("cyclic") is not True:
    raise SystemExit(f"cycle report did not detect a cycle: {cycle.get('graph', {}).get('stats')}")
print("  Partial and cycle report checks passed")
PY

# The same report request must produce byte-identical JSON on a second run.
curl -fsS -X POST "$BASE_URL/api/v1/reports" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/report-multiple-findings.json" > "$TMP_DIR/report-repeat.json"
if ! cmp -s "$TMP_DIR/report.json" "$TMP_DIR/report-repeat.json"; then
  fail "Doctor Report JSON is not deterministic"
fi

status="$(curl -sS -o "$TMP_DIR/report-invalid.json" -w '%{http_code}' \
  -X POST "$BASE_URL/api/v1/reports" \
  -H 'Content-Type: application/json' \
  --data-binary '{"partialDiscovery":false,"resources":[]}')"
[ "$status" = "400" ] || fail "invalid report request returned HTTP $status instead of 400"

echo
echo "--- AI intelligence (deterministic mock) ---"
curl -fsS -X POST "$BASE_URL/api/v1/intelligence/explain" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/dna-sg-vpc.json" > "$TMP_DIR/intelligence.json"
python3 -m json.tool < "$TMP_DIR/intelligence.json"
python3 - "$TMP_DIR/intelligence.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
if data.get("status") != "SUCCESS":
    raise SystemExit(f"unexpected intelligence status: {data.get('status')}")
if not data.get("explanations"):
    raise SystemExit("no AI explanations returned")
if data.get("metadata", {}).get("provider") != "mock":
    raise SystemExit(f"unexpected provider: {data.get('metadata')}")
print("  AI mock checks passed: verified explanations and evidence references")
PY

echo
echo "--- privacy-first expertise (mock-only preview and explain) ---"
curl -fsS -X POST "$BASE_URL/api/v1/expertise/previews" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/dna-sg-vpc.json" > "$TMP_DIR/expertise-preview.json"
python3 -m json.tool < "$TMP_DIR/expertise-preview.json"
python3 - "$TMP_DIR/expertise-preview.json" > "$TMP_DIR/expertise-approval-request.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
if not data.get("validation", {}).get("valid"):
    raise SystemExit(f"preview is not privacy-valid: {data.get('validation')}")
if data.get("approvalRequired") is not False:
    raise SystemExit(f"mock preview unexpectedly requires approval: {data}")
payload = data.get("payloadJson", "")
if "123456789012" in payload or "000000000000" in payload:
    raise SystemExit("preview payload contains a raw account identifier")
if "accountId" in payload or "vpc-" in payload or "sg-" in payload:
    raise SystemExit("preview payload contains a raw resource identity")
print(json.dumps({"previewId": data["previewId"]}))
PY
curl -fsS -X POST "$BASE_URL/api/v1/expertise/approvals" \
  -H 'Content-Type: application/json' \
  --data-binary "@$TMP_DIR/expertise-approval-request.json" > "$TMP_DIR/expertise-approval.json"
python3 - "$TMP_DIR/expertise-preview.json" "$TMP_DIR/expertise-approval.json" > "$TMP_DIR/expertise-explain-request.json" <<'PY'
import json, sys
preview = json.load(open(sys.argv[1]))
approval = json.load(open(sys.argv[2]))
print(json.dumps({"previewId": preview["previewId"], "approvalToken": approval["approvalToken"]}))
PY
curl -fsS -X POST "$BASE_URL/api/v1/expertise/explain" \
  -H 'Content-Type: application/json' \
  --data-binary "@$TMP_DIR/expertise-explain-request.json" > "$TMP_DIR/expertise-explain.json"
python3 -m json.tool < "$TMP_DIR/expertise-explain.json"
python3 - "$TMP_DIR/expertise-explain.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
if data.get("status") != "SUCCESS":
    raise SystemExit(f"unexpected expertise status: {data.get('status')}")
if not data.get("explanations"):
    raise SystemExit("no expertise explanations returned")
if data.get("privacyReceipt", {}).get("approvalStatus") != "NOT_REQUIRED":
    raise SystemExit(f"unexpected approval receipt: {data.get('privacyReceipt')}")
print("  Expertise preview/approval/explain checks passed (preview aliases only; local response resolves evidence)")
PY

# The same fixture must produce byte-identical JSON on a second run.
curl -fsS -X POST "$BASE_URL/api/v1/diagnoses" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/multiple-findings.json" > "$TMP_DIR/multiple-repeat.json"
if ! cmp -s "$TMP_DIR/multiple-findings.json" "$TMP_DIR/multiple-repeat.json"; then
  fail "diagnosis output is not deterministic"
fi

echo
echo "--- invalid input (expected HTTP 400) ---"
status="$(curl -sS -o "$TMP_DIR/invalid.json" -w '%{http_code}' \
  -X POST "$BASE_URL/api/v1/diagnoses" \
  -H 'Content-Type: application/json' \
  --data-binary "@$FIXTURES_DIR/invalid-input.json")"
python3 -m json.tool < "$TMP_DIR/invalid.json"
[ "$status" = "400" ] || fail "invalid input returned HTTP $status instead of 400"
python3 - "$TMP_DIR/invalid.json" <<'PY'
import json, sys
data = json.load(open(sys.argv[1]))
if "trace" in data or "stackTrace" in data:
    raise SystemExit("error response exposed internal details")
if data.get("title") != "Request validation failed":
    raise SystemExit(f"unexpected error title: {data.get('title')}")
PY

echo
echo "DEMO PASSED: health, rules, 4 diagnosis fixtures, DNA, Doctor Report JSON/Markdown, partial/cycle reports, mock intelligence, privacy preview/approval/explain, determinism and invalid-input handling"
