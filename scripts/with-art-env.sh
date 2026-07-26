#!/usr/bin/env bash
# Load .env.local (ART_*) and run gradle with -P jar paths.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f .env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

: "${ART_STS_JAR:?ART_STS_JAR unset — copy .env.example to .env.local}"
: "${ART_BASEMOD_JAR:?ART_BASEMOD_JAR unset}"
: "${ART_MODTHESPIRE_JAR:?ART_MODTHESPIRE_JAR unset}"

exec ./gradlew "$@" \
  -PstsJar="$ART_STS_JAR" \
  -PbaseModJar="$ART_BASEMOD_JAR" \
  -PmodTheSpireJar="$ART_MODTHESPIRE_JAR"
