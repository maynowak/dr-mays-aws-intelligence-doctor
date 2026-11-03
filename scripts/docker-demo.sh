#!/usr/bin/env bash
# Build and run the local container, execute the offline demo, then stop it.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

cleanup() {
  docker compose down --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker compose config >/dev/null
docker compose build
docker compose up -d

DR_MAYS_API_URL="http://127.0.0.1:18080" ./scripts/demo.sh

echo "--- container ---"
docker compose ps
echo "--- security ---"
docker inspect --format 'User={{.Config.User}} ReadonlyRootfs={{.HostConfig.ReadonlyRootfs}} CapDrop={{.HostConfig.CapDrop}} SecurityOpt={{.HostConfig.SecurityOpt}}' dr-mays-api-demo
