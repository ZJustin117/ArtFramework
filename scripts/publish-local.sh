#!/usr/bin/env bash
# Build SpireUI.jar and optionally install for a consumer (e.g. CrossSpire).
#
# Env:
#   SPIREUI_* jar keys — via with-env / .env.local
#   SPIREUI_INSTALL_DIR — if set, copy SpireUI.jar here after build
#   SPIREUI_CONSUMER_JAR — if set, also copy/overwrite this exact file path
#
# Prints the absolute artifact path on success.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

./scripts/with-env.sh jar

ARTIFACT="$ROOT/build/libs/SpireUI.jar"
if [[ ! -f "$ARTIFACT" ]]; then
  echo "error: missing $ARTIFACT" >&2
  exit 1
fi

if [[ -n "${SPIREUI_INSTALL_DIR:-}" ]]; then
  mkdir -p "$SPIREUI_INSTALL_DIR"
  cp -f "$ARTIFACT" "$SPIREUI_INSTALL_DIR/SpireUI.jar"
  echo "Installed: $SPIREUI_INSTALL_DIR/SpireUI.jar"
fi

if [[ -n "${SPIREUI_CONSUMER_JAR:-}" ]]; then
  mkdir -p "$(dirname "$SPIREUI_CONSUMER_JAR")"
  cp -f "$ARTIFACT" "$SPIREUI_CONSUMER_JAR"
  echo "Installed: $SPIREUI_CONSUMER_JAR"
fi

echo "Artifact: $ARTIFACT"
