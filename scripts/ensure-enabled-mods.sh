#!/usr/bin/env bash
# Ensure Amethyst enabled_mods.txt lists SpireUI.jar (before CrossSpire if present).
# Usage: SPIREUI_D1_SERIAL=... ./scripts/ensure-enabled-mods.sh
# Optional: SPIREUI_D2_SERIAL for dual.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -f "$ROOT/.env.local" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/.env.local"
  set +a
fi

: "${SPIREUI_D1_SERIAL:?SPIREUI_D1_SERIAL unset}"

REMOTE_DIR="/sdcard/Android/data/io.stamethyst/files/sts"
ENABLED="$REMOTE_DIR/enabled_mods.txt"
SPIREUI_PATH="/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/SpireUI.jar"
CROSS_PATH="/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/CrossSpire.jar"

ensure_one() {
  local serial="$1"
  echo "ensure-enabled-mods serial=$serial"
  adb -s "$serial" shell "test -f $REMOTE_DIR/mods_library/SpireUI.jar" \
    || { echo "missing mods_library/SpireUI.jar — push jar first"; return 1; }

  local has_cross=0
  if adb -s "$serial" shell "test -f $REMOTE_DIR/mods_library/CrossSpire.jar" 2>/dev/null; then
    has_cross=1
  fi

  if [[ "$has_cross" -eq 1 ]]; then
    adb -s "$serial" shell "printf '%s\n' '$SPIREUI_PATH' '$CROSS_PATH' > $ENABLED"
  else
    adb -s "$serial" shell "printf '%s\n' '$SPIREUI_PATH' > $ENABLED"
  fi
  echo "enabled_mods.txt:"
  adb -s "$serial" shell "cat $ENABLED"
}

ensure_one "$SPIREUI_D1_SERIAL"
if [[ -n "${SPIREUI_D2_SERIAL:-}" && "${SPIREUI_ENSURE_DUAL:-}" == "1" ]]; then
  ensure_one "$SPIREUI_D2_SERIAL"
fi
