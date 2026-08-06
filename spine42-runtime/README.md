# Optional Spine 4.2 Runtime

This is an independent optional artifact. It is not part of the main `ArtFramework.jar` build.

```bash
./scripts/build-spine42-runtime.sh
```

Output:

```text
spine42-runtime/build/libs/ArtFramework-Spine42Runtime.jar
```

This sub-build contains no test source set and does not consume `ART_STS2_ROOT`,
`ART_STS2_ASSET_JAR`, or any STS2 asset. The generated jar contains the relocated Spine runtime
and the Spine Runtimes license notice, but does not contain libGDX or unrelocated Spine classes.

The current build is the packaging and class-isolation layer. The runtime renderer adapter must
still be validated against the host's libGDX 1.9.5 API before enabling live `spine42` rendering.
