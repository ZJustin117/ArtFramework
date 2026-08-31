#!/usr/bin/env python3
"""Compare a Harness screenshot with a reference PNG."""

from __future__ import annotations

import argparse
import json
import math
import sys
import zlib
from pathlib import Path

_HERE = Path(__file__).resolve().parent
if str(_HERE) not in sys.path:
    sys.path.insert(0, str(_HERE))

from png_compare import Crop, compare_pngs, write_diff_png  # noqa: E402


def _non_negative_int(value: str) -> int:
    parsed = int(value)
    if parsed < 0:
        raise argparse.ArgumentTypeError("must be >= 0")
    return parsed


def _ratio(value: str) -> float:
    parsed = float(value)
    if not math.isfinite(parsed) or not 0 <= parsed <= 1:
        raise argparse.ArgumentTypeError("must be between 0 and 1")
    return parsed


def _crop(value: str) -> Crop:
    parts = value.split(",")
    if len(parts) != 4:
        raise argparse.ArgumentTypeError("must be X,Y,W,H")
    try:
        parsed = tuple(int(part) for part in parts)
    except ValueError:
        raise argparse.ArgumentTypeError("must contain four integers")
    if any(part < 0 for part in parsed):
        raise argparse.ArgumentTypeError("must contain non-negative integers")
    return parsed  # type: ignore[return-value]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Compare two 8-bit RGB/RGBA PNG screenshots")
    parser.add_argument("reference", type=Path)
    parser.add_argument("actual", type=Path)
    parser.add_argument("--threshold", type=int, default=0, help="per-channel error allowed per pixel (0-255)")
    parser.add_argument("--max-diff-pixels", type=_non_negative_int, default=0)
    parser.add_argument("--max-diff-ratio", type=_ratio, default=0.0)
    parser.add_argument("--crop", type=_crop, metavar="X,Y,W,H", help="compare only this image region")
    parser.add_argument("--diff", type=Path, metavar="PATH", help="write a red heatmap PNG")
    args = parser.parse_args(argv)
    try:
        result = compare_pngs(args.reference, args.actual, args.threshold, args.crop)
        if args.diff:
            write_diff_png(args.reference, args.actual, args.diff, args.crop)
    except (OSError, ValueError, zlib.error) as exc:
        print("error: " + str(exc), file=sys.stderr)
        return 2
    payload = {"width": result.width, "height": result.height, "same_size": result.same_size,
               "differing_pixels": result.differing_pixels,
               "differing_ratio": result.differing_ratio, "max_error": result.max_error,
               "threshold": args.threshold, "crop": list(args.crop) if args.crop is not None else None}
    print(json.dumps(payload, sort_keys=True))
    return 0 if result.same_size and result.differing_pixels <= args.max_diff_pixels and result.differing_ratio <= args.max_diff_ratio else 1


if __name__ == "__main__":
    raise SystemExit(main())
