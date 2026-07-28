# HostAssets — unified asset library

Design for a **host-managed asset library**: vanilla catalog keys, replaceable **packs/skins**,
unified config, and **ART-only resolve-and-render** consumption. Complements
[`backend-context.md`](./backend-context.md), [`c2-full-present.md`](./c2-full-present.md),
[`art-framework.md`](./art-framework.md) (Host SPI `assets()`).
Roadmap: [`docs/task.md`](../task.md) milestones **15** (contract) and **16.2** (STS1 catalog).

## STS1 vanilla catalog (16.2)

`artframework.sts1.assets.Sts1VanillaCatalog` maps ResourceIds to logical `sts1:` sources
(UI / card frames / map nodes / common card art / audio). `Sts1HostAssets.install()` replaces
the minimal vanilla catalog at mod init. Texture handle materialization remains host-side for
16.3+ draw; resolve stays pure (no GL in JUnit).

## Purpose

1. Parse and host **vanilla interface asset keys** in one store.
2. Register **replaceable packs** (map, character, card art, UI skins, SFX/BGM, FX).
3. **Unified configuration**: enable order, domain switches, profiles — observable via probe.
4. ART (C1/C2 full present, Theme, RenderHost) **only resolves ResourceIds and draws**.
5. Close BaseMod multi-pack gaps **on the ART render path**: priority, key space, probeability,
   multi-domain config — without claiming ownership of every non-ART native draw path in v1.

## Non-goals (phase 1)

- Rewriting all of BaseMod’s asset APIs.
- Making ART the on-disk owner of the entire STS art tree.
- Exposing raw GL types on the public consumer contract.
- Putting combat/protocol authority in the asset layer.
- Mandatory reverse-patch of every non-ART native draw (optional later **global bridge**).

## Placement

```text
Vanilla / pack files / config
        → HostAssets (register, merge, resolve, probe)
        → ART Theme / C1 / C2 / RenderHost
        → hard-sync present (+ optional audio cues)

Backend snapshot carries ResourceId only
Return channel does not ship textures; may carry skin-profile intents
```

| Role | Relation to HostAssets |
|------|------------------------|
| **Host adapter (e.g. STS1)** | **Manages** implementation, lifecycle, loading |
| **Primary Backend** | Binds which assets scope is active; frames use keys |
| **ART core** | Defines contract; calls `resolve`; short-lived handle cache only |
| **Pack / beautify mods** | `registerPack` / enable / order at init |
| **UI runtime** | Consumes resolve results; no second filesystem authority |
| **Consumers** | Reference keys; do not own the global loader singleton |

## ResourceId

Stable string keys, versionable, pack-aligned. Domains (illustrative):

| Domain | Example keys |
|--------|----------------|
| Card | `card.art.Strike_R`, `card.frame.red` |
| Character | `char.ironclad.portrait`, `char.ironclad.shoulder` |
| Map | `map.node.monster`, `map.bg.act1` |
| UI | `ui.button.red`, `ui.panel.gray` |
| FX | `fx.glow`, `shader.glass` |
| Audio | `audio.sfx.card_select`, `audio.bgm.exordium` |

Rules:

- Packs **override by same key**, not by inventing unresolvable paths as public ids.
- **Vanilla catalog** maps original interface assets into this key space at host startup.
- Public API prefers keys + metadata; **handles stay inside host implementation**.

## AssetPack

```text
AssetPack
  id, version, provider (modId)
  domains: CARD | CHAR | MAP | UI | FX | AUDIO | …
  priority / explicit order participation
  enabled
  entries: ResourceId → AssetRef (path or host-local descriptor)
  replaces: optional declared vanilla keys
```

Covers: vanilla map skins, character retextures, card beautify, SFX/BGM replace, ART control
skins, registered FX/shaders by id.

## Resolve result

Host turns `AssetRef` into ART-consumable **descriptors**:

| Kind | Result shape (conceptual) |
|------|---------------------------|
| Image | size, nine-patch meta, host handle id |
| Font / color | Theme tokens or font keys |
| Audio | playable cue id / descriptor (playback via host audio backend) |
| Missing | `missing` + reason → ART fallback |

Capability-aware hosts: unsupported shader/capture → validated failure / fallback
([`art-framework.md`](./art-framework.md)).

## Registration API (conceptual)

| API family | Role |
|------------|------|
| `registerPack` / `unregisterPack` | Install pack |
| `enablePack` / `disablePack` | Runtime switch |
| `setPackOrder(ids…)` | Explicit global order |
| `registerAlias(from, to)` | Legacy key compatibility |
| `registerVanillaCatalog(…)` | Usually once at host boot |
| `resolve(resourceId)` | Current merge result |
| `probeAssets()` | Packs, conflicts, missing, domain stats |
| `config()` | Profiles, domain enable, pack enable, options |

### Merge order (fixed policy)

1. Domain disabled → pack entries in that domain ignored.
2. Among enabled packs: **explicit packOrder**, then priority, then registration order.
3. No pack hit → **vanilla catalog**.
4. Still none → **ART safe fallback** (placeholder / silence / default control chrome).
5. Conflicts are **observable** in probe (“key X won by pack Y over Z”), not silent forever.

## Unified config

First-class config (not scattered ad-hoc files only):

```text
assets.config
  activeProfile
  packOrder[]
  domainEnable { CARD, MAP, CHAR, UI, AUDIO, … }
  packEnable { packId → bool }
  options
    strictMissing
    allowAudioReplace
    allowMapReplace
```

- Driven by local config, console, and optional return intents
  (`select_skin_profile`, `enable_pack`) from [`backend-context.md`](./backend-context.md).
- Config change → **invalidate** related caches → subsequent frames resolve anew (hard-sync
  present picks up new skins without rewriting card authority).

## Lookup order while drawing

1. Instance / control local override  
2. Window / surface Theme  
3. Backend frame content ResourceIds  
4. Host default Theme (STS-flavored tokens)  
5. ART built-in fallback  

Missing **content** art → placeholder + probe missing.  
Missing **framework** chrome → ART default.  
Missing **mod overlay** icon → hide overlay only.

## BaseMod relationship

| Gap | HostAssets response |
|-----|---------------------|
| Multi beautify packs, unclear priority | Explicit `packOrder` + domains |
| Fragmented key spaces | Forced ResourceId + vanilla catalog |
| Invisible “who won” | `probeAssets` |
| Art / UI / audio silos | One register + config surface |
| Hard to unit-test | `FakeHostAssets` + in-memory packs |

**Honest boundary:** phase 1 is the **unified store for ART render**. STS1 may **bridge-read**
ImageMaster / existing loads to fill vanilla catalog. Optional later: broader native-path
bridge so non-ART draws share packs — not required to ship ART full present.

## Lifecycle

```text
Host start
  → install HostAssets
  → load vanilla catalog
  → load config profile
  → mods registerPack (dependency order)
  → apply enable / order
  → ART resolves for draw

Runtime
  → enable / reorder / profile switch
  → invalidate
  → next frames use new resolve

Backend switch / unload
  → tear down or re-scope packs/handles
  → ART drops stale handles
```

## Iron rules

1. One resolve → **one** winner per key (no half-A half-B).  
2. ART does **not** own file authority; register at host/mod init.  
3. **Stable keys** over file paths.  
4. Config changes **probeable**.  
5. Missing assets **degrade**, never crash combat UI.  
6. Retexture ≠ change card rules or playability.  
7. Switching Primary Backend re-scopes assets; no cross-backend handle reuse.

## Testing

| Layer | Focus |
|-------|--------|
| JUnit | Merge order, domain off, alias, conflict probe, FakeHostAssets |
| No GL required for merge logic |
| Optional D1 | Pack enable changes visible on full-present hand/map after deploy |

## Phasing

| Slice | Deliverable |
|-------|-------------|
| 15.0 | This design + links |
| 15.2 | Pure merge/config/FakeHostAssets + JUnit |
| 15.3 | ResourceId conventions + minimal vanilla catalog (card/map/UI) |
| 15.4 | ART draw paths go through resolve (Theme/C1 first; C2 with full present) |
| 15.8 | Pack register API for beautify mods + probe/console |
| Later | Optional global bridge beyond ART draw |

## Related

- [`backend-context.md`](./backend-context.md) — who binds assets; skin intents
- [`c2-full-present.md`](./c2-full-present.md) — frames carry resource ids
- [`art-framework.md`](./art-framework.md) — Host SPI `assets()`, effects/shaders
- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — Theme cascade
- [`docs/development/consumer.md`](../development/consumer.md) — consumer load order
