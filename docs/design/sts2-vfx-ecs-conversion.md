# STS2 Godot VFX → ART ECS conversion

Status: **design fixed for implementation planning**

This document fixes the first implementation route for importing a restricted subset of
Slay the Spire 2 Godot VFX into ArtFramework and rendering it in the STS1/libGDX host.
The route is deliberately incremental: the converter emits a stable ART-owned data model,
and later work adds parsers and ECS systems without replacing that model.

## Decision

Use an offline converter plus an ART ECS runtime:

```text
STS2 .tscn/.tres/.png/.atlas
        → tools/sts2-converter
        → ART VFX bundle (manifest + scene data + resources + diagnostics)
        → Java loader
        → PresentationWorld entities/components
        → stateless ECS systems
        → libGDX render projection
```

The converter does not create ECS entity IDs and does not execute Godot scripts. The Java
runtime does not load `.tscn`, `.tres`, `.gdshader`, Godot UID files, or Godot runtime objects.
The converted bundle is the boundary between the Godot source representation and ART's
presentation data model.

This is a B-route implementation with a CPU particle baseline. It is not a Godot runtime
embedding project and it is not a promise of pixel parity with every STS2 effect.

## Boundaries

### In scope

- Developer-local STS2 assets selected through `ART_STS2_ROOT` or a generated local bundle.
- A restricted, explicit `.tscn` subset.
- A versioned, serializable ART VFX schema.
- Runtime projection into `PresentationWorld`.
- Stateless particle, lifecycle, curve, and render-projection systems.
- STS1/libGDX-compatible texture and atlas output.
- Capability diagnostics and fail-open behavior.

### Out of scope

- Godot or .NET runtime embedding in STS1.
- Executing STS2 C# scripts, Tweens, SceneTree callbacks, or game logic.
- Importing STS2 combat, party, protocol, or authority objects.
- Direct consumption of `.tscn`, `.tres`, `.gdshader`, `.spatlas`, or `.spskel` by ART runtime.
- Reproducing arbitrary Godot GPU particle or shader behavior in the first version.
- Putting VFX state into `EntityPresent`; that contract remains effect-free.
- Suppressing native STS1 pixels before complete ART output and evidence exist.

## Source facts and translation rule

STS2's VFX are Godot scene graphs. A typical scene combines `Node2D`, `Sprite2D`,
`GPUParticles2D`, `ParticleProcessMaterial`, curves, gradients, `ShaderMaterial`, z-order,
and script-driven lifecycle. `NVfxParticleSystem` starts descendant CPU/GPU particles and
removes the scene after a lifetime. Gameplay VFX such as `NSovereignBladeVfx` additionally
change particle emission, transforms, ordering, and animation through `_Process` and Tween.

The converter therefore translates **data**, not behavior. Its output has two deliberately
different layers: a lossless/source-preserving parsed IR and a selective typed runtime
projection. Every reliably recognizable section, attribute, assignment, constructor, and raw
value is retained in the parsed IR with logical source provenance. Only values whose semantics
are supported and valid are instantiated as typed definitions/components. Opaque values are
never interpreted by runtime systems:

| Godot source | ART representation |
|---|---|
| SceneTree node | Definition node plus ECS entity hierarchy |
| `ParticleProcessMaterial` | Immutable emitter definition |
| `Curve` / `Gradient` | Explicit control-point/source data and gradient stops; never silent sampling |
| `SceneTreeTimer` / lifetime | `VfxLifecycleComponent` |
| `_Process(delta)` motion | Stateless integration systems |
| `z_index` / node order | Render sort fields |
| `ShaderMaterial` | Capability diagnostic, later adapter or baked fallback |
| `GPUParticles2D` | ART particle entity with CPU baseline |

## ART VFX data contract

The first schema is ART-owned and versioned. It must remain usable when later systems add
features. New optional fields may be added; existing field meanings and units must not change.

### Bundle

```text
build/dev/sts2-vfx/<bundle-id>/
  manifest.json
  scenes/<scene-id>.json
  textures/...
  atlas/...
  diagnostics.json
```

`manifest.json` contains the format name, schema version, source identity, scene entries,
resource entries, and aggregate capability. `diagnostics.json` is part of the conversion
result, not an optional log. A scene file contains the exact source text, parsed section IR,
per-property conversion results, and typed nodes. Source paths are normalized logical paths;
absolute filesystem paths are forbidden throughout the bundle.

The bundle is source-preserving and suitable for later reconversion. Runtime ECS loading is
selective: it reads only typed definitions and resource entries. Parsed IR and opaque properties
are inspection/conversion inputs, not executable component data.

### Scene definition

```java
VfxSceneDefinition {
    id: String
    schemaVersion: int
    duration: float
    nodes: List<VfxNodeDefinition>
    resources: List<VfxResourceRef>
    capability: VfxCapability
}
```

`VfxNodeDefinition` contains a stable node ID, parent ID, node type, local transform, z-index,
visibility, modulate, source path, and an optional particle or sprite definition. Parent-child
relationships are data; the runtime may materialize them as ECS entities without recreating
Godot's SceneTree API.

### Particle emitter definition

The first complete baseline is:

```java
ParticleEmitterDefinition {
    amount: int
    lifetime: float
    lifetimeRandomness: float
    direction: Vec2
    spreadDegrees: float
    initialVelocity: Range
    gravity: Vec2
    scale: Range
    scaleCurve: CurveDefinition?
    alphaCurve: CurveDefinition?
    colorRamp: GradientDefinition?
    angularVelocity: Range
    flipbook: FlipbookDefinition?
    blendMode: BlendMode
    randomSeed: long
}
```

The baseline has complete semantics for every emitted typed field. The converter emits a field
only when its source value is semantically valid; it does not invent defaults for malformed or
missing values. Every non-emitted property remains in parsed IR and receives an explicit
conversion result. Runtime systems must never infer executable semantics from opaque data.

### Runtime state components

Definition data and mutable playback state are separate:

```text
VfxSceneComponent
VfxNodeComponent
ParticleEmitterDefinitionComponent
ParticleRenderComponent
CurveComponent / GradientComponent

VfxLifecycleComponent
ParticleEmitterStateComponent
ParticleBufferComponent
VfxTransformStateComponent
```

Definitions are immutable after import. Runtime state belongs in components. Systems do not
retain cross-tick mutable state, following `EcsSystem` and `PresentationWorld` rules.

### Resources

Resources use stable bundle-relative keys, never absolute paths or Godot UIDs:

```java
VfxResourceRef {
    resourceId: String
    kind: TEXTURE | ATLAS | SHADER_SOURCE | CURVE_TEXTURE | OTHER
    sourcePath: String
    outputPath: String
}
```

Runtime resolution remains host-managed through `HostAssets`/STS1 asset adapters. Raw texture
handles do not enter the framework-neutral public contract. Spine `.atlas` conversion continues
to use the existing `SpineAtlas4xParser` / `Spine42AtlasMaterializer`; `.tpsheet` conversion is
a separate resource materialization step.

## First `.tscn` support matrix

### Supported in schema version 1

- `[gd_scene]`, `[ext_resource]`, `[sub_resource]`, and `[node]` sections.
- `ExtResource(...)` and `SubResource(...)` references.
- `Vector2`, `Vector3`, `Color`, packed float/color arrays.
- `Node2D`, `Sprite2D`, `GPUParticles2D`, and `CPUParticles2D` nodes.
- `ParticleProcessMaterial` amount, lifetime, lifetime randomness, direction, spread,
  velocity, gravity, scale, angular velocity, scale/alpha curves, color ramp, and flipbook.
- Basic `CanvasItemMaterial` blend mode.
- Node transform, visibility, modulate, z-index, and hierarchy.
- PNG resources and developer-local bundle-relative resource copying.

### Parsed but reported as degraded or unsupported

- `ShaderMaterial` and `.gdshader`.
- `SCREEN_TEXTURE`, `SCREEN_UV`, and post-processing.
- turbulence and noise-driven particle motion.
- particle collision.
- sub-emitters.
- `CurveXYZTexture` and other multi-axis resources.
- Tween and script-authored runtime changes.
- `BackBufferCopy`, 3D nodes, Godot imported resources, UID/import cache products.

Unsupported input is retained in parsed IR and diagnostics with source path, node path where
available, section, line, property, reason, and capability. It is not silently discarded.

### Conversion result cases

- `supported`: recognized, valid, and emitted into the typed projection.
- `degraded`: recognized and partially representable with an explicit approximation; original
  source data remains in IR and the approximation is diagnosed.
- `missing-resource`: a valid allowed resource reference did not resolve; conversion continues
  without fabricating a handle or path.
- `unsupported-known`: the converter recognizes the feature but version 1 has no executable
  semantics for it (for example scripts, shaders, turbulence, collision, or sub-emitters).
- `unknown`: an otherwise parseable constructor, property, or node has no registered meaning.
- `malformed`: syntax is unparseable or a known semantic field has an invalid value/reference.

Unknown and unsupported-known values are opaque structured properties. Malformed values retain
their raw source form. All three remain non-executable. A supported sibling node continues to
convert when another node/property is degraded, missing, unsupported, unknown, or malformed.

## Capability and fail-open policy

```java
SUPPORTED | DEGRADED | BAKED_ONLY | UNSUPPORTED
```

- `SUPPORTED`: ART may render the converted result.
- `DEGRADED`: ART may render as an overlay; native STS1 output continues.
- `BAKED_ONLY`: the scene requires a separately generated flipbook/baked asset.
- `UNSUPPORTED`: no ART renderer is created for the unsupported portion.

These aggregate capabilities do not replace the per-property cases above. A bundle can be
`DEGRADED` while containing supported typed nodes and unsupported opaque siblings. `UNSUPPORTED`
applies to a portion that is not instantiated; it does not erase its source IR.

Version 1 always uses `NATIVE_WITH_ART_OVERLAY` for host integration. No native STS1 effect is
suppressed by the converter or the initial VFX renderer. FULL delegation requires a later
capability proof, complete draw evidence, and an explicit render-family policy update.

## ECS systems

Systems are stateless processors. Persistent facts are components; host caches remain
non-authoritative and are used only for resource/materialization or evidence.

### `VfxInstantiateSystem`

Creates a root entity and child entities from a `VfxSceneDefinition`, attaches definition and
initial runtime components, and establishes parent-child presentation relationships.

### `ParticleSpawnSystem`

Uses emitter definition and deterministic seed to create particles with age, position, velocity,
rotation, scale, and color. The first implementation is CPU-based and bounded.

### `ParticleIntegrateSystem`

Advances position, velocity, rotation, and age using the frame delta. The baseline supports
gravity and linear motion; turbulence and collision remain explicit future systems.

### `ParticleCurveSystem`

Samples scale curves, alpha curves, and color ramps using normalized particle age and writes
the current render state to `ParticleBufferComponent`.

### `VfxLifecycleSystem`

Translates Godot one-shot/lifetime behavior into component state. It marks completed emitters,
removes expired particles, and destroys the VFX entity graph on completion, scene epoch change,
or host recreation.

### `ParticleRenderProjectionSystem`

Reads ECS state, sorts by node order/z-index, resolves texture regions, computes flipbook frame,
and emits libGDX-compatible draw data. It does not own particle simulation state and does not
write directly into `EntityPresent`.

This follows the existing collection → projection → system family pattern. A future host-side
ledger/registry may be added for native VFX observation and lifecycle evidence, analogous to the
transient-effect template, but it must not become a second presentation authority.

## Converter and runtime placement

The converter belongs under `tools/sts2-converter/` and may use Python 3. It may inspect
`ART_STS2_ROOT` and developer-local assets but must not add STS2 packages to the Java build.

The Java side should be split into:

```text
VfxManifestLoader       JSON/schema → immutable definitions
VfxResourceResolver     HostAssets/bundle-relative resources
VfxInstantiateSystem    definitions → PresentationWorld
Particle*System         ECS simulation
ParticleRenderProjectionSystem  ECS → host draw data
VfxLifecycleAdapter     cleanup/recovery boundary
```

`Sts2AssetBundle` remains an asset source/materialization helper. `.tscn` parsing does not belong
inside it. `HostAssets` remains the logical resource catalog and does not become a filesystem
authority.

## Extension order

Add capabilities in this order:

1. `.tpsheet` to legacy libGDX atlas materialization.
2. Angular velocity and flipbook parity improvements.
3. `CurveXYZTexture`.
4. Sub-emitter definitions and a `ParticleSubEmitterSystem`.
5. CPU approximation of turbulence.
6. A restricted `ShaderMaterial` schema and explicit GLSL adapter.
7. Baked flipbook fallback for unsupported shaders and screen effects.
8. Only after evidence, any native-pixel delegation policy.

Each extension adds a parser rule, schema field, diagnostics/test coverage, and one or more
stateless systems. It must not replace the version 1 data contract or add Godot runtime types
to ART core.

## Verification contract

Pure tests cover:

- section and Godot value parsing;
- resource and subresource resolution;
- curve and gradient decoding;
- scene hierarchy and transform conversion;
- manifest/schema validation and deterministic output;
- capability diagnostics and path traversal rejection;
- deterministic particle spawning and curve sampling;
- lifecycle cleanup and epoch/recreation recovery;
- missing-resource fail-open behavior.

Real STS2 samples are developer-local and are never committed or packaged in `ArtFramework.jar`.
The first device/UI acceptance uses one simple particle scene in overlay mode and records the
converted manifest, source identity, runtime identity, viewport, and capture evidence under the
developer-only debug artifacts area.

## Fixed first milestone

The first implementation milestone is complete when one real STS2 `.tscn` containing basic
2D particles can be converted into an ART VFX bundle, loaded by Java, instantiated into
`PresentationWorld`, advanced by the CPU ECS systems, projected to STS1/libGDX draw data, and
cleaned up without native-pixel suppression. Complex properties must appear in diagnostics and
must not prevent supported sibling nodes from converting.
