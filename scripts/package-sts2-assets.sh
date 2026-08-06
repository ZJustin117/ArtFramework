#!/usr/bin/env bash
# Package local STS2 animations for developer-only engine tests.
# The output is intentionally gitignored and is never consumed by release jar tasks.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f .env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
fi

: "${ART_STS2_ROOT:?ART_STS2_ROOT unset — set it to the local STS2 checkout}"
SOURCE="$ART_STS2_ROOT/animations"
OUTPUT="${ART_STS2_ASSET_JAR:-$ROOT/build/dev/Sts2Assets.jar}"

if [[ ! -d "$SOURCE" ]]; then
  echo "error: missing STS2 animations directory: $SOURCE" >&2
  exit 1
fi

WORK="$(mktemp -d "${TMPDIR:-/tmp}/art-sts2-assets.XXXXXX")"
cleanup() { rm -rf "$WORK"; }
trap cleanup EXIT

mkdir -p "$WORK/animations" "$(dirname "$OUTPUT")"

count=0
while IFS= read -r -d '' file; do
  relative="${file#"$SOURCE/"}"
  target="$WORK/animations/$relative"
  mkdir -p "$(dirname "$target")"
  cp -p -- "$file" "$target"
  count=$((count + 1))
done < <(
  find "$SOURCE" -type f \( \
    -name '*.skel' -o -name '*.atlas' -o -name '*.png' -o -name '*.tres' \
  \) -print0 | sort -z
)

if [[ "$count" -eq 0 ]]; then
  echo "error: no STS2 animation assets found under $SOURCE" >&2
  exit 1
fi

cat > "$WORK/META-INF-artframework-sts2-assets.properties" <<'EOF'
format=artframework-sts2-assets
formatVersion=1
sourceRoot=animations
spineData=4.2
createdBy=scripts/package-sts2-assets.sh
EOF

mkdir -p "$WORK/META-INF"
mv "$WORK/META-INF-artframework-sts2-assets.properties" \
  "$WORK/META-INF/artframework-sts2-assets.properties"

rm -f "$OUTPUT"
# Keep this compatible with Java 8's jar tool, which has no --create/--file flags.
(cd "$WORK" && jar cf "$OUTPUT" .)

echo "Packaged STS2 developer assets"
echo "Files: $count"
echo "Artifact: $OUTPUT"
sha256sum "$OUTPUT"
