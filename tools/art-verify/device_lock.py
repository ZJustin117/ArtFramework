"""Per-D1 OS lock for device-mode UI verification."""

from __future__ import annotations

import fcntl
import hashlib
import os
import time
from contextlib import AbstractContextManager
from pathlib import Path


def _timeout_seconds() -> int:
    value = os.environ.get("ART_DEVICE_LOCK_TIMEOUT_SECONDS", "30").strip()
    return int(value) if value.isdigit() else 30


def _lock_path(serial: str) -> Path:
    digest = hashlib.sha256(serial.encode("utf-8")).hexdigest()[:24]
    runtime = Path(os.environ.get("XDG_RUNTIME_DIR", "/tmp"))
    directory = runtime / "artframework-device-locks"
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    return directory / f"d1-{digest}.lock"


class D1Lock(AbstractContextManager["D1Lock"]):
    def __init__(self, serial: str) -> None:
        self.serial = serial
        self.path = _lock_path(serial)
        self.handle = None

    def __enter__(self) -> "D1Lock":
        if os.environ.get("ART_D1_LOCK_HELD") == "1":
            return self
        self.handle = self.path.open("a+")
        timeout = _timeout_seconds()
        deadline = time.monotonic() + timeout
        while True:
            try:
                fcntl.flock(self.handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
                return self
            except BlockingIOError:
                if time.monotonic() >= deadline:
                    info = self.path.with_suffix(".lock.info")
                    holder = "unknown"
                    try:
                        holder = " ".join(info.read_text(encoding="utf-8").split()) or holder
                    except OSError:
                        pass
                    self.handle.close()
                    self.handle = None
                    raise RuntimeError(
                        f"D1 device lock unavailable after {timeout}s; lock={self.path}; holder={holder}"
                    )
                time.sleep(0.1)

    def __exit__(self, *_: object) -> None:
        if self.handle is not None:
            fcntl.flock(self.handle.fileno(), fcntl.LOCK_UN)
            self.handle.close()
            self.handle = None
