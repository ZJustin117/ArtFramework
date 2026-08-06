# Spine 4.2 Asset Tests

This is a developer-only asset test layer. It is intentionally outside
`src/test/java` and is never included in `ArtFramework-Spine42Runtime.jar`.

The tests require either:

- `ART_STS2_ASSET_JAR`, pointing to a generated asset bundle; or
- `ART_STS2_ROOT`, pointing to a local STS2 checkout. In that case the runner first invokes
  `scripts/package-sts2-assets.sh`.

Run from the repository root:

```bash
./tests/spine42-assets/run.sh
```

These tests do not run as part of `./scripts/with-art-env.sh test`. They inspect the bundle
layout and metadata without loading game/runtime classes. The D1 scenarios under
`tests/ui-scenarios/device/` perform the device-side runtime load after the two developer jars
have been pushed with `scripts/deploy-spine42-d1.sh`.
