# ArtFramework pure API / logic-layer testing

## Pyramid

```
rare:      CrossSpire dual-device life (other repo)
optional:  single-device UI smoke (@art-verify after @android-deploy-jar)
tooling:   tools/art-verify offline unittest + fixture YAML
default:   pure registry + C1/C2 templates + (soon) UiOps/UiProbe JUnit
```

**Default gate:** `./scripts/with-art-env.sh test` or OpenCode `@junit-test`.

Dual-device life suites and multiplayer protocol assertions are **out of scope** for ArtFramework. Consumers (e.g. CrossSpire) own co-op E2E. UI intercept/trigger fixtures: [`ui-layer-verification.md`](./ui-layer-verification.md). Optional JVM diagnostics: [`android-arthas.md`](./android-arthas.md) / `@android-arthas` (not a semantic gate).

## What to test here

- `ArtFramework.register` / `open` / `bind` / `close` / `find` / `listOpenIds`
- `WindowDef` validation and resource defaults
- C1: `LayoutLoader` parse / classpath load; `WindowManager` + `SyntheticRuntime` open/close
- C1: `FakeStageBackend` attach/detach with open/close (no GL)
- C2: `MapTemplate` intercept chain, pins, bind/unbind via `ArtFramework`
- C2: `EventTemplate` / `SelectTemplate` / `EndTurnTemplate` gates + bind
- C2: `EntityPresent` / `DefaultEntityPresent` attach-sync-layout-detach (no GL)
- Pure `UiOps` / `UiProbe` / `UiOpResult` + `FakeNativeOps` (`UiOpsProbeTest`)
- Pure `NativeUiHooks` gate entry (`NativeUiHooksTest`)
- Composition: `UiNodeLoader` / `LayoutEngine` / `TemplateExpander` / `ComponentRegistry` / `Composition` (no GL)
- Render: `RenderHost` bind/sync/probe; `ShaderRuntime` failure paths without GL (no live ShaderProgram in default gate)

## What not to test in unit suite

- `ArtFrameworkMod` BaseMod subscribe (needs full STS/BaseMod on test classpath)
- scene2d draw frames, real `StageHost`/`StsSkin` GL init, `@SpirePatch` bytecode
  (document design first; add focused tests only when behavior is pure)
- CrossSpire protocol, party election, combat phase

## Rules

1. Prefer **pure** static/API tests (JUnit 4, no Mockito required for scaffold).
2. **Do not** mirror production branching with copy-pasted `if` trees inside tests; assert observable outcomes.
3. Use `ArtFramework.resetForTests()` in `@After` so cases stay isolated.
4. Filter when iterating: `./scripts/with-art-env.sh test --tests 'artframework.api.*'`.
5. After a coherent API change, delegate `@junit-test` rather than pasting full gradle logs into the parent chat.
6. After art-verify runner changes, run `cd tools/art-verify && python3 -m unittest discover -s tests -v` (or `@art-verify`).

## Related

- [`docs/design/dual-track.md`](../design/dual-track.md)
- [`docs/design/ui-ops-probe.md`](../design/ui-ops-probe.md)
- [`docs/development/ui-layer-verification.md`](./ui-layer-verification.md)
- [`docs/task.md`](../task.md)
- [`AGENTS.md`](../../AGENTS.md)
