import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from device_lock import D1Lock, _lock_path


class D1LockTest(unittest.TestCase):
    def setUp(self):
        self.runtime = tempfile.TemporaryDirectory()
        self.previous_runtime = os.environ.get("XDG_RUNTIME_DIR")
        self.previous_timeout = os.environ.get("ART_DEVICE_LOCK_TIMEOUT_SECONDS")
        self.previous_held = os.environ.get("ART_D1_LOCK_HELD")
        os.environ["XDG_RUNTIME_DIR"] = self.runtime.name
        os.environ["ART_DEVICE_LOCK_TIMEOUT_SECONDS"] = "0"
        os.environ.pop("ART_D1_LOCK_HELD", None)

    def tearDown(self):
        self._restore("XDG_RUNTIME_DIR", self.previous_runtime)
        self._restore("ART_DEVICE_LOCK_TIMEOUT_SECONDS", self.previous_timeout)
        self._restore("ART_D1_LOCK_HELD", self.previous_held)
        self.runtime.cleanup()

    @staticmethod
    def _restore(key, value):
        if value is None:
            os.environ.pop(key, None)
        else:
            os.environ[key] = value

    def test_busy_lock_reports_holder_without_waiting(self):
        serial = "test-device-lock"
        path = _lock_path(serial)
        info = Path(f"{path}.info")
        info.write_text("label=verification\npid=1234\n", encoding="utf-8")
        holder = subprocess.Popen(
            [
                sys.executable,
                "-c",
                "import fcntl, sys; f=open(sys.argv[1], 'a+'); fcntl.flock(f, fcntl.LOCK_EX); print('held', flush=True); sys.stdin.read()",
                str(path),
            ],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            text=True,
        )
        try:
            self.assertEqual("held\n", holder.stdout.readline())
            with self.assertRaisesRegex(
                RuntimeError,
                r"D1 device lock unavailable after 0s;.*holder=label=verification pid=1234",
            ):
                with D1Lock(serial):
                    pass
        finally:
            holder.stdin.close()
            holder.wait(timeout=5)
            holder.stdout.close()

    def test_outer_wrapper_marker_skips_nested_lock(self):
        os.environ["ART_D1_LOCK_HELD"] = "1"
        with D1Lock("test-device-lock") as lock:
            self.assertIsNone(lock.handle)


if __name__ == "__main__":
    unittest.main()
