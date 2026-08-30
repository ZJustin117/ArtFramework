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
art skeleton dev bone d1_ironclad root
art skeleton dev stop d1_ironclad
```

Inspect first load, idle loop, attack, rotated atlas parts, PMA edges, and teardown. Repeat
load/play/stop at least ten times and save screenshots plus `art probe` output for each failure.

The standard JUnit gate remains independent of these resources. The pure `Spine42Parity` JUnit
contract can validate expanded CPU vertices, UVs, packed color, triangle coverage, winding,
expected-vector mismatches, and invalid/degenerate data without a device, STS2 resource, or
downstream protocol. It is not a
GL/rasterization or screenshot pixel-parity test. Human testing must inspect
first load, idle, one-shot animation, atlas rotation, PMA edges, bone anchors, surface teardown,
and repeated load/unload behavior.

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
