# ArtFramework UI-verify

Minimal YAML runner for **UI-layer** checks (intercept / trigger / C1).  
Not multiplayer life/co-op scenarios.

```bash
pip install -r requirements.txt
python3 -m unittest discover -s tests -v

# Fixture scenarios (no device)
python3 run.py ../../tests/ui-scenarios/fixtures/f1_probe_shape.yaml
```

See [`docs/development/ui-layer-verification.md`](../../docs/development/ui-layer-verification.md).

## Screenshot pixel comparison

Compare a real Harness `screencap` PNG with a checked-in/reference screenshot:

```bash
python3 compare_png.py reference.png harness-screencap.png \
  --threshold 2 --max-diff-pixels 20 --max-diff-ratio 0.001 --diff debug-artifacts/diff.png
```

Use `--crop X,Y,W,H` to compare only the same non-negative rectangular region in both same-sized images. The JSON output includes `crop` as `[X, Y, W, H]`, or `null` when no crop is requested; the optional diff PNG is cropped to the same region.

The command accepts non-interlaced 8-bit RGB or RGBA PNGs, including RGB PNGs with the standard 6-byte `tRNS` transparent color key (decoded to RGBA). `--max-diff-pixels` must be at least 0 and `--max-diff-ratio` must be between 0 and 1; invalid values print usage and exit 2. Images with different dimensions fail comparison. The optional diff is a red heatmap. This is a D1 evidence tool: fixture/offline runs do not render real GL and must not be described as pixel parity.

Device `screenshot` steps bound the Harness subprocess to 30 seconds by default. Set `ART_SCREENSHOT_TIMEOUT_SECONDS` to a positive value when the device transport needs a different limit; a timeout is recorded as a failed scenario step.

Device mode (`--device`): see `docs/development/android-device-lab.md`  
(connector + `enabled_mods.txt` + harness READY + `art probe` / log scrape).

The device-only `screenshot` step invokes the existing Amethyst Harness screenshot command and records both
the Harness `result.json` and captured PNG. Fixture mode skips this step because it cannot produce real GL.
Immediately after (or later than) a device `screenshot`, `compare_screenshot` compares the most recent
capture with a developer-local PNG using the existing dependency-free comparator:

```yaml
- screenshot: true
- compare_screenshot:
    reference: ${ART_SPINE42_REFERENCE_PNG}
    reference_kind: native_capture
    crop: ${ART_SPINE42_CROP}
    threshold: 2
    max_diff_pixels: 20
    max_diff_ratio: 0.001
    diff: debug-artifacts/my-screen-diff.png
```

The step is device-only and requires a prior screenshot; fixture runs skip it. It records image paths,
reference provenance (`reference_kind`, when supplied), crop, dimensions, difference metrics, and limits in
the scenario result. Relative `reference` and `diff` paths
are resolved relative to the scenario file; absolute paths are used as-is. A whole-path `${ENV_KEY}` value
is also supported and resolves only that environment variable; an unset key fails while reporting its key
name, never its value. `crop` also accepts a whole-value `${ENV_KEY}` whose value must be `X,Y,W,H`; an unset
or malformed key fails with its key name and crop configuration. Literal four-integer list crops remain supported.
Spine42 uses `ART_SPINE42_REFERENCE_PNG` and requires `ART_SPINE42_CROP`; `ART_SPINE42_DIFF_PNG` is optional when a
developer wants to override the diff output path. Missing files, invalid config, size mismatches, and limit
violations fail the step. Reference images are developer-local and must not be committed.
Use `tests/ui-scenarios/device/d1_spine42_screenshot.yaml` for a Spine42 `idle_loop` capture. Reference PNGs
for comparison must be paired native captures from the same fixed frozen state (`reference_kind: native_capture`);
the scenario captures ART output and does not generate or claim native pixels.
