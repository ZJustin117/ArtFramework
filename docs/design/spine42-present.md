# Spine 4.2 Present Architecture

ArtFramework can mirror the Slay the Spire 2 display architecture without distributing Slay
the Spire 2 assets. The public framework owns the presentation contracts, animation graph,
provider selection, and tests. Creature `.skel`, `.atlas`, and texture files remain user-provided
assets resolved through HostAssets or local packs.

## Boundary

- STS2 resources are not committed, copied into fixtures, or packaged in `ArtFramework.jar`.
- Developers may set `ART_STS2_ROOT` and run `scripts/package-sts2-assets.sh` to create a
  local `build/dev/Sts2Assets.jar`; this bundle is gitignored and is not a release input.
- The optional runtime is built separately with `scripts/build-spine42-runtime.sh`; its
  sub-build has no test source set and never consumes the developer asset bundle.
- Spine Runtime use is governed by the Spine Runtime license; distributions that include the
  runtime must include the license notice and users must satisfy the applicable Spine license.
- Core ArtFramework APIs remain Spine-free. Runtime-specific code lives behind
  `SkeletonProvider` implementations.
- STS1's built-in Spine 3.4 runtime and an optional shaded Spine 4.2 runtime coexist by provider id.

## Providers

| Provider | Runtime | Scope |
|---|---|---|
| `fake` | none | Pure JUnit and probe tests. |
| `spine34` | STS1-bundled `com.esotericsoftware.spine` 3.4 | Original STS1 JSON skeletons. |
| `spine42` | user-provided shaded runtime under `artframework.shaded.spine42.*` | User-owned Spine 4.2 binary skeletons. |

The 4.2 provider must not statically link the unshaded `com.esotericsoftware.spine` package,
because the game jar already contains Spine 3.4 under the same package name. It detects the
shaded package reflectively and reports `available=false` when the runtime is absent.

## STS2-style Runtime Layer

STS2 uses `SpineSprite` plus a C# reflection wrapper (`MegaSprite`) and a trigger-driven
`CreatureAnimator`. ArtFramework mirrors this with host-neutral types:

- `AnimState`: state id, loop flag, optional next state.
- `AnimGraph`: initial state plus trigger-to-state mapping.
- `SkeletonMixTable`: default mix plus explicit from/to mix durations.
- `SkeletonAnimator`: trigger queue, one-shot-to-idle chaining, random idle phase/speed.
- `SkeletonProvider`: command surface for `setAnimation`, `addAnimation`, `setMix`, time scale,
  track time, update/apply, and bone transform lookup.

## Atlas 4.x

STS2 atlases use the newer compact libGDX/Spine format (`bounds`, `offsets`, `rotate:90`,
`scale`) that STS1's libGDX 1.9.5 parser cannot read. `SpineAtlas4xParser` parses this format
into neutral region metadata. Provider implementations can materialize those regions into their
own renderer without requiring the old `TextureAtlasData` parser.

## Rendering Notes

Spine 4.2's stock libGDX runtime expects newer libGDX APIs such as `AtlasRegion.degrees` and
`Batch.setBlendFunctionSeparate`, which STS1's libGDX 1.9.5 lacks. The production 4.2 provider
therefore must either use a patched/shaded runtime adapter or a custom renderer that computes UVs
from parsed atlas metadata and applies blend state directly through GL. The initial provider shell
keeps this boundary explicit and safely degrades when the shaded runtime is not installed.

The current source-patched runtime validates binary skeleton data, animation state, bone
transforms, and reflective RegionAttachment and MeshAttachment batch paths. The provider draws
supported attachments into an already-active legacy libGDX batch without linking the incompatible
stock renderer. Because legacy `Batch.draw(Texture, float[], int, int)` has no indexed-mesh entry,
each mesh triangle is expanded to a four-vertex quad with a repeated final vertex. This preserves
triangle coverage through the quad shader but adds one draw call per triangle and does not provide
native indexed-mesh performance. Clipping is explicitly capability-gated: legacy STS1 Batch has no
polygon clip or stencil host SPI, so the provider pre-scans draw order and returns zero when a
`ClippingAttachment` is present. This prevents partially rendered, incorrectly unclipped output
and lets the native renderer recover; the batch is never begun, ended, or otherwise mutated by
this path. Unsupported attachment types and malformed attachment data are skipped fail-open and
never cause native suppression to claim pixels. Two-color attachments are also pre-scanned:
legacy Batch's fixed five-float vertex format has no dark-color attribute, so a non-null
`getDarkColor()` on a slot or attachment returns zero for the entire ART draw before any
submission. `supportsTwoColor(batch)` remains explicitly false until a host exposes a
compatible vertex contract; no partial two-color rendering or incorrect draw count is claimed.
The pure `Spine42Parity` contract validates the CPU side of this boundary: RegionAttachment and
MeshAttachment expanded positions, UVs, packed color propagation, triangle coverage,
counter-clockwise winding, and explicit invalid/degenerate results. Its `ParityResult` is
diagnostic and reusable in tests; `compareCoverage` also compares expected five-float vectors with
an explicit tolerance. This is CPU vertex/coverage parity only. It does not execute
GL, rasterization, blending, clipping, texture sampling, or image comparison. Real screenshot
pixel parity therefore remains an explicit open gap.

## Testing

Pure JUnit covers atlas parsing, animation graphs, mix tables, fake-provider commands, provider
availability probes, the RegionAttachment/MeshAttachment vertex contracts, the reusable CPU
parity report (including UV/coverage, winding, degenerate, and malformed-data cases), and the
explicit clipping capability/fail-open contract. Device verification is optional and
must use user-owned local assets.

`tests/spine42-assets` is a separate developer-only layer. It accepts `ART_STS2_ASSET_JAR`, or
uses `ART_STS2_ROOT` to invoke the local bundle script. It is not a Gradle source set, is not
included in the optional runtime artifact, and is not invoked by the default JUnit gate.
