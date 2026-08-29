import json
import os
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ART_LAB = ROOT / "scripts" / "art-lab"


class ArtLabTest(unittest.TestCase):
    def test_ready_reads_oversized_status_result_from_file(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            slay_root = temp / "slay"
            tools_dir = slay_root / "scripts" / "tools"
            connector_dir = tools_dir / "connector"
            lib_dir = tools_dir / "lib"
            bin_dir = temp / "bin"
            out_dir = temp / "harness"
            for directory in (connector_dir, lib_dir, bin_dir, out_dir):
                directory.mkdir(parents=True, exist_ok=True)

            self._write(bin_dir / "adb", "#!/usr/bin/env sh\nexit 0\n", executable=True)
            self._write(
                tools_dir / "main.py",
                """
                import json
                import sys
                from pathlib import Path

                args = sys.argv[1:]
                out_dir = Path(args[args.index("-OutDir") + 1])
                result_dir = out_dir / "status"
                result_dir.mkdir(parents=True, exist_ok=True)
                result = {
                    "success": True,
                    "status": "ok",
                    "statusSnapshot": {
                        "observedState": "READY",
                        "latestLog": {"lastNonBlankLine": "ART_PROBE " + "x" * 200_000},
                    },
                }
                (result_dir / "result.json").write_text(json.dumps(result), encoding="utf-8")
                """,
            )
            self._write(connector_dir / "__init__.py", "")
            self._write(connector_dir / "__main__.py", "raise SystemExit(0)\n")
            self._write(
                connector_dir / "client.py",
                """
                class ConnectorClient:
                    def __init__(self, auto_start=False):
                        pass

                    def connect(self):
                        pass

                    def select(self, serial):
                        pass

                    def connect_stream(self, port):
                        return object()

                    def close(self):
                        pass
                """,
            )
            self._write(lib_dir / "__init__.py", "")
            self._write(
                lib_dir / "agent_client.py",
                """
                class AgentClient:
                    def __init__(self, stream):
                        pass

                    def ready(self):
                        return True

                    def close(self):
                        pass
                """,
            )

            env = os.environ.copy()
            env.update(
                {
                    "SLAY_THE_AMETHYST_ROOT": str(slay_root),
                    "ART_AMETHYST_TOOLS_DIR": str(tools_dir),
                    "ART_D1_SERIAL": "test-device",
                    "STS_CONNECTOR_PORT": "39999",
                    "ART_HARNESS_OUT_DIR": str(out_dir),
                    "ART_GAME_PROBE_PORT": "9099",
                    "PATH": f"{bin_dir}{os.pathsep}{env['PATH']}",
                }
            )
            completed = subprocess.run(
                [str(ART_LAB), "ready"],
                cwd=ROOT,
                env=env,
                text=True,
                capture_output=True,
                timeout=10,
                check=False,
            )

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertIn("success=True status=ok observedState=READY", completed.stdout)
            self.assertNotIn("Argument list too long", completed.stderr)

    def _write(self, path, content, executable=False):
        path.write_text(textwrap.dedent(content).lstrip(), encoding="utf-8")
        if executable:
            path.chmod(0o755)


if __name__ == "__main__":
    unittest.main()
