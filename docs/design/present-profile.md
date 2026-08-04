# PresentProfile (node-scoped, Lightwave and friends)

Named **presentation resource** for ArtFramework: Theme + PresentChromeStyle + optional
packId. Applied through **node cascade** (Godot-like), not a process “active profile”.

## Role

```text
PresentProfiles registry          (resources only — no active id)
ProjectPresent                    (process fallback when no node binding)
        │
art.present_profile | present_profile prop
  mode: override | attach
        │
PresentResolve.forNode / forTree
        ├─► Theme     → C1 StsSkin / UiTree.theme
        ├─► Chrome    → C2 hand / controls (project when no surface node)
        └─► packId    → HostAssets (optional)

Effects live in layout / LML (or pack templates), never implied by Java profile id.
PresentPack registers classpath LML/JSON; select profile → activate pack by packId.
```

**FullPresentMode** still decides suppress / FULL vs OBSERVE. A profile never
auto-suppresses native surfaces.

## PresentPack (UI module)

```text
PresentPacks.register(manifest | PresentPack)
  templates: name → classpath (.json|.lml)  → ComponentRegistry on activate
  windows:   id   → classpath              → WindowDef on activate
  autoOpen:  optional mounts after activate

ProjectPresent.set(profileId)
  → theme/chrome restyle
  → PresentPacks.activateForProfile(profile)   // packId, else pack.profileId match
```

Builtin `lightwave` profile has `packId=lightwave` and manifest
`present-packs/lightwave/pack.json` (templates, **effectDefaults**, **fullFrame**,
**surfaceEffects**, **bindSurfaces**). Activate applies ambient tables via `PresentPackApply` (not Java
`if (lightwave)`). Layout `effects[]` still wins when non-empty.

**Why “set lightwave” alone looked like no global FX:** skin/chrome + pack templates
register without open windows / full-frame / surface binds until pack ambient tables
exist. After pack.json ambient fields, `art profile set lightwave` enables full-frame
band + C2 chrome binds + empty-node effectDefaults.

**Coverage (honest):**

| Surface | Lightwave band | Notes |
|---------|----------------|-------|
| C1 synthetic (Art open windows) | Per-widget via effectDefaults + layout effects | StageHost maps targets to actor stage bounds |
| Full screen | `fullFrame` on active pack | Always-on overlay while pack active |
| C2 / native STS UI | Surface chrome + per-surface LightwaveEffect when FULL is drawn | Native UI remains unchanged unless FULL present owns the surface |
| Native dialogs (not Art windows) | Only full-frame wash | Not individual hitboxes |

Native STS is not a scene2d tree ART owns — component frames apply to **ArtFramework C1** only.

## Built-in resources

| id | Theme | Notes |
|----|--------|--------|
| `sts` | `StsTheme` | Cream/gold default; opaque cards |
| `lightwave` | `LightwaveTheme` | Cool tint, semi-transparent panel, white border tokens, card alpha ~0.88 |

## Node model

| Mechanism | Scope |
|-----------|--------|
| `art.present_profile` child | props `profile` + `mode` (`override` \| `attach`); may wrap a subtree |
| Root/layout sugar `present_profile` | override binding on that node |
| `theme` prop name only | theme tokens without full profile id |
| `ProjectPresent` | fallback when cascade empty |

**override** — truncate parent present chain; this layer is the base for the subtree.  
**attach** — stack; nearest leaf-ward layer wins the whole Theme+Chrome package.

## API

### Global catalog (register — does not apply)

| API | Role |
|-----|------|
| `ArtFramework.registerPresentProfile(PresentProfile)` | Install skin resource |
| `ArtFramework.registerPresentProfile(id, Theme [, packId])` | Sugar |
| `ArtFramework.getPresentProfile(id)` / `presentProfileIds()` | Lookup |
| `ArtFramework.presentProfiles()` | `PresentProfileCatalog` view (register/get/ids/probe) |
| `PresentProfiles.register` | Same store (core; facade preferred for consumers) |

Register syncs `Themes` under theme name (and profile id when needed). **Does not** change
`ProjectPresent` or open windows.

| Pack / enable API | Role |
|-------------------|------|
| `registerPresentPack` / `activatePresentPack` | UI module catalog |
| `enabledPresentIds` / `setEnabledPresentProfiles` | Panel enable set (empty = all) |
| `presentIdsMatching(regex)` / `selectPresentMatching(regex)` | Regex select (first enabled) |
| `modifyPresentsMatching(regex, enable)` | Regex enable/disable |
| `modifyPresentPackIdMatching(regex, packId, selectFirst)` | Regex patch packId |

### Apply

- `ArtFramework.setProjectPresent("lightwave")` — skin restyle + **activate linked pack**
- `ArtFramework.bindSurfacePresent(surfaceId, profileId)` — C2 surface chrome
- Console: `art profile list|set|select <regex>|enable|disable|pack|modify|…`
- Probe: `presentPacks`, `enabledPresents`, `presentProfiles`, `projectPresent`
## Lightwave visuals

| Concern | Mechanism |
|---------|-----------|
| Tokens | `LightwaveTheme` colors/constants |
| C1 skin | `StsSkin.create(resolved Theme)` |
| Diagonal band | `LightwaveEffect` via layout `effects` / shader node (explicit) |
| Bounds | border tokens + effect border; C2 surface regions share draw-plan bounds |
| Event anim | `art.animation_player` (independent node) |

## Lookup (Theme colors)

1. Instance color/constant override  
2. Parent chain node themes (variation `themeType` → base type)  
3. `UiTree.theme()` from `PresentResolve` at root / project fallback

## Surface present (C2)

```text
SurfacePresent.bind(surfaceId, profileId)
  → PresentResolve.chromeForSurface / forSurface
  → Sts1SurfaceRenderer / C2 chrome painter / surface FX targets
```

Unbound surfaces use **ProjectPresent**. Binding does not enable FULL present.

## Pack preference

`PresentProfile.packId` non-empty → `HostAssets.preferPresentPack` (enable + last in
packOrder). Applied on project present change and surface bind when pack set.

## Known limits

- Node-override C1 windows keep their resolve on project switch (by design); only
  **fromProject** trees re-attach Stage skin
- Lightwave does not auto-enable FULL present or native chrome
- Scenarios assert probe contracts, not screenshot SSIM
- C2 chrome is label/panel/border/alpha tokens — not full STS atlas fidelity
- Surface FX only draws for active FULL draw-plan entries; profile selection alone does not suppress native UI

## Related

- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — Theme cascade  
- [`host-assets.md`](./host-assets.md) — packs / resolve  
- [`c2-full-present.md`](./c2-full-present.md) — FULL vs overlay  
