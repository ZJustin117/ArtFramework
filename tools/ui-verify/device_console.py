"""Amethyst connector + game-probe console (same pattern as CrossSpire device-scenario-runner)."""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

from probe_parse import last_probe_from_text, parse_probe_line

DEFAULT_STS_LATEST_LOG = "/sdcard/Android/data/io.stamethyst/files/sts/latest.log"
PROBE_MARKER = "SPIREUI_PROBE"


def _env(key: str, default: str = "") -> str:
    return os.environ.get(key, default).strip()


def serial_d1() -> str:
    val = _env("SPIREUI_D1_SERIAL") or _env("STS_TEST_DEVICE")
    if not val:
        raise RuntimeError("missing env SPIREUI_D1_SERIAL")
    return val


def amethyst_root() -> Path:
    root = _env("SLAY_THE_AMETHYST_ROOT")
    if not root:
        tools = _env("SPIREUI_AMETHYST_TOOLS_DIR")
        if tools:
            root = str(Path(tools).resolve().parent.parent)
    if not root:
        raise RuntimeError("missing env SLAY_THE_AMETHYST_ROOT")
    return Path(root)


def agent_port() -> int:
    return int(_env("SPIREUI_GAME_PROBE_PORT", "9099") or "9099")


def connect_console(serial: Optional[str] = None) -> Tuple[Any, Any]:
    """Return (AgentClient, close_fn). Requires connector daemon on STS_CONNECTOR_PORT."""
    if not _env("STS_CONNECTOR_PORT"):
        raise RuntimeError("missing env STS_CONNECTOR_PORT")
    root = str(amethyst_root())
    if root not in sys.path:
        sys.path.insert(0, root)

    from scripts.tools.connector.client import ConnectorClient
    from scripts.tools.lib.agent_client import AgentClient

    ser = serial or serial_d1()
    port = agent_port()
    # harness-style: do not auto-start daemon; lab doc requires explicit start
    conn = ConnectorClient(auto_start=False)
    conn.connect()
    conn.select(ser)
    stream = conn.connect_stream(port=port)
    client = AgentClient(stream=stream)

    def close() -> None:
        try:
            client.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass

    return client, close


def console_exec(client: Any, command: str, *, retries: int = 3) -> Dict[str, Any]:
    last_err: Optional[str] = None
    for attempt in range(max(1, retries)):
        try:
            result = client.console_exec(command)
            if not isinstance(result, dict):
                last_err = f"unexpected result type {type(result)}"
            else:
                # game-probe sometimes returns empty/error right after READY
                err = result.get("error")
                if err and not result.get("executed", False):
                    last_err = str(err)
                else:
                    return result
        except Exception as e:
            last_err = f"{type(e).__name__}: {e}"
        if attempt + 1 < retries:
            import time

            time.sleep(0.8 * (attempt + 1))
    return {"executed": False, "error": last_err or "console_exec failed"}


def console_output_text(raw: Dict[str, Any]) -> str:
    for key in ("output", "result", "text", "message"):
        v = raw.get(key)
        if isinstance(v, str) and v.strip():
            return v
    return str(raw)


def scrape_probe_log(
    serial: Optional[str] = None,
    *,
    adb: str = "adb",
    log_path: str = DEFAULT_STS_LATEST_LOG,
    lines: int = 400,
) -> Optional[Any]:
    ser = serial or serial_d1()
    cmd = [
        adb,
        "-s",
        ser,
        "shell",
        f"tail -n {lines} {log_path} 2>/dev/null || true",
    ]
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    except Exception:
        return None
    text = proc.stdout or ""
    return last_probe_from_text(text)


def probe_after_console(
    client: Any,
    *,
    serial: Optional[str] = None,
    command: str = "spireui probe",
) -> Any:
    """Run spireui probe; prefer console body, else scrape STS latest.log for SPIREUI_PROBE."""
    raw = console_exec(client, command)
    text = console_output_text(raw) if raw else ""
    parsed = parse_probe_line(text) if text else None
    if parsed is None and text:
        parsed = last_probe_from_text(text)
    if parsed is not None:
        return parsed
    # Always scrape log: game-probe often returns only "ok"
    scraped = scrape_probe_log(serial)
    if scraped is not None:
        return scraped
    err = (raw or {}).get("error")
    raise RuntimeError(
        f"no {PROBE_MARKER} in console {text[:120]!r} or device log"
        + (f" (console error: {err})" if err else "")
        + " — is SpireUI.jar loaded and cold-started?"
    )
