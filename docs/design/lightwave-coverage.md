# Lightwave coverage matrix

Lightwave is the presentation verification profile for ArtFramework. It validates that the
framework can resolve a profile, inflate C1 components, attach effects to stable bounds, draw C2
surface chrome, expose probe state, and clean up managed targets when the profile changes.

Spine and skeleton rendering are intentionally outside this matrix.

## C1 component coverage

| Category | Types | Verification role |
|---|---|---|
| Containers | `window`, `panel`, `glass`, `row`, `col`, `stack`, `fragment`, `scroll`, `center`, `margin`, `grid`, `tabs` | Bounds, nesting, clipping, stacking, and layout effect attachment |
| Controls | `label`, `button`, `slider`, `textfield`, `checkbox`, `progress`, `hitarea` | Visual bounds, readable chrome, and control state surfaces |
| Composition | `ref`, `slot` | Pack template expansion and subtree composition |
| Behavior | `animation_player` | Property animation and signal-driven FX |
| Visual behavior | `art.shader_effect` | Explicit shader/effect attachment |
| Profile behavior | `art.present_profile` | Node-scoped profile cascade |

The first two rows are visual component coverage. The last three rows validate composition and
runtime behavior and are not assigned a separate default effect target.

## C2 surface coverage

The pack binds Lightwave chrome and surface effects for every implemented non-Spine surface:
combat hand, controls, card slots, energy, intents, proceed, map, event, grid select, hand
select, reward variants, rest, treasure, shop, and top panel.

Profile selection never enables FULL present. Native suppression remains controlled by the
per-surface full-present capability state.

## Acceptance rules

1. Every visual C1 type has a Lightwave `effectDefaults` entry.
2. The gallery contains at least one instance of every visual C1 type.
3. Every non-Spine C2 surface has a Lightwave surface effect entry.
4. Switching away from Lightwave removes managed full-frame, surface, and C2 effect bindings.
5. Probe and offline fixtures expose profile, pack, component, target, and cleanup state.
