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
Use `tests/ui-scenarios/device/d1_spine42_screenshot.yaml` for a Spine42 `idle_loop` capture. Reference PNGs
for comparison must still come from an independent native capture; the scenario does not claim pixel parity.
