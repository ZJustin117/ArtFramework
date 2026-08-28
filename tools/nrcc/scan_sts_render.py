#!/usr/bin/env python3
"""Build a static native-render inventory for STS1 and ART SpirePatches.

This is an inventory tool, not a proof that a path was executed.  Unknown
paths are intentionally retained in the output so a later coverage manifest
can classify them as replaced, allowed, or out of scope.
"""

from __future__ import print_function

import argparse
import json
import os
import re
import subprocess
import sys
import zipfile

from coverage_manifest import check_manifest, check_patch_ownership, load_manifest, write_inventory_manifest
from families import family_for


STS_PREFIX = "com.megacrit.cardcrawl."
STATIC_SCAN_SCHEMA = "nrcc.static-scan.v4"
CLASS_HINTS = re.compile(
    r"(render|effect|relic|power|screen|room|creature|player|energy|card|dialog|panel|button|map|menu|tip)",
    re.IGNORECASE,
)
EXPLICIT_RENDER_CLASSES = (
    STS_PREFIX + "powers.AbstractPower",
    STS_PREFIX + "characters.AbstractPlayer",
    STS_PREFIX + "ui.buttons.EndTurnButton",
    STS_PREFIX + "core.OverlayMenu",
    STS_PREFIX + "helpers.TipHelper",
    STS_PREFIX + "ui.panels.PotionPopUp",
)
RENDER_METHOD_NAMES = (
    "draw",
    "render",
    "renderAmount",
    "renderBlackScreen",
    "renderBlights",
    "renderGenericTip",
    "renderGlowEffect",
    "renderHand",
    "renderHoldEndTurn",
    "renderHoverReticle",
    "renderIcons",
    "renderIntent",
    "renderOrb",
    "renderPowerTips",
    "renderPowers",
    "renderRelics",
    "renderStatScreen",
    "renderTargetingUi",
    "renderTip",
    "renderTipForCard",
)
RENDER_METHOD_NAMES_BY_LENGTH = sorted(RENDER_METHOD_NAMES, key=len, reverse=True)
RENDER_METHOD_PATTERN = "|".join(re.escape(name) for name in RENDER_METHOD_NAMES_BY_LENGTH)
RENDER_METHOD = re.compile(
    r"\b(public|protected|private)?\s*(static\s+)?[^ ]+\s+(" + RENDER_METHOD_PATTERN + r")\s*\("
)
JAVAP_METHOD_DECLARATION = re.compile(r"^\s*(?:public|protected|private)\s+(?:static\s+)?(?:abstract\s+)?(?:final\s+)?[^=;]+\s+(%s)\s*\(" % RENDER_METHOD_PATTERN)
PATCH_TARGET = re.compile(
    r"@SpirePatch\s*\(.*?clz\s*=\s*([A-Za-z0-9_.$]+).*?method\s*=\s*\"([^\"]+)\"",
    re.DOTALL,
)
PATCH_TARGET_SHORT = re.compile(
    r"@SpirePatch\s*\(\s*clz\s*=\s*([A-Za-z0-9_.$]+).*?method\s*=\s*\"([^\"]+)\"",
    re.DOTALL,
)


def class_name(entry):
    return entry[:-6].replace("/", ".")


def load_art_patches(source_root):
    patches = []
    for root, _, names in os.walk(source_root):
        for name in names:
            if not name.endswith(".java"):
                continue
            path = os.path.join(root, name)
            try:
                text = open(path, "r", encoding="utf-8").read()
            except (IOError, UnicodeError):
                continue
            imports = parse_imports(text)
            for match in PATCH_TARGET.finditer(text):
                target_class = resolve_target_class(match.group(1), imports)
                patches.append({
                    "source": os.path.relpath(path, source_root),
                    "targetClass": target_class,
                    "targetMethod": match.group(2),
                    "hasSpireReturn": "SpireReturn.Return" in text,
                    "continuationHint": continuation_hint(text),
                })
            # The short regex is useful when annotation formatting prevents
            # the broad expression from matching, but do not duplicate rows.
            if not PATCH_TARGET.search(text):
                for match in PATCH_TARGET_SHORT.finditer(text):
                    target_class = resolve_target_class(match.group(1), imports)
                    patches.append({
                        "source": os.path.relpath(path, source_root),
                        "targetClass": target_class,
                        "targetMethod": match.group(2),
                        "hasSpireReturn": "SpireReturn.Return" in text,
                        "continuationHint": continuation_hint(text),
                    })
    return patches


def parse_imports(text):
    imports = {}
    for match in re.finditer(r"^\s*import\s+(?:static\s+)?([A-Za-z0-9_.$]+);", text, re.MULTILINE):
        qualified = match.group(1)
        imports[qualified.rsplit(".", 1)[-1]] = qualified
    return imports


def resolve_target_class(raw, imports):
    value = raw[:-6] if raw.endswith(".class") else raw
    if "." in value:
        return value
    return imports.get(value, value)


def continuation_hint(text):
    hints = []
    for line in text.splitlines():
        if "shouldSuppress" in line or "suppress" in line.lower():
            value = line.strip()
            if value not in hints:
                hints.append(value)
    return hints[:8]


def _normalize_descriptor(descriptor):
    return descriptor.strip() if descriptor else ""


def _method_descriptor(name, descriptor):
    descriptor = _normalize_descriptor(descriptor)
    return "{}:{}".format(name, descriptor) if descriptor else name


def javap_methods(jar, name):
    try:
        result = subprocess.run(
            ["javap", "-classpath", jar, "-p", name],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
            check=False,
        )
    except OSError as exc:
        raise RuntimeError("javap is required: {}".format(exc))
    if result.returncode != 0:
        return []
    return [method["name"] for method in parse_javap_render_methods(result.stdout)]


def parse_javap_render_methods(text):
    """Parse javap -p -s output into render method rows with descriptors."""
    methods = []
    pending = None
    for line in text.splitlines():
        match = JAVAP_METHOD_DECLARATION.match(line)
        if match:
            pending = match.group(1)
            continue
        descriptor = re.match(r"^\s*descriptor:\s*(\S+)\s*$", line)
        if descriptor and pending:
            methods.append({
                "name": pending,
                "descriptor": _normalize_descriptor(descriptor.group(1)),
                "methodDescriptor": _method_descriptor(pending, descriptor.group(1)),
            })
            pending = None
            continue
        if line.strip() and not line.startswith(" "):
            pending = None
    return sorted(methods, key=lambda item: (item["name"], item["descriptor"]))


def javap_candidate_methods(jar, names):
    """Inspect candidates in one javap process instead of spawning per class."""
    if not names:
        return {}
    try:
        result = subprocess.run(
            ["javap", "-classpath", jar, "-p", "-s"] + names,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
            check=False,
        )
    except OSError as exc:
        raise RuntimeError("javap is required: {}".format(exc))
    wanted = set(names)
    methods = dict((name, []) for name in names)
    current = None
    pending = None
    for line in result.stdout.splitlines():
        header = re.match(r"^(public |protected |private )?(abstract )?(class|interface|enum) ([A-Za-z0-9_.$]+)", line)
        if header:
            current = header.group(4)
            pending = None
            if current not in wanted:
                current = None
            continue
        if current is None:
            continue
        match = JAVAP_METHOD_DECLARATION.match(line)
        if match:
            pending = match.group(1)
            continue
        descriptor = re.match(r"^\s*descriptor:\s*(\S+)\s*$", line)
        if descriptor and pending:
            methods[current].append({
                "name": pending,
                "descriptor": _normalize_descriptor(descriptor.group(1)),
                "methodDescriptor": _method_descriptor(pending, descriptor.group(1)),
            })
            pending = None
    return dict(
        (name, sorted(values, key=lambda item: (item["name"], item["descriptor"])))
        for name, values in methods.items()
    )


def patch_index(patches):
    result = {}
    for patch in patches:
        key = (patch["targetClass"], patch["targetMethod"])
        result.setdefault(key, []).append(patch)
    return result


def candidate_classes(entries, extra_names=None):
    result = []
    for entry in entries:
        if not entry.endswith(".class") or "$" in entry:
            continue
        name = class_name(entry)
        if not name.startswith(STS_PREFIX) or not CLASS_HINTS.search(name):
            continue
        result.append(name)
    for name in tuple(extra_names or ()) + EXPLICIT_RENDER_CLASSES:
        if name not in result and "$" not in name:
            result.append(name)
    return result


def scan(args):
    if not os.path.isfile(args.sts_jar):
        raise SystemExit("STS jar does not exist: {}".format(args.sts_jar))
    if not os.path.isdir(args.source_root):
        raise SystemExit("ART source root does not exist: {}".format(args.source_root))

    with zipfile.ZipFile(args.sts_jar) as jar:
        entries = jar.namelist()
    patches = [p for p in load_art_patches(args.source_root) if is_render_patch(p)]
    indexed = patch_index(patches)
    candidates = candidate_classes(entries, [p["targetClass"] for p in patches])
    discovered_methods = javap_candidate_methods(args.sts_jar, candidates)
    paths = []
    for name in candidates:
        for method_info in discovered_methods.get(name, []):
            method = method_info["name"]
            descriptor = method_info.get("descriptor", "")
            rows = indexed.get((name, method), [])
            paths.append({
                "nativeClass": name,
                "nativeMethod": method,
                "nativeDescriptor": descriptor,
                "nativeMethodDescriptor": method_info.get("methodDescriptor", _method_descriptor(method, descriptor)),
                "family": family_for(name, method),
                "kind": classify(name, method),
                "artPatches": rows,
                "classification": "hooked" if rows else "unclassified",
                "captureRequired": True,
            })

    report = {
        "schema": STATIC_SCAN_SCHEMA,
        "stsJar": os.path.abspath(args.sts_jar),
        "sourceRoot": os.path.abspath(args.source_root),
        "candidateClassCount": len(candidates),
        "patchCount": len(patches),
        "patches": patches,
        "paths": paths,
        "summary": {
            "renderPaths": len(paths),
            "hooked": sum(1 for p in paths if p["classification"] == "hooked"),
            "unclassified": sum(1 for p in paths if p["classification"] == "unclassified"),
            "families": family_summary(paths),
        },
        "limitations": [
            "Method declarations are static candidates; runtime execution still needs a dynamic ledger.",
            "Obfuscated, generated, reflective, and third-party draw paths require explicit additions.",
            "Rows include nativeDescriptor/nativeMethodDescriptor when javap -s reports them; patch annotations are still parsed at method-name granularity.",
            "The scan does not prove that a SpirePatch was loaded by ModTheSpire.",
        ],
    }
    if args.check_manifest:
        manifest_check = check_manifest(
            report, args.check_manifest, strict_unknown=args.strict_manifest
        )
        ownership_errors = check_patch_ownership(report, args.check_manifest)
        manifest_check["ownershipErrors"] = ownership_errors
        if ownership_errors:
            manifest_check["ok"] = False
            manifest_check["errors"].extend(ownership_errors)
        report["manifestCheck"] = manifest_check
    if args.write_manifest:
        existing_path = args.check_manifest if args.check_manifest else None
        write_inventory_manifest(report, args.write_manifest, existing_path)
    if args.output:
        parent = os.path.dirname(os.path.abspath(args.output))
        if parent and not os.path.isdir(parent):
            os.makedirs(parent)
        with open(args.output, "w", encoding="utf-8") as handle:
            json.dump(report, handle, indent=2, sort_keys=True)
            handle.write("\n")
    return report


def is_render_patch(patch):
    method = patch["targetMethod"].lower()
    return method in set(name.lower() for name in RENDER_METHOD_NAMES) or "render" in patch["source"].lower()


def classify(name, method):
    lower = name.lower()
    if "effect" in lower:
        return "transient-effect"
    if "relic" in lower or "power" in lower:
        return "relic-power"
    if "screen" in lower or "dialog" in lower or "room" in lower:
        return "native-surface"
    if method == "draw":
        return "draw-owner"
    return "render-owner"


def family_summary(paths):
    """Count static paths per semantic family, in stable family-id order."""
    counts = {}
    for path in paths:
        family = path.get("family", "")
        counts[family] = counts.get(family, 0) + 1
    return dict(sorted(counts.items()))


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sts-jar", required=True)
    parser.add_argument("--source-root", default="src/main/java")
    parser.add_argument("--output", default="debug-artifacts/nrcc/sts-static-scan.json")
    parser.add_argument("--check-manifest", help="Validate the static paths against a YAML manifest")
    parser.add_argument(
        "--strict-manifest",
        action="store_true",
        help="Also reject explicit UNKNOWN manifest policies",
    )
    parser.add_argument(
        "--write-manifest",
        help="Write an explicit manifest entry for every static candidate; new entries use UNKNOWN",
    )
    args = parser.parse_args(argv)
    report = scan(args)
    print(json.dumps(report["summary"], sort_keys=True))
    if args.check_manifest:
        print(json.dumps(report["manifestCheck"], sort_keys=True))
        if not report["manifestCheck"]["ok"]:
            return 2
    print("wrote {}".format(args.output) if args.output else "report not written")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
