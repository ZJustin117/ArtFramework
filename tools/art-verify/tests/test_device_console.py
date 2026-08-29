import sys
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import device_console


class DeviceConsoleTest(unittest.TestCase):
    def test_console_reconnects_again_after_reconnect_still_returns_empty_response(self):
        client = Mock()
        reconnect = Mock()
        client._art_reconnect = reconnect
        client.console_exec.side_effect = [
            {"executed": False, "error": "unexpected response: "},
            {"executed": False, "error": "unexpected response: "},
            {"executed": True, "output": "ok"},
        ]
        with patch.object(device_console.time, "sleep"):
            result = device_console.console_exec(client, "art lab dump")

        self.assertTrue(result["executed"])
        self.assertEqual(2, reconnect.call_count)
        self.assertEqual(3, client.console_exec.call_count)

    def test_console_reconnects_when_game_classloader_is_not_ready(self):
        client = Mock()
        reconnect = Mock()
        client._art_reconnect = reconnect
        client.console_exec.side_effect = [
            RuntimeError("BaseMod DevConsole not loaded"),
            {"executed": True, "output": "ok"},
        ]
        with patch.object(device_console.time, "sleep"):
            result = device_console.console_exec(client, "art lab dump")
        self.assertTrue(result["executed"])
        reconnect.assert_called_once()

    def test_probe_waits_for_new_sidecar_after_clearing_previous_snapshot(self):
        client = object()
        with patch.object(device_console, "clear_probe_sidecar", return_value=True), \
                patch.object(device_console, "console_exec", return_value={"executed": True, "output": "ok"}), \
                patch.object(device_console, "scrape_probe_sidecar", side_effect=[None, {"frame": "new"}]), \
                patch.object(device_console, "scrape_probe_log") as log, \
                patch.object(device_console.time, "sleep"):
            self.assertEqual({"frame": "new"}, device_console.probe_after_console(client))
            log.assert_not_called()

    def test_wait_for_command_log_ignores_previous_sequence(self):
        previous = {"sequence": 4, "status": "OK"}
        current = {"sequence": 5, "status": "OK"}
        with patch.object(
            device_console,
            "scrape_command_log",
            side_effect=[previous, current],
        ), patch.object(device_console.time, "sleep"):
            self.assertEqual(
                current,
                device_console.wait_for_command_log(
                    "art lab start-run IRONCLAD", previous, timeout_seconds=1
                ),
            )

    def test_wait_for_command_log_rejects_legacy_result_without_sequence(self):
        with patch.object(
            device_console,
            "scrape_command_log",
            return_value={"status": "OK", "command": "art lab dump"},
        ), patch.object(device_console.time, "sleep", side_effect=lambda _: None):
            self.assertIsNone(
                device_console.wait_for_command_log(
                    "art lab dump", None, timeout_seconds=0
                )
            )

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
