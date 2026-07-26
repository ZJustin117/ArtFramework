"""Load SpireUI UI-verify YAML scenarios (v1)."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Dict, List

try:
    import yaml
except ImportError:  # pragma: no cover
    yaml = None  # type: ignore


def load_scenario(path: Path) -> Dict[str, Any]:
    if yaml is None:
        raise RuntimeError("PyYAML required: pip install -r tools/ui-verify/requirements.txt")
    raw = path.read_text(encoding="utf-8")
    data = yaml.safe_load(raw)
    if not isinstance(data, dict):
        raise ValueError(f"scenario root must be mapping: {path}")
    name = data.get("name") or path.stem
    schema = data.get("schemaVersion", 1)
    if schema != 1:
        raise ValueError(f"unsupported schemaVersion={schema} in {path}")
    mode = str(data.get("mode") or "fixture")
    if mode not in ("fixture", "device"):
        raise ValueError(f"mode must be fixture|device: {mode}")
    steps = data.get("steps")
    if not isinstance(steps, list):
        raise ValueError(f"steps must be a list: {path}")
    return {
        "name": name,
        "schemaVersion": schema,
        "mode": mode,
        "fixture": data.get("fixture"),
        "device": data.get("device") or "d1",
        "require": data.get("require") or {},
        "steps": steps,
        "path": str(path),
    }


def expand_steps(steps: List[Any]) -> List[Dict[str, Any]]:
    out: List[Dict[str, Any]] = []
    for i, step in enumerate(steps):
        if not isinstance(step, dict):
            raise ValueError(f"step {i} must be mapping")
        out.append(step)
    return out
