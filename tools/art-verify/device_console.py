"""Amethyst connector + game-probe console for ArtFramework device scenarios."""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, Optional, Tuple

from probe_parse import last_probe_from_text, parse_probe_line
from command_parse import last_command_for_text

DEFAULT_STS_LATEST_LOG = "/sdcard/Android/data/io.stamethyst/files/sts/latest.log"
DEFAULT_STS_PROBE_SIDECAR = "/sdcard/Android/data/io.stamethyst/files/sts/art_probe_latest.log"
PROBE_MARKER = "ART_PROBE"


def _env(key: str, default: str = "") -> str:
    return os.environ.get(key, default).strip()


def serial_d1() -> str:
    val = _env("ART_D1_SERIAL") or _env("STS_TEST_DEVICE")
    if not val:
        raise RuntimeError("missing env ART_D1_SERIAL")
    return val


def amethyst_root() -> Path:
    root = _env("SLAY_THE_AMETHYST_ROOT")
    if not root:
        tools = _env("ART_AMETHYST_TOOLS_DIR")
        if tools:
            root = str(Path(tools).resolve().parent.parent)
    if not root:
        raise RuntimeError("missing env SLAY_THE_AMETHYST_ROOT")
    return Path(root)


def agent_port() -> int:
    return int(_env("ART_GAME_PROBE_PORT", "9099") or "9099")


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

    def reconnect() -> None:
        nonlocal conn, stream
        try:
            client.close()
        except Exception:
            pass
        try:
            conn.close()
        except Exception:
            pass
        conn = ConnectorClient(auto_start=False)
        conn.connect()
        conn.select(ser)
        stream = conn.connect_stream(port=port)
        client._stream = stream

    client._art_reconnect = reconnect

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
                    if "unexpected response:" in last_err and not getattr(client, "_art_reconnected", False):
                        reconnect = getattr(client, "_art_reconnect", None)
                        if reconnect is not None:
                            reconnect()
                            client._art_reconnected = True
                            continue
                else:
                    client._art_reconnected = False
                    return result
        except Exception as e:
            last_err = f"{type(e).__name__}: {e}"
            if isinstance(e, (BrokenPipeError, ConnectionResetError, ConnectionAbortedError, EOFError)):
                reconnect = getattr(client, "_art_reconnect", None)
                if reconnect is not None and not getattr(client, "_art_reconnected", False):
                    reconnect()
                    client._art_reconnected = True
                    continue
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


def scrape_command_log(
    command: str, serial: Optional[str] = None, *, adb: str = "adb", lines: int = 400
) -> Optional[Dict[str, Any]]:
    ser = serial or serial_d1()
    text = ""
    try:
        root = str(amethyst_root())
        if root not in sys.path:
            sys.path.insert(0, root)
        from scripts.tools.connector.client import ConnectorClient

        conn = ConnectorClient(auto_start=False)
        conn.connect()
        conn.select(ser)
        response = conn.shell(
            f"tail -n {lines} {DEFAULT_STS_LATEST_LOG} 2>/dev/null || true",
            timeout_ms=10000,
        )
        conn.close()
        if isinstance(response, dict):
            text = str(response.get("stdout") or response.get("output") or "")
    except Exception:
        text = ""
    if not text:
        cmd = [adb, "-s", ser, "shell", f"tail -n {lines} {DEFAULT_STS_LATEST_LOG} 2>/dev/null || true"]
        try:
            proc = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
            text = proc.stdout or ""
        except Exception:
            return None
    return last_command_for_text(text, command)


def scrape_probe_sidecar(serial: Optional[str] = None) -> Optional[Any]:
    if not _env("STS_CONNECTOR_PORT"):
        return None
    root = str(amethyst_root())
    if root not in sys.path:
        sys.path.insert(0, root)
    try:
        from scripts.tools.connector.client import ConnectorClient

        conn = ConnectorClient(auto_start=False)
        conn.connect()
        conn.select(serial or serial_d1())
        resp = conn.shell(f"cat '{DEFAULT_STS_PROBE_SIDECAR}' 2>/dev/null || true", timeout_ms=10000)
        conn.close()
    except Exception:
        return None
    text = ""
    if isinstance(resp, dict):
        text = str(resp.get("stdout") or resp.get("output") or "")
    parsed = parse_probe_line(text) if text else None
    if parsed is None and text:
        parsed = last_probe_from_text(text)
    if isinstance(parsed, dict):
        lab = parsed.get("lab")
        if isinstance(lab, dict) and lab.get("message") == "host not ready":
            return None
    return parsed


def probe_after_console(
    client: Any,
    *,
    serial: Optional[str] = None,
    command: str = "art probe",
) -> Any:
    """Run art probe; prefer console body, else scrape STS latest.log for ART_PROBE."""
    raw = console_exec(client, command)
    text = console_output_text(raw) if raw else ""
    parsed = parse_probe_line(text) if text else None
    if parsed is None and text:
        parsed = last_probe_from_text(text)
    if parsed is not None:
        return parsed
    sidecar = scrape_probe_sidecar(serial)
    if sidecar is not None:
        return sidecar
    # Always scrape log: game-probe often returns only "ok"
    scraped = scrape_probe_log(serial)
    if scraped is not None:
        return scraped
    err = (raw or {}).get("error")
    raise RuntimeError(
        f"no {PROBE_MARKER} in console {text[:120]!r} or device log"
        + (f" (console error: {err})" if err else "")
        + " — is ArtFramework.jar loaded and cold-started?"
    )
