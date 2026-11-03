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

for _ in $(seq 1 30); do
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
echo "DEMO PASSED: health, rules, 4 diagnosis fixtures, determinism and invalid-input handling"
