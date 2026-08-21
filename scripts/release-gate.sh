#!/usr/bin/env bash
# Full ArtFramework release gate (JUnit + art-verify + release docs + version assert + consumer fixture).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "== release-gate: JUnit =="
"$ROOT/scripts/with-art-env.sh" test

echo "== release-gate: art-verify offline =="
(cd "$ROOT/tools/art-verify" && python3 -m unittest discover -s tests -v)

echo "== release-gate: release documentation =="
"$ROOT/scripts/verify-release-docs.sh"

echo "== release-gate: jar + version assert =="
"$ROOT/scripts/with-art-env.sh" jar
PROP_VER="$(grep -E '^artframework\.version=' "$ROOT/gradle.properties" | cut -d= -f2- | tr -d '[:space:]')"
MANIFEST_VER="$(unzip -p "$ROOT/build/libs/ArtFramework.jar" META-INF/MANIFEST.MF \
  | tr -d '\r' | grep -i '^Implementation-Version:' | sed 's/^[^:]*:[[:space:]]*//' | tr -d '[:space:]')"
MTS_VER="$(unzip -p "$ROOT/build/libs/ArtFramework.jar" ModTheSpire.json \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('version',''))" 2>/dev/null \
  || unzip -p "$ROOT/build/libs/ArtFramework.jar" ModTheSpire.json | sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"
if [[ -z "$PROP_VER" || -z "$MANIFEST_VER" || -z "$MTS_VER" ]]; then
  echo "release-gate: missing version (prop=$PROP_VER manifest=$MANIFEST_VER mts=$MTS_VER)" >&2
  exit 1
fi
if [[ "$PROP_VER" != "$MANIFEST_VER" || "$PROP_VER" != "$MTS_VER" ]]; then
  echo "release-gate: version mismatch prop=$PROP_VER manifest=$MANIFEST_VER mts=$MTS_VER" >&2
  exit 1
fi
echo "version ok: $PROP_VER"

echo "== release-gate: Java 8 bytecode assert =="
python3 - "$ROOT/build/libs/ArtFramework.jar" <<'PY'
import struct
import sys
import zipfile

artifact = sys.argv[1]
maximum_major = 52
violations = []
with zipfile.ZipFile(artifact) as jar:
    for name in jar.namelist():
        if not name.endswith('.class'):
            continue
        data = jar.read(name)
        if len(data) < 8 or data[:4] != b'\xca\xfe\xba\xbe':
            violations.append((name, 'invalid class header'))
            continue
        major = struct.unpack('>H', data[6:8])[0]
        if major > maximum_major:
            violations.append((name, 'major {}'.format(major)))
if violations:
    for name, reason in violations:
        print('java8 bytecode violation: {} ({})'.format(name, reason), file=sys.stderr)
    sys.exit(1)
print('Java 8 bytecode ok (class major <= {})'.format(maximum_major))
PY

echo "== release-gate: consumer fixture =="
"$ROOT/scripts/verify-consumer-fixture.sh"

echo "release-gate PASS ($PROP_VER)"
