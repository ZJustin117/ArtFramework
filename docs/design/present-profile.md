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

Effects are separate nodes (art.shader_effect / effects[]), never implied by profile.
```

**FullPresentMode** still decides suppress / FULL vs OBSERVE. A profile never
auto-suppresses native surfaces.

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

- `ArtFramework.setProjectPresent("lightwave")` / `projectPresent()` / `presentChrome()` (project fallback)
- `ArtFramework.resolvePresent(windowId)` — tree root resolve
- `UiTree.resolvePresent()` / `UiInstance.resolvePresent()`
- Console: `art profile\|theme list\|get\|set\|project\|resolve <windowId>`
- Probe: `projectPresent` / `presentProfile` (alias with `project` + `active`); `windows.byId.*.present`

## Lightwave visuals

| Concern | Mechanism |
|---------|-----------|
| Tokens | `LightwaveTheme` colors/constants |
| C1 skin | `StsSkin.create(resolved Theme)` |
| Diagonal band | `LightwaveEffect` via layout `effects` / shader node (explicit) |
| Bounds | border tokens + effect border; C2 hand white frame from chrome |
| Event anim | `art.animation_player` (independent node) |

## Lookup (Theme colors)

1. Instance color/constant override  
2. Parent chain node themes (variation `themeType` → base type)  
3. `UiTree.theme()` from `PresentResolve` at root / project fallback

## Known limits

- Project switch does not rebuild already-open windows (close/reopen to restyle)
- C2 without a surface present node uses **project** chrome only
- Lightwave does not auto-enable FULL present or native chrome
- Scenarios assert probe contracts, not screenshot SSIM

## Related

- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — Theme cascade  
- [`host-assets.md`](./host-assets.md) — packs / resolve  
- [`c2-full-present.md`](./c2-full-present.md) — FULL vs overlay  
