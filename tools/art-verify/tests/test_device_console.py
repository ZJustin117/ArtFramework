import sys
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import device_console


class DeviceConsoleTest(unittest.TestCase):
    def test_probe_waits_for_new_sidecar_after_clearing_previous_snapshot(self):
        client = object()
        with patch.object(device_console, "clear_probe_sidecar", return_value=True), \
                patch.object(device_console, "console_exec", return_value={"executed": True, "output": "ok"}), \
                patch.object(device_console, "scrape_probe_sidecar", side_effect=[None, {"frame": "new"}]), \
                patch.object(device_console, "scrape_probe_log") as log, \
                patch.object(device_console.time, "sleep"):
            self.assertEqual({"frame": "new"}, device_console.probe_after_console(client))
            log.assert_not_called()

    def test_probe_does_not_fall_back_to_stale_log_after_successful_clear(self):
        client = object()
        with patch.object(device_console, "clear_probe_sidecar", return_value=True), \
                patch.object(device_console, "console_exec", return_value={"executed": True, "output": "ok"}), \
                patch.object(device_console, "scrape_probe_sidecar", return_value=None), \
                patch.object(device_console, "scrape_probe_log") as log, \
                patch.object(device_console.time, "sleep"):
            with self.assertRaisesRegex(RuntimeError, "fresh sidecar"):
                device_console.probe_after_console(client)
            log.assert_not_called()

    def test_probe_keeps_log_fallback_when_sidecar_cannot_be_cleared(self):
        client = object()
        with patch.object(device_console, "clear_probe_sidecar", return_value=False), \
                patch.object(device_console, "console_exec", return_value={"executed": True, "output": "ok"}), \
                patch.object(device_console, "scrape_probe_sidecar", return_value=None), \
                patch.object(device_console, "scrape_probe_log", return_value={"frame": "log"}):
            self.assertEqual({"frame": "log"}, device_console.probe_after_console(client))


if __name__ == "__main__":
    unittest.main()
