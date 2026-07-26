#!/usr/bin/env bash
# Load .env.local (SPIREUI_*) and run gradle with -P jar paths.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f .env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

: "${SPIREUI_STS_JAR:?SPIREUI_STS_JAR unset — copy .env.example to .env.local}"
: "${SPIREUI_BASEMOD_JAR:?SPIREUI_BASEMOD_JAR unset}"
: "${SPIREUI_MODTHESPIRE_JAR:?SPIREUI_MODTHESPIRE_JAR unset}"

exec ./gradlew "$@" \
  -PstsJar="$SPIREUI_STS_JAR" \
  -PbaseModJar="$SPIREUI_BASEMOD_JAR" \
  -PmodTheSpireJar="$SPIREUI_MODTHESPIRE_JAR"
