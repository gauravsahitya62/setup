#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${1:-http://localhost:8080}"
curl -fsS "${BASE_URL}/actuator/health/liveness" >/dev/null
curl -fsS "${BASE_URL}/actuator/health/readiness" >/dev/null
echo "Health checks passed against ${BASE_URL}"
