# Spine 4.2 D1 Resource Test

This is a developer-only D1 workflow. It does not add `Sts2Assets.jar` or
`ArtFramework-Spine42Runtime.jar` to `mods_library`; both are pushed to the app-private
`art-assets` directory and are loaded explicitly by the development asset provider.

## Build and Push

```bash
./scripts/package-sts2-assets.sh
./scripts/build-spine42-runtime.sh
./scripts/deploy-spine42-d1.sh
```

Required environment keys:

- `ART_D1_SERIAL`
- `ART_STS2_ASSET_JAR`
- `ART_SPINE42_RUNTIME_JAR`

Optional `ART_D1_ASSET_DIR` overrides the default device directory. Values live only in
`.env.local`; shared documentation uses key names only.

## Execution Order

1. Build and deploy `ArtFramework.jar` through the normal Android deploy workflow.
2. Push the two developer jars with `scripts/deploy-spine42-d1.sh`.
3. Confirm the connector is already online.
4. Run `scripts/art-lab ready` and wait for `READY`.
5. Run the asset status scenario, then load/animation/lifecycle scenarios.
6. Capture `result.json`, `latest.log`, ART probe output, and screenshots.

For human testing, use the following console sequence after `READY`:

```text
art present skeleton on
art skeleton dev load d1_ironclad animations/characters/ironclad/ironclad.atlas animations/characters/ironclad/ironclad.skel
art skeleton dev play d1_ironclad idle_loop
art skeleton dev play d1_ironclad attack
art skeleton dev seek d1_ironclad 0.0
art skeleton dev freeze d1_ironclad
art skeleton dev bone d1_ironclad root
art skeleton dev stop d1_ironclad
```

Inspect first load, idle loop, attack, rotated atlas parts, PMA edges, and teardown. Repeat
load/play/stop at least ten times and save screenshots plus `art probe` output for each failure.
`seek <id> 0.0` resets the primary animation track to a fixed pose time; `freeze <id>` sets its
time scale to zero so the pose remains stable while taking screenshots.

The standard JUnit gate remains independent of these resources. The pure `Spine42Parity` JUnit
contract can validate expanded CPU vertices, UVs, packed color, triangle coverage, winding,
expected-vector mismatches, and invalid/degenerate data without a device, STS2 resource, or
downstream protocol. It is not a
GL/rasterization or screenshot pixel-parity test. Human testing must inspect
first load, idle, one-shot animation, atlas rotation, PMA edges, bone anchors, surface teardown,
and repeated load/unload behavior.

## 47.35 GL/screenshot acceptance

The real parity check uses one canonical, approved developer-local reference fixture only. Record
the exact skeleton plus atlas/texture identity, the named animation state, and a fixed primary-track
time (or a documented setup pose); both native and ART must use that same record. The reference
files are not committed assets and must not be copied into repository tests or release artifacts.
Render the native and ART paths on the same D1 device
with the same viewport, camera/scale, origin, clear color, filtering, blend mode, and orientation.

Capture one native screenshot and one ART screenshot for that fixed state. Crop both to the same
documented pixel rectangle, with identical crop dimensions, before comparison. Record full image
dimensions, crop coordinates, image format, alpha handling, and color-space/format conversion.
Choose and record the per-channel absolute-difference threshold T and maximum differing-pixel
ratio R before comparison. After conversion, a pixel differs when any compared channel is greater
than T; pass requires maximum channel difference <= T and differing-pixel ratio <= R. If alpha is
ignored, exclude it from both calculations. The comparison output must include T, R, measured maximum
difference, differing-pixel count/ratio, and pass/fail. Do not use an undocumented visual
inspection threshold or infer parity from draw counts, probe state, lifecycle success, or the
CPU `Spine42Parity` result.

The acceptance bundle must contain the paired native/ART screenshots, comparison output, fixture
state/pose and asset provenance identifiers, device/build/runtime identifiers, viewport and crop
metadata, threshold values, measured metrics, and ART/native draw plus fail-open evidence. Keep
these outputs under the developer-only debug-artifacts area or equivalent local output; no asset
image becomes a committed fixture. 47.35 remains open until this complete bundle passes.

The check is fail-open and excludes any fixture with `ClippingAttachment`, two-color data,
unsupported attachment types, malformed attachment data, or another capability not supported by
the legacy batch contract. For an excluded fixture, ART must draw zero without claiming native
suppression, native rendering must recover, and the result must be marked excluded rather than
passed as pixel parity. Partial rendering is never acceptance evidence.

## Current Capability

The current optional runtime is source-patched for the STS1 libGDX 1.9.5 data/animation path.
The D1 scenarios verify a real binary `.skel` load, animation state changes, bone transforms, and
lifecycle cleanup. MeshAttachment rendering now uses the legacy Batch quad entry by expanding each
indexed triangle; this is a compatibility path with one draw call per triangle. Clipping is
capability-gated: because the legacy Batch host exposes no polygon clip/stencil capability, a
skeleton containing `ClippingAttachment` is left to the native renderer and contributes zero ART
draws. Two-color is explicitly capability-gated as well: the legacy five-float Batch vertex
contract has no dark-color attribute, so slots or attachments with a non-null dark color cause
the complete ART draw to return zero before submission and native rendering recovers. These
scenarios still do not constitute pixel-parity proof. Real GL/screenshot pixel parity remains
open and requires controlled device captures plus image comparison evidence; it must not be inferred
from the CPU contract or from successful D1 lifecycle results.

Device scenarios:

```text
tests/ui-scenarios/device/d1_spine42_assets_status.yaml
tests/ui-scenarios/device/d1_spine42_load.yaml
tests/ui-scenarios/device/d1_spine42_animation.yaml
tests/ui-scenarios/device/d1_spine42_lifecycle.yaml
```
