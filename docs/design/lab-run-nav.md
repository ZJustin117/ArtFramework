# Lab run navigation (`art lab`)

BaseMod console surface for **deterministic lab navigation**: return to main menu,
clear saves, open character select, embark, optional seed, native map-room entry,
native grid/hand selection, and smart recipes that compose those steps. Enables D1
full-present scenarios without manual menu clicking.

Complements [`dev-ui-console.md`](./dev-ui-console.md) (`art ui native` whitelist)
and [`android-device-lab.md`](../development/android-device-lab.md).

## Why

| Need | Gap before this slice |
|------|------------------------|
| D1 combat/map present | No automated path from title → run |
| Abandon mid-run | Only manual settings / autoplay in Amethyst |
| Fresh save policy | Resume buttons block new runs |
| Reusable YAML steps | Only ad-hoc `art ui native click` combat paths |

## Architecture

```text
art lab …
  ├─ L1 atomic     → StsLabNav (whitelist STS hitbox / open / delete save)
  ├─ L2 recipes    → StsLabRecipes (branch on LabStateSnapshot + limited ticks)
  └─ dump          → StsLabState (mode / menu / buttons / inGame / combat)
```

- **Lab only** — not consumer-stable API (`artframework.api`).
- Failures soft: `UiOpResult.unavailable`, log prefix **`ART_LAB`**.
- Main thread: schedule via `Gdx.app.postRunnable` when present.
- Inspired by Amethyst `AutoplayMainMenuActions` / `AutoplaySingleRoomRunner`,
  but **on-demand console**, not a permanent autoplay tick.

### Two layers

| Layer | Examples | Role |
|-------|----------|------|
| **L1 atomic** | `clear-saves`, `open-char-select`, `char`, `embark`, `abandon` | One schedule / one effect; YAML may `wait_ms` between steps |
| **L2 recipes** | `ensure-menu`, `ensure-fresh-menu`, `start-run` | **Arm async job**; `LabRecipeRunner.tick()` each postUpdate |

Default fresh run **skips** `MenuPanelScreen → PLAY` and calls
`charSelectScreen.open(false)` (same stability choice as Amethyst autoplay).
Optional L1 `menu-click PLAY` remains for true button paths.

Console L2 commands **return immediately** (`… armed`). Progress via
`art lab status` / `art lab dump` (`recipe` field). Do not block the game thread.

## Commands

### Dump

| Command | Role |
|---------|------|
| `art lab dump` | JSON: mode, menuScreen, buttons, hasResume, hasAbandon, inGame, inCombat, … |

### L1

| Command | Role |
|---------|------|
| `art lab clear-saves` | Delete per-class SaveAndContinue paths + `.backUp` |
| `art lab strip-resume` | Remove RESUME/ABANDON menu buttons; ensure PLAY |
| `art lab open-char-select` | `charSelectScreen.open(false)` |
| `art lab char <id>` | Select character by class name / label (paging if needed) |
| `art lab embark` | Character select confirm (Embark) |
| `art lab seed [text]` | Optional SeedPanel set; bare `seed` = skip/no-op ok |
| `art lab menu-click <RESULT>` | Click MenuButton by `ClickResult` name |
| `art lab abandon` | Best-effort abandon / leave run toward menu |
| `art lab abandon-confirm` | Confirm abandon popup only |
| `art lab return-menu` | Death/victory return-to-menu whitelist |
| `art lab proceed` | Overlay proceed hitbox |
| `art lab enter-event [id]` | Enter a supported vanilla event through the native map path |
| `art lab enter-room <rest|shop|treasure>` | Enter a reachable native room through the map path |
| `art lab enter-select <grid|hand>` | Open selection over the run's real deck or hand |
| `art lab tick` | One recipe engine step (debug) |

### L2

| Command | Role |
|---------|------|
| `art lab ensure-menu` | From gameplay/end → main menu (retry budget) |
| `art lab ensure-fresh-menu` | ensure-menu + clear-saves + strip-resume |
| `art lab start-run [char] [seed=…]` | ensure-fresh-menu → open char → select → seed? → embark |
| `art lab reset` | Alias of `ensure-fresh-menu` |

## Packages

| Type | Package |
|------|---------|
| Snapshot | `artframework.sts1.lab.LabStateSnapshot` / `StsLabState` |
| Host SPI | `artframework.sts1.lab.LabHost` / `FakeLabHost` / `StsLabHost` |
| L1 | `artframework.sts1.lab.StsLabNav` |
| L2 | `artframework.sts1.lab.StsLabRecipes` |
| Console | `artframework.console.ArtCommand` (`lab` subcommand) |

## Non-goals

- Consumer public API stability for lab commands
- Creating or mutating cards solely to manufacture a selection fixture
- Dual-device life
- Replacing Amethyst `single-room` harness (optional external; not default gate)
- Arbitrary reflection REPL

## Testing

| Layer | Focus |
|-------|--------|
| JUnit | `FakeLabHost` + recipe branch order / timeout / missing char |
| D1 | `tests/ui-scenarios/device/d1_lab_*.yaml` after cold start |

## Related

- [`docs/development/console-commands.md`](../development/console-commands.md)
- [`docs/development/android-device-lab.md`](../development/android-device-lab.md)
- [`docs/task.md`](../task.md) §18
- Amethyst: `AutoplayMainMenuActions`, `AutoplaySingleRoomRunner`
