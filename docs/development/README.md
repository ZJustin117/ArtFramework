# ArtFramework development infrastructure

Local build, pure JUnit, UI-verify tooling, and optional single-device jar deploy. **Not** CrossSpire multiplayer life/harness infrastructure.

## Local config

| File | Committed | Content |
|------|-----------|---------|
| [`.env.example`](../../.env.example) | yes | Key names and placeholders |
| `.env.local` | no | Real paths and optional ADB serials |

```bash
cp .env.example .env.local
# edit paths; set ART_D1_SERIAL to the same device as CrossSpire D1 when testing on-device
./scripts/with-art-env.sh test
```

OpenCode plugin [`.opencode/plugins/local-env.ts`](../../.opencode/plugins/local-env.ts) loads allowlisted keys for shell and test subagents. Restart opencode after agent/plugin edits.

## OpenCode subagents

| Agent | Invoke | Role |
|-------|--------|------|
| `junit-test` | `@junit-test` | Default gate: `./scripts/with-art-env.sh test` (read-only) |
| `android-deploy-jar` | `@android-deploy-jar` | Build + push `ArtFramework.jar` (default D1); not semantic regression |
| `art-verify` | `@art-verify` | `tools/art-verify` fixture YAML + offline unittest; optional D1 later |

## Docs in this folder

| File | Content |
|------|---------|
| [`logic-layer-testing.md`](./logic-layer-testing.md) | Test pyramid, pure API rules, anti-patterns |
| [`ui-layer-verification.md`](./ui-layer-verification.md) | Intercept/trigger/C1 verify; tools/art-verify |
| [`android-deploy.md`](./android-deploy.md) | Device jar push paths and force-stop |
| [`android-device-lab.md`](./android-device-lab.md) | Amethyst connector / harness / UI smoke (D1) |
| [`consumer.md`](./consumer.md) | How CrossSpire (or others) depend on `ArtFramework.jar` |
| [`../design/art-framework.md`](../design/art-framework.md) | ART presentation graph + Host SPI + milestone 12 |
## Boundary

- Default verification is **JUnit**. Device deploy is optional and does not replace API tests.
- Dual-device life YAML, connector, and Arthas are **CrossSpire** gates. ArtFramework may use **single-device** UI smoke (`ART_D1_SERIAL`) only.
