#!/usr/bin/env bash
# Build the optional Spine 4.2 runtime artifact. It has no tests or STS2 assets.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/spine42-runtime"

"$ROOT/gradlew" --no-daemon shadowJar verifyRuntimeArtifact

echo "Runtime artifact: $ROOT/spine42-runtime/build/libs/ArtFramework-Spine42Runtime.jar"
