#!/usr/bin/env python3
"""Verify the optional smoke classpath uses the STS1 libGDX ABI."""

import subprocess
import sys
import zipfile
import os


def fail(message):
    print("FAIL: " + message, file=sys.stderr)
    return 1


def main():
    if len(sys.argv) != 4:
        return fail("usage: verify_host_abi.py HOST_JAR RUNTIME_JAR SMOKE_CLASSPATH")
    host_jar, runtime_jar, smoke_classpath = sys.argv[1:]
    classpath_entries = smoke_classpath.split(os.pathsep)
    if host_jar not in classpath_entries:
        return fail("smoke classpath does not contain the requested host jar")
    unexpected_gdx = [entry for entry in classpath_entries
                      if entry not in (host_jar, runtime_jar)
                      and "gdx" in os.path.basename(entry).lower()]
    if unexpected_gdx:
        return fail("smoke classpath contains an unexpected libGDX jar: " + unexpected_gdx[0])
    try:
        output = subprocess.check_output(
            ["javap", "-classpath", smoke_classpath, "-p", "-s",
             "com.badlogic.gdx.graphics.g2d.Gdx2DPixmap"],
            stderr=subprocess.STDOUT, universal_newlines=True)
    except (OSError, subprocess.CalledProcessError) as exc:
        return fail("cannot inspect host Gdx2DPixmap: %s" % exc)

    if "public static native void setBlend(int);" not in output:
        return fail("host Gdx2DPixmap does not expose static native setBlend(int)")
    if "descriptor: (I)V" not in output:
        return fail("host Gdx2DPixmap setBlend descriptor is not (I)V")
    if "public static synchronized void load();" not in subprocess.check_output(
            ["javap", "-classpath", smoke_classpath, "-p", "com.badlogic.gdx.utils.GdxNativesLoader"],
            stderr=subprocess.STDOUT, universal_newlines=True):
        return fail("smoke classpath does not expose host GdxNativesLoader.load()")

    try:
        with zipfile.ZipFile(runtime_jar) as runtime:
            if any(name.startswith("com/badlogic/gdx/") for name in runtime.namelist()):
                return fail("runtime jar contains libGDX classes")
    except (OSError, zipfile.BadZipFile) as exc:
        return fail("cannot inspect runtime jar: %s" % exc)

    print("PASS: smoke classpath resolves host Gdx2DPixmap/GdxNativesLoader; runtime has no libGDX classes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
