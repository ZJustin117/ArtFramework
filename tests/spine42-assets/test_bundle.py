#!/usr/bin/env python3
"""Validate a local STS2 developer asset bundle without loading game/runtime classes."""

import os
import sys
import zipfile


REQUIRED_METADATA = {
    "format": "artframework-sts2-assets",
    "formatVersion": "1",
    "sourceRoot": "animations",
    "spineData": "4.2",
}
ALLOWED_SUFFIXES = (".skel", ".atlas", ".png", ".tres")


def fail(message):
    print("FAIL: " + message, file=sys.stderr)
    return 1


def main():
    path = os.environ.get("ART_STS2_ASSET_JAR")
    if not path:
        return fail("ART_STS2_ASSET_JAR is unset; run package-sts2-assets.sh first")
    if not os.path.isfile(path):
        return fail("asset bundle does not exist: " + path)

    try:
        with zipfile.ZipFile(path) as bundle:
            names = bundle.namelist()
            metadata_name = "META-INF/artframework-sts2-assets.properties"
            if metadata_name not in names:
                return fail("missing " + metadata_name)

            metadata = {}
            for raw in bundle.read(metadata_name).decode("utf-8").splitlines():
                if not raw or raw.startswith("#") or "=" not in raw:
                    continue
                key, value = raw.split("=", 1)
                metadata[key] = value
            for key, value in REQUIRED_METADATA.items():
                if metadata.get(key) != value:
                    return fail("metadata %s=%r, expected %r" % (key, metadata.get(key), value))

            assets = [name for name in names if name.startswith("animations/") and not name.endswith("/")]
            if not assets:
                return fail("bundle contains no animations")
            for name in assets:
                if not name.endswith(ALLOWED_SUFFIXES):
                    return fail("unexpected asset entry: " + name)
                if os.path.isabs(name) or ".." in name.split("/"):
                    return fail("unsafe asset path: " + name)

            counts = {suffix: sum(name.endswith(suffix) for name in assets) for suffix in ALLOWED_SUFFIXES}
            print("PASS: %s (%d assets, %s)" % (path, len(assets), counts))
            return 0
    except (OSError, zipfile.BadZipFile, UnicodeError) as exc:
        return fail(str(exc))


if __name__ == "__main__":
    sys.exit(main())
