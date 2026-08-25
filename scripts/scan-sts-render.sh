#!/usr/bin/env bash
# Produce the NRCC static STS render inventory. Output is gitignored evidence.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env.local"
  set +a
fi

: "${ART_STS_JAR:?ART_STS_JAR unset — copy .env.example to .env.local}"
exec python3 "$ROOT/tools/nrcc/scan_sts_render.py" \
  --sts-jar "$ART_STS_JAR" \
  --source-root "$ROOT/src/main/java" \
  "$@"
