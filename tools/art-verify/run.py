#!/usr/bin/env python3
"""CLI: run ArtFramework UI-verify YAML scenarios."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

_HERE = Path(__file__).resolve().parent
if str(_HERE) not in sys.path:
    sys.path.insert(0, str(_HERE))

from runner import run_files  # noqa: E402


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description="ArtFramework UI-layer YAML verifier")
    p.add_argument("scenarios", nargs="+", type=Path, help="YAML scenario paths")
    p.add_argument(
        "--device",
        action="store_true",
        help="Force device mode (requires ART_D1_SERIAL; probe console later)",
    )
    p.add_argument(
        "--out-dir",
        type=Path,
        default=None,
        help="Write result JSON (default: debug-artifacts/art-verify or ART_UI_VERIFY_OUT_DIR)",
    )
    args = p.parse_args(argv)

    expanded: list[Path] = []
    for path in args.scenarios:
        if path.is_dir():
            expanded.extend(sorted(path.glob("**/*.yaml")))
        else:
            expanded.append(path)

    if not expanded:
        print("no scenarios", file=sys.stderr)
        return 2

    results = run_files(expanded, force_device=args.device, out_dir=args.out_dir)
    failed = 0
    skipped = 0
    for r in results:
        status = r.get("status")
        line = f"{status:5} {r.get('name')}  {r.get('error') or ''}"
        print(line.strip())
        if status == "fail":
            failed += 1
        elif status == "skip":
            skipped += 1
        if r.get("out_file"):
            print(f"      → {r['out_file']}")

    summary = {
        "total": len(results),
        "pass": sum(1 for r in results if r.get("status") == "pass"),
        "fail": failed,
        "skip": skipped,
    }
    print(json.dumps(summary))
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
