#!/usr/bin/env bash
# Build ArtFramework.jar and optionally install for a consumer (e.g. CrossSpire).
#
# Env:
#   ART_* jar keys — via with-art-env / .env.local
#   ART_INSTALL_DIR — if set, copy ArtFramework.jar here after build
#   ART_CONSUMER_JAR — if set, also copy/overwrite this exact file path
#
# Prints the absolute artifact path on success.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

./scripts/with-art-env.sh jar

ARTIFACT="$ROOT/build/libs/ArtFramework.jar"
if [[ ! -f "$ARTIFACT" ]]; then
  echo "error: missing $ARTIFACT" >&2
  exit 1
fi

if [[ -n "${ART_INSTALL_DIR:-}" ]]; then
  mkdir -p "$ART_INSTALL_DIR"
  cp -f "$ARTIFACT" "$ART_INSTALL_DIR/ArtFramework.jar"
  echo "Installed: $ART_INSTALL_DIR/ArtFramework.jar"
fi

if [[ -n "${ART_CONSUMER_JAR:-}" ]]; then
  mkdir -p "$(dirname "$ART_CONSUMER_JAR")"
  cp -f "$ARTIFACT" "$ART_CONSUMER_JAR"
  echo "Installed: $ART_CONSUMER_JAR"
fi

echo "Artifact: $ARTIFACT"
