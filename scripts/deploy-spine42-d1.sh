#!/usr/bin/env bash
# Push optional developer-only Spine42 runtime and STS2 assets to D1.
# These files are not mods and are never written to mods_library.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f .env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

: "${ART_D1_SERIAL:?ART_D1_SERIAL unset}"
: "${ART_STS2_ASSET_JAR:?ART_STS2_ASSET_JAR unset}"
: "${ART_SPINE42_RUNTIME_JAR:?ART_SPINE42_RUNTIME_JAR unset}"

[[ -f "$ART_STS2_ASSET_JAR" ]] || { echo "error: missing asset jar: $ART_STS2_ASSET_JAR" >&2; exit 1; }
[[ -f "$ART_SPINE42_RUNTIME_JAR" ]] || { echo "error: missing runtime jar: $ART_SPINE42_RUNTIME_JAR" >&2; exit 1; }

REMOTE_DIR="${ART_D1_ASSET_DIR:-/sdcard/Android/data/io.stamethyst/files/sts/art-assets}"
adb -s "$ART_D1_SERIAL" shell mkdir -p "$REMOTE_DIR"
adb -s "$ART_D1_SERIAL" push "$ART_STS2_ASSET_JAR" "$REMOTE_DIR/Sts2Assets.jar"
adb -s "$ART_D1_SERIAL" push "$ART_SPINE42_RUNTIME_JAR" "$REMOTE_DIR/ArtFramework-Spine42Runtime.jar"

echo "D1 asset directory: $REMOTE_DIR"
echo "Assets: $(stat -c '%s bytes' "$ART_STS2_ASSET_JAR") $(sha256sum "$ART_STS2_ASSET_JAR" | cut -d' ' -f1)"
echo "Runtime: $(stat -c '%s bytes' "$ART_SPINE42_RUNTIME_JAR") $(sha256sum "$ART_SPINE42_RUNTIME_JAR" | cut -d' ' -f1)"
