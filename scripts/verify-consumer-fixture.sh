#!/usr/bin/env bash
# Compile a minimal external consumer against the published ART jar.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

"$ROOT/scripts/with-art-env.sh" jar

ARTIFACT="$ROOT/build/libs/ArtFramework.jar"
OUT="$ROOT/build/consumer-fixture"
rm -rf "$OUT"
mkdir -p "$OUT"

javac -source 8 -target 8 -cp "$ARTIFACT" -d "$OUT" \
  "$ROOT/tools/consumer-fixture/src/ConsumerFixture.java"

test -f "$OUT/ConsumerFixture.class"
rm -rf "$OUT"
echo "Consumer fixture compiled against: $ARTIFACT"
