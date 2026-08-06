#!/usr/bin/env bash
# Run developer-only STS2 asset bundle checks. Not part of the standard Gradle test gate.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

if [[ -f .env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

if [[ -z "${ART_STS2_ASSET_JAR:-}" && -n "${ART_STS2_ROOT:-}" ]]; then
  ./scripts/package-sts2-assets.sh
  export ART_STS2_ASSET_JAR="$ROOT/build/dev/Sts2Assets.jar"
fi

: "${ART_STS2_ASSET_JAR:?ART_STS2_ASSET_JAR or ART_STS2_ROOT is required}"
python3 "$ROOT/tests/spine42-assets/test_bundle.py"

if [[ -n "${ART_SPINE42_RUNTIME_JAR:-}" && -n "${ART_STS_JAR:-}" && -f "$ART_SPINE42_RUNTIME_JAR" ]]; then
  CLASS_DIR="$ROOT/agent-tmp/spine42-assets-classes"
  mkdir -p "$CLASS_DIR"
  javac -source 8 -target 8 -cp "$ROOT/build/classes/java/main:$ART_STS_JAR:$ART_SPINE42_RUNTIME_JAR" \
    -d "$CLASS_DIR" "$ROOT/tests/spine42-assets/Spine42LoadSmoke.java"
  java -cp "$CLASS_DIR:$ROOT/build/classes/java/main:$ART_STS_JAR:$ART_SPINE42_RUNTIME_JAR" Spine42LoadSmoke
else
  echo "SKIP: set ART_SPINE42_RUNTIME_JAR and ART_STS_JAR for real Spine42 load smoke"
fi
