# SpireUI UI-verify

Minimal YAML runner for **UI-layer** checks (intercept / trigger / C1).  
Not CrossSpire life/co-op scenarios.

```bash
pip install -r requirements.txt
python3 -m unittest discover -s tests -v

# Fixture scenarios (no device)
python3 run.py ../../tests/ui-scenarios/fixtures/f1_probe_shape.yaml
```

See [`docs/development/ui-layer-verification.md`](../../docs/development/ui-layer-verification.md).

Device mode (`--device`): see `docs/development/android-device-lab.md`  
(connector + `enabled_mods.txt` + harness READY + `spireui probe` / log scrape).
