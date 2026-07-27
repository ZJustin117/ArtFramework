# API Stability Checklist

The public consumer surface is the `artframework.api` facade and the explicitly documented
contracts below. Consumers should compile against `ArtFramework.jar` with `compileOnly` and load
the jar as a separate ModTheSpire dependency.

## Stable Surface

- `artframework.api.ArtFramework`: registration, mount/unmount aliases, tree, component, ops, probe, render, host, and entities.
- `artframework.api.WindowDef`, `WindowClass`, and `WindowHandle`.
- `artframework.api.UiOps`, `UiOpResult`, and `UiProbe`.
- `artframework.core.UiTree`, `UiInstance`, `SignalHub`, `Theme`, `HostBackend`, `HostCapabilities`, and `UiComponent`.
- `artframework.c2.NativeTemplateIds`: canonical IDs use the `sts1.*` namespace.

## Compatibility Rules

- `open`, `bind`, and `close` remain aliases for the mount lifecycle for at least one minor release.
- Legacy `sts.*` IDs are accepted as input and canonicalized to `sts1.*`; new probe output uses canonical IDs.
- `UiProbe.SCHEMA_VERSION` changes only with a documented field migration.
- `UiOpResult.Status` is additive only within a minor release.
- `artframework.component`, `artframework.c1.layout`, and `artframework.render` types not listed above are implementation or extension surfaces and may evolve with explicit release notes.

## Release Gate

1. `./scripts/with-art-env.sh test`
2. `cd tools/art-verify && python3 -m unittest discover -s tests -v`
3. Build `ArtFramework.jar` and verify the manifest version and `ModTheSpire.json`.
4. `./scripts/verify-consumer-fixture.sh`
5. Deploy both ArtFramework and the consumer jar before D1 UI verification.
