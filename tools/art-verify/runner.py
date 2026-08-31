"""Run one UI-verify scenario (fixture or device)."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

from assert_ops import AssertError, run_assert
from scenario_loader import expand_steps, load_scenario


def _repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def _default_out_dir() -> Path:
    env = os.environ.get("ART_UI_VERIFY_OUT_DIR", "").strip()
    if env:
        return Path(env)
    return _repo_root() / "debug-artifacts" / "art-verify"


def _load_fixture(scenario_path: Path, fixture_rel: Optional[str]) -> Any:
    if not fixture_rel:
        raise ValueError("fixture mode requires fixture: path")
    base = scenario_path.parent
    candidates = [
        base / fixture_rel,
        _repo_root() / "tests" / "ui-scenarios" / fixture_rel,
        _repo_root() / fixture_rel,
    ]
    for c in candidates:
        if c.is_file():
            return json.loads(c.read_text(encoding="utf-8"))
    raise FileNotFoundError(
        f"fixture not found: {fixture_rel} (searched {[str(x) for x in candidates]})"
    )


def _check_require(require: Dict[str, Any], *, device: bool) -> Optional[str]:
    env_keys = list((require.get("env") or []) if isinstance(require, dict) else [])
    if device:
        for k in (
            "ART_D1_SERIAL",
            "STS_CONNECTOR_PORT",
            "SLAY_THE_AMETHYST_ROOT",
        ):
            if k not in env_keys:
                env_keys.append(k)
    missing = []
    for k in env_keys:
        if not os.environ.get(str(k), "").strip():
            missing.append(str(k))
    if missing:
        return "missing env: " + ", ".join(missing)
    return None


def _harness_screenshot() -> Dict[str, Any]:
    """Run the existing Harness screenshot command without owning its lifecycle."""
    required = (
        "ART_D1_SERIAL",
        "STS_CONNECTOR_PORT",
        "ART_GAME_PROBE_PORT",
        "ART_HARNESS_OUT_DIR",
    )
    missing = [key for key in required if not os.environ.get(key, "").strip()]
    if not (os.environ.get("ART_AMETHYST_TOOLS_DIR", "").strip()
            or os.environ.get("SLAY_THE_AMETHYST_ROOT", "").strip()):
        missing.append("ART_AMETHYST_TOOLS_DIR or SLAY_THE_AMETHYST_ROOT")
    if missing:
        raise RuntimeError("screenshot requires env: " + ", ".join(missing))

    root = os.environ.get("SLAY_THE_AMETHYST_ROOT", "").strip()
    if not root:
        root = str(Path(os.environ["ART_AMETHYST_TOOLS_DIR"]).resolve().parents[1])
    out_dir = os.environ["ART_HARNESS_OUT_DIR"].strip()
    command = [
        sys.executable,
        str(Path(root) / "scripts" / "tools" / "main.py"),
        "sts-harness",
        "-Command", "screenshot",
        "-DeviceSerial", os.environ["ART_D1_SERIAL"].strip(),
        "-ConnectorPort", os.environ["STS_CONNECTOR_PORT"].strip(),
        "-AgentPort", os.environ["ART_GAME_PROBE_PORT"].strip(),
        "-OutDir", out_dir,
    ]
    env = os.environ.copy()
    env["SLAY_THE_AMETHYST_ROOT"] = root
    if env.get("ART_AMETHYST_TOOLS_DIR", "").strip():
        env["ART_AMETHYST_TOOLS_DIR"] = env["ART_AMETHYST_TOOLS_DIR"].strip()
    env["PYTHONPATH"] = root + (os.pathsep + env["PYTHONPATH"] if env.get("PYTHONPATH") else "")
    timeout_seconds = float(os.environ.get("ART_SCREENSHOT_TIMEOUT_SECONDS", "30"))
    if timeout_seconds <= 0:
        raise RuntimeError("ART_SCREENSHOT_TIMEOUT_SECONDS must be positive")
    try:
        proc = subprocess.run(
            command,
            cwd=root,
            env=env,
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
    except subprocess.TimeoutExpired as exc:
        raise RuntimeError(
            f"Harness screenshot timed out after {timeout_seconds:g}s"
        ) from exc
    result_path: Optional[Path] = None
    for line in (proc.stdout or "").splitlines():
        marker = "Harness result:"
        if marker in line:
            candidate = Path(line.split(marker, 1)[1].strip())
            if candidate.is_file():
                result_path = candidate
                break
    if result_path is None:
        candidates = sorted(Path(out_dir).glob("*/result.json"), key=lambda p: p.stat().st_mtime, reverse=True)
        if candidates:
            result_path = candidates[0]
    if result_path is None:
        raise RuntimeError(
            f"Harness screenshot produced no result.json (exit {proc.returncode}): "
            f"{(proc.stderr or proc.stdout or '').strip()[-500:]}"
        )
    harness_result = json.loads(result_path.read_text(encoding="utf-8"))
    success = harness_result.get("success") is True
    status = str(harness_result.get("status", "")).upper()
    if proc.returncode != 0 or not success or status not in {"OK", "SCREENSHOT_CAPTURED"}:
        raise RuntimeError(
            f"Harness screenshot failed: exit={proc.returncode}, success={harness_result.get('success')}, "
            f"status={harness_result.get('status')}, message={harness_result.get('message') or harness_result.get('error')}"
        )
    artifacts = harness_result.get("artifacts") or {}
    png_path = artifacts.get("screenshot")
    if not png_path or not Path(str(png_path)).is_file():
        raise RuntimeError(f"Harness screenshot result has no PNG: {png_path}")
    return {"result_json": str(result_path), "png": str(png_path), "harness": harness_result}


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
    client = None
    close_fn = None
    if mode == "fixture":
        try:
            last_probe = _load_fixture(path, sc.get("fixture"))
        except Exception as e:
            result["status"] = "fail"
            result["error"] = str(e)
            _write_result(result, out_dir)
            return result
    else:
        try:
            from device_console import connect_console

            client, close_fn = connect_console()
        except Exception as e:
            result["status"] = "fail"
            result["error"] = f"device setup: {type(e).__name__}: {e}"
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
                client=client,
            )
            result["steps"].append(rec)
            if rec.get("probe") is not None:
                last_probe = rec["probe"]
            if rec.get("status") == "fail":
                result["status"] = "fail"
                result["error"] = rec.get("error")
                break
            if rec.get("status") == "skip":
                result["status"] = "skip"
                result["error"] = rec.get("error")
                break
    except AssertError as e:
        result["status"] = "fail"
        result["error"] = str(e)
    except Exception as e:
        result["status"] = "fail"
        result["error"] = f"{type(e).__name__}: {e}"
    finally:
        if close_fn is not None:
            try:
                close_fn()
            except Exception:
                pass
    _write_result(result, out_dir)
    return result


def _run_step(
    step: Dict[str, Any],
    index: int,
    *,
    mode: str,
    last_probe: Any,
    vars_map: Dict[str, Any],
    client: Any,
) -> Dict[str, Any]:
    rec: Dict[str, Any] = {"index": index, "status": "pass", "step": step}
    if "screenshot" in step:
        if mode != "device":
            rec["status"] = "skip"
            rec["error"] = "screenshot is device-only and was not executed in fixture mode"
            return rec
        capture = _harness_screenshot()
        rec["screenshot"] = {
            "result_json": capture["result_json"],
            "png": capture["png"],
        }
        return rec

    if "wait_ms" in step:
        ms = int(step["wait_ms"])
        if mode == "device":
            time.sleep(ms / 1000.0)
        rec["wait_ms"] = ms
        return rec

    if "assert" in step:
        run_assert(last_probe, step["assert"], vars=vars_map)
        return rec

    if "wait_probe" in step:
        spec = step["wait_probe"]
        if not isinstance(spec, dict) or not isinstance(spec.get("assert"), dict):
            raise ValueError("wait_probe requires assert: mapping")
        timeout_ms = int(spec.get("timeout_ms", 30000))
        interval_ms = int(spec.get("interval_ms", 500))
        deadline = time.monotonic() + timeout_ms / 1000.0
        attempts = 0
        last_error = ""
        while True:
            attempts += 1
            if mode == "fixture":
                probe = last_probe
            else:
                from device_console import probe_after_console

                try:
                    probe = probe_after_console(client)
                except Exception as e:
                    last_error = f"probe unavailable: {type(e).__name__}: {e}"
                    if time.monotonic() >= deadline:
                        rec["status"] = "fail"
                        rec["error"] = f"wait_probe timeout after {timeout_ms}ms: {last_error}"
                        rec["attempts"] = attempts
                        return rec
                    if mode == "device":
                        time.sleep(max(0, interval_ms) / 1000.0)
                    continue
            try:
                run_assert(probe, spec["assert"], vars=vars_map)
                rec["probe"] = probe
                rec["attempts"] = attempts
                return rec
            except AssertError as e:
                last_error = str(e)
            if time.monotonic() >= deadline:
                rec["status"] = "fail"
                rec["error"] = f"wait_probe timeout after {timeout_ms}ms: {last_error}"
                rec["probe"] = probe
                rec["attempts"] = attempts
                return rec
            if mode == "device":
                time.sleep(max(0, interval_ms) / 1000.0)

    if "probe" in step:
        if mode == "fixture":
            rec["probe"] = last_probe
            return rec
        from device_console import probe_after_console

        rec["probe"] = probe_after_console(client)
        return rec

    if "console" in step or "op" in step:
        if mode == "fixture":
            rec["status"] = "skip"
            rec["error"] = "op/console ignored in fixture mode"
            return rec
        from device_console import console_exec, console_exec_once, scrape_command_log, wait_for_command_log

        if "console" in step:
            cmd = str(step["console"])
        else:
            # op: short form → art op …
            op = step["op"]
            if isinstance(op, str):
                cmd = "art op " + op
            elif isinstance(op, list):
                cmd = "art op " + " ".join(str(x) for x in op)
            else:
                cmd = "art op " + str(op)
        previous_command = scrape_command_log(cmd) if cmd.startswith("art ") else None
        raw = console_exec_once(cmd) if cmd.startswith("art ") else console_exec(client, cmd)
        rec["console"] = cmd
        rec["console_raw"] = {
            k: raw.get(k) for k in ("executed", "command", "output", "error") if k in raw
        }
        # ART commands publish a structured result. Prefer that fresh game-side evidence when the
        # connector loses the protocol response after executing the command.
        if mode == "device":
            command = (
                wait_for_command_log(cmd, previous_command)
                if cmd.startswith("art ")
                else scrape_command_log(cmd)
            )
            if command is not None:
                rec["command_log"] = command
                if str(command.get("status", "")).upper() == "ERROR":
                    rec["status"] = "fail"
                    rec["error"] = str(command.get("message") or "command failed")
            elif cmd.startswith("art lab "):
                rec["status"] = "fail"
                rec["error"] = "no fresh ART_COMMAND result for lab command"
            elif raw.get("executed") is False and raw.get("error"):
                rec["status"] = "fail"
                rec["error"] = str(raw.get("error"))
        elif raw.get("executed") is False and raw.get("error"):
            rec["status"] = "fail"
            rec["error"] = str(raw.get("error"))
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
    # drop huge raw if present
    slim = dict(result)
    path.write_text(json.dumps(slim, indent=2, sort_keys=True, default=str) + "\n", encoding="utf-8")
    result["out_file"] = str(path)


def run_files(
    paths: List[Path],
    *,
    force_device: bool = False,
    out_dir: Optional[Path] = None,
) -> List[Dict[str, Any]]:
    return [run_scenario(p, force_device=force_device, out_dir=out_dir) for p in paths]
