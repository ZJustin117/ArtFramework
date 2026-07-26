"""Run one UI-verify scenario (fixture or device stub)."""

from __future__ import annotations

import json
import os
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

from assert_ops import AssertError, run_assert
from scenario_loader import expand_steps, load_scenario


def _repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def _default_out_dir() -> Path:
    env = os.environ.get("SPIREUI_UI_VERIFY_OUT_DIR", "").strip()
    if env:
        return Path(env)
    return _repo_root() / "debug-artifacts" / "ui-verify"


def _load_fixture(scenario_path: Path, fixture_rel: Optional[str]) -> Any:
    if not fixture_rel:
        raise ValueError("fixture mode requires fixture: path")
    base = scenario_path.parent
    # allow tests/ui-scenarios relative to repo
    candidates = [
        base / fixture_rel,
        _repo_root() / "tests" / "ui-scenarios" / fixture_rel,
        _repo_root() / fixture_rel,
    ]
    for c in candidates:
        if c.is_file():
            return json.loads(c.read_text(encoding="utf-8"))
    raise FileNotFoundError(f"fixture not found: {fixture_rel} (searched {[str(x) for x in candidates]})")


def _check_require(require: Dict[str, Any], *, device: bool) -> Optional[str]:
    env_keys = list((require.get("env") or []) if isinstance(require, dict) else [])
    if device and "SPIREUI_D1_SERIAL" not in env_keys:
        env_keys.append("SPIREUI_D1_SERIAL")
    missing = []
    for k in env_keys:
        if not os.environ.get(str(k), "").strip():
            missing.append(str(k))
    if missing:
        return "missing env: " + ", ".join(missing)
    return None


def run_scenario(
    path: Path,
    *,
    force_device: bool = False,
    out_dir: Optional[Path] = None,
) -> Dict[str, Any]:
    sc = load_scenario(path)
    mode = "device" if force_device else sc["mode"]
    result: Dict[str, Any] = {
        "name": sc["name"],
        "path": sc["path"],
        "mode": mode,
        "status": "pass",
        "steps": [],
        "error": None,
    }
    skip = _check_require(sc.get("require") or {}, device=(mode == "device"))
    if skip:
        result["status"] = "skip"
        result["error"] = skip
        _write_result(result, out_dir)
        return result

    last_probe: Any = None
    vars_map: Dict[str, Any] = {}
    if mode == "fixture":
        try:
            last_probe = _load_fixture(path, sc.get("fixture"))
        except Exception as e:
            result["status"] = "fail"
            result["error"] = str(e)
            _write_result(result, out_dir)
            return result

    steps = expand_steps(sc["steps"])
    try:
        for i, step in enumerate(steps):
            rec = _run_step(
                step,
                i,
                mode=mode,
                last_probe=last_probe,
                vars_map=vars_map,
            )
            result["steps"].append(rec)
            if rec.get("probe") is not None:
                last_probe = rec["probe"]
            if rec.get("status") == "fail":
                result["status"] = "fail"
                result["error"] = rec.get("error")
                break
            if rec.get("status") == "skip":
                # Stop remaining steps (e.g. device probe not ready)
                result["status"] = "skip"
                result["error"] = rec.get("error")
                break
    except AssertError as e:
        result["status"] = "fail"
        result["error"] = str(e)
    except Exception as e:
        result["status"] = "fail"
        result["error"] = f"{type(e).__name__}: {e}"

    _write_result(result, out_dir)
    return result


def _run_step(
    step: Dict[str, Any],
    index: int,
    *,
    mode: str,
    last_probe: Any,
    vars_map: Dict[str, Any],
) -> Dict[str, Any]:
    rec: Dict[str, Any] = {"index": index, "status": "pass", "step": step}
    if "wait_ms" in step:
        ms = int(step["wait_ms"])
        if mode == "device":
            time.sleep(ms / 1000.0)
        rec["wait_ms"] = ms
        return rec

    if "assert" in step:
        run_assert(last_probe, step["assert"], vars=vars_map)
        return rec

    if "probe" in step:
        if mode == "fixture":
            # fixture already loaded; optional re-load path
            rec["probe"] = last_probe
            return rec
        # device: skip until spireui probe console exists (task 6.6)
        rec["status"] = "skip"
        rec["error"] = (
            "device probe not implemented yet (need SpireUI console SPIREUI_PROBE)"
        )
        return rec

    if "op" in step or "console" in step:
        if mode == "fixture":
            rec["status"] = "skip"
            rec["error"] = "op/console ignored in fixture mode"
            return rec
        rec["status"] = "skip"
        rec["error"] = "device op/console not implemented yet"
        return rec

    if "set" in step and isinstance(step["set"], dict):
        for k, v in step["set"].items():
            vars_map[str(k)] = v
        return rec

    raise ValueError(f"unknown step keys: {list(step.keys())}")


def _write_result(result: Dict[str, Any], out_dir: Optional[Path]) -> None:
    dest = out_dir or _default_out_dir()
    dest.mkdir(parents=True, exist_ok=True)
    name = result.get("name") or "scenario"
    safe = "".join(c if c.isalnum() or c in "-_" else "_" for c in str(name))
    path = dest / f"{safe}.json"
    path.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    result["out_file"] = str(path)


def run_files(
    paths: List[Path],
    *,
    force_device: bool = False,
    out_dir: Optional[Path] = None,
) -> List[Dict[str, Any]]:
    return [run_scenario(p, force_device=force_device, out_dir=out_dir) for p in paths]
