# ArtFramework development infrastructure

Local build, pure JUnit, UI-verify tooling, and optional single-device jar deploy.

## Local config

| File | Committed | Content |
|------|-----------|---------|
| [`.env.example`](../../.env.example) | yes | Key names and placeholders |
| `.env.local` | no | Real paths and optional ADB serials |

```bash
cp .env.example .env.local
# edit paths; optionally set ART_D1_SERIAL when testing on-device
./scripts/with-art-env.sh test
```

OpenCode plugin [`.opencode/plugins/local-env.ts`](../../.opencode/plugins/local-env.ts) loads allowlisted keys for shell and developer/test subagents. Restart opencode after agent/plugin edits.

## OpenCode subagents

| Agent | Invoke | Role |
|-------|--------|------|
| `developer` | `@developer` | Scoped source/test/doc implementation; no commit, merge, push, deploy, or recursive delegation |
| `junit-test` | `@junit-test` | Default gate: `./scripts/with-art-env.sh test` (read-only) |
| `android-deploy-jar` | `@android-deploy-jar` | Build + push `ArtFramework.jar` (default D1); not semantic regression |
| `art-verify` | `@art-verify` | `tools/art-verify` fixture YAML + offline unittest; optional D1 later |
| `android-arthas` | `@android-arthas` | Read-only Android JVM diagnostics; bounded `start -> query -> stop`; not a default gate |

Recommended flow: parent scopes the task → `@developer` implements the bounded change → `@junit-test` verifies pure logic/API → `@art-verify` or device-specific agents run only when the touched area requires them → parent reviews the diff and owns final integration.

## Docs in this folder

| File | Content |
|------|---------|
| [`logic-layer-testing.md`](./logic-layer-testing.md) | Test pyramid, pure API rules, anti-patterns |
| [`ui-layer-verification.md`](./ui-layer-verification.md) | Intercept/trigger/C1 verify; tools/art-verify |
| [`console-commands.md`](./console-commands.md) | BaseMod `art` console reference (probe / op / ui / lab / gate / …) |
| [`../design/lab-run-nav.md`](../design/lab-run-nav.md) | Lab run navigation (`art lab`) for D1 |
| [`android-deploy.md`](./android-deploy.md) | Device jar push paths and force-stop |
| [`android-device-lab.md`](./android-device-lab.md) | Amethyst connector / harness / UI smoke (D1) |
| [`android-arthas.md`](./android-arthas.md) | Optional Arthas JVM diagnostics on the Android device |
| [`api-overview.md`](./api-overview.md) | Facade, typed domain, extension SPI, and host/lab API layers |
| [`api-stability.md`](./api-stability.md) | Stable consumer API and compatibility rules |
| [`consumer.md`](./consumer.md) | How downstream mods depend on `ArtFramework.jar` |
| [`../design/art-framework.md`](../design/art-framework.md) | ART presentation graph + Host SPI + milestone 12 |
| [`../design/backend-context.md`](../design/backend-context.md) | Backend / context / intents (milestone 15) |
| [`../design/c2-full-present.md`](../design/c2-full-present.md) | C2 full present hard-sync (15) |
| [`../design/host-assets.md`](../design/host-assets.md) | HostAssets packs / ResourceId (15) |
| [`../task.md`](../task.md) | Open implementation tasks |

## Boundary

- Default verification is **JUnit**. Device deploy is optional and does not replace API tests.
- ArtFramework may use **single-device** UI smoke (`ART_D1_SERIAL`) and optional read-only Arthas JVM diagnostics; neither replaces JUnit or `art-verify`.
