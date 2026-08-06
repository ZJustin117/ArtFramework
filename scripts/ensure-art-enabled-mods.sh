#!/usr/bin/env bash
# Ensure Amethyst enabled_mods.txt lists only ArtFramework.jar.
# Usage: ART_D1_SERIAL=... ./scripts/ensure-art-enabled-mods.sh
# Optional: ART_D2_SERIAL for dual (set ART_ENSURE_DUAL=1).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -f "$ROOT/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env.local"
  set +a
fi

: "${ART_D1_SERIAL:?ART_D1_SERIAL unset}"

REMOTE_DIR="/sdcard/Android/data/io.stamethyst/files/sts"
ENABLED="$REMOTE_DIR/enabled_mods.txt"
ART_PATH="/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/ArtFramework.jar"

ensure_one() {
  local serial="$1"
  echo "ensure-enabled-mods serial=$serial"
  adb -s "$serial" shell "test -f $REMOTE_DIR/mods_library/ArtFramework.jar" \
    || { echo "missing mods_library/ArtFramework.jar — push jar first"; return 1; }

  adb -s "$serial" shell "printf '%s\n' '$ART_PATH' > $ENABLED"
  echo "enabled_mods.txt:"
  adb -s "$serial" shell "cat $ENABLED"
}

ensure_one "$ART_D1_SERIAL"
if [[ -n "${ART_D2_SERIAL:-}" && "${ART_ENSURE_DUAL:-}" == "1" ]]; then
  ensure_one "$ART_D2_SERIAL"
fi
