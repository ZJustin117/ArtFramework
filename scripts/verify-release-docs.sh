#!/usr/bin/env bash
# Verify release-facing version and milestone documentation without building the jar.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

PROP_VER="$(sed -n 's/^artframework\.version=//p' "$ROOT/gradle.properties" | tr -d '[:space:]')"
if [[ -z "$PROP_VER" ]]; then
  echo "release-docs: missing artframework.version" >&2
  exit 1
fi

grep -Fq "Implemented milestones **0–46**" "$ROOT/README.md"
grep -Fq 'later completed work remains under `Unreleased` until the next version' "$ROOT/README.md"
grep -Fq "## $PROP_VER" "$ROOT/CHANGELOG.md"
grep -Fq "## Unreleased" "$ROOT/CHANGELOG.md"

unreleased_line="$(grep -n -m1 '^## Unreleased$' "$ROOT/CHANGELOG.md" | cut -d: -f1)"
version_line="$(grep -n -m1 "^## $PROP_VER$" "$ROOT/CHANGELOG.md" | cut -d: -f1)"
if [[ -z "$unreleased_line" || -z "$version_line" || "$unreleased_line" -ge "$version_line" ]]; then
  echo "release-docs: Unreleased must precede $PROP_VER" >&2
  exit 1
fi
milestone_3943_line="$(grep -n -m1 'Milestones \*\*39–43\*\*' "$ROOT/CHANGELOG.md" | cut -d: -f1)"
if [[ -z "$milestone_3943_line" || "$milestone_3943_line" -le "$unreleased_line" || "$milestone_3943_line" -ge "$version_line" ]]; then
  echo "release-docs: milestones 39–43 are not documented under Unreleased" >&2
  exit 1
fi
for milestone in 44 45 46; do
  milestone_line="$(grep -n -m1 "Milestone \*\*$milestone\*\*" "$ROOT/CHANGELOG.md" | cut -d: -f1)"
  if [[ -z "$milestone_line" || "$milestone_line" -le "$unreleased_line" || "$milestone_line" -ge "$version_line" ]]; then
    echo "release-docs: milestone $milestone is not documented under Unreleased" >&2
    exit 1
  fi
done

section_44="$(grep -n -m1 '^### 44\. Spine 4\.2 present architecture$' "$ROOT/docs/task.md" | cut -d: -f1)"
section_45="$(grep -n -m1 '^### 45\. Unified Presentation Entity Runtime$' "$ROOT/docs/task.md" | cut -d: -f1)"
line_4413="$(grep -n -m1 '^[- ]*\[x\] 44\.13 ' "$ROOT/docs/task.md" | cut -d: -f1)"
line_451="$(grep -n -m1 '^[- ]*\[x\] 45\.1 ' "$ROOT/docs/task.md" | cut -d: -f1)"
if [[ -z "$section_44" || -z "$section_45" || -z "$line_4413" || -z "$line_451" \
      || "$section_44" -ge "$line_4413" || "$line_4413" -ge "$section_45" \
      || "$section_45" -ge "$line_451" ]]; then
  echo "release-docs: milestone 44.13 is not inside section 44 before section 45" >&2
  exit 1
fi

grep -Fq 'ArtFramework.dispatch(new UiSignal' "$ROOT/docs/development/api-overview.md"
grep -Fq '`SignalBackend`' "$ROOT/docs/development/api-stability.md"
grep -Fq '`PresentationWorld`' "$ROOT/docs/development/consumer.md"

echo "release-docs PASS ($PROP_VER; implemented milestones 0–46)"
