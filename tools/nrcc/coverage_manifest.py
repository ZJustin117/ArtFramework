#!/usr/bin/env python3
"""Validate the explicit NRCC coverage manifest against a static scan."""

from __future__ import print_function

import yaml
import re

from families import FAMILY_DEFAULT_POLICY, FAMILY_IDS, family_for


POLICIES = {
    "ART_DELEGATED",
    "NATIVE_PASSTHROUGH",
    "CAPTURED_PASSTHROUGH",
    "NATIVE_WITH_ART_OVERLAY",
    "OUT_OF_SCOPE",
    "UNKNOWN",
}
REQUIRED_FIELDS = (
    "ownerId",
    "nativeClass",
    "nativeMethod",
    "pathKind",
    "surfaceId",
    "effectFamily",
    "hook",
)
ART_DELEGATED_REQUIRED = (
    "justification",
    "test",
)


def effective_policy(entry):
    """Resolve the manifest policy for one entry.

    An explicit policy always wins.  Without one the entry inherits its
    family default (see families.FAMILY_DEFAULT_POLICY); entries without a
    usable family default stay UNKNOWN.  Inherited values are not manual
    annotations, so strict UNKNOWN reporting keeps looking at the explicit
    field only.
    """
    explicit = entry.get("policy")
    if explicit:
        return explicit
    return FAMILY_DEFAULT_POLICY.get(entry.get("family"), "UNKNOWN")


def load_manifest(path):
    with open(path, "r", encoding="utf-8") as handle:
        data = yaml.safe_load(handle.read()) or {}
    if not isinstance(data, dict):
        raise ValueError("manifest root must be a mapping")
    entries = data.get("entries", [])
    if not isinstance(entries, list):
        raise ValueError("manifest entries must be a list")
    return data, entries


def check_manifest(report, manifest_path, strict_unknown=False):
    """Return deterministic errors and counts for a static scan/manifest pair."""
    data, entries = load_manifest(manifest_path)
    errors = []
    seen = {}
    manifest_keys = set()

    for index, entry in enumerate(entries):
        if not isinstance(entry, dict):
            errors.append("entries[{}] must be a mapping".format(index))
            continue
        for field in REQUIRED_FIELDS:
            if field not in entry:
                errors.append("entries[{}] missing {}".format(index, field))
        family = entry.get("family")
        if family is not None and family not in FAMILY_IDS:
            errors.append("entries[{}] unknown family: {}".format(index, family))
        # policy is optional: entries may inherit their family default.
        explicit_policy = entry.get("policy")
        if explicit_policy is not None and explicit_policy not in POLICIES:
            errors.append("entries[{}] invalid policy: {}".format(index, explicit_policy))
        key = (entry.get("nativeClass"), entry.get("nativeMethod"))
        if key in seen:
            errors.append(
                "entries[{}] duplicates native path from entries[{}]: {}#{}".format(
                    index, seen[key], key[0], key[1]
                )
            )
        else:
            seen[key] = index
        manifest_keys.add(key)

    static_paths = report.get("paths", [])
    static_keys = set(
        (path.get("nativeClass"), path.get("nativeMethod"))
        for path in static_paths
    )
    missing = sorted(static_keys - manifest_keys)
    stale = sorted(manifest_keys - static_keys)
    unknown = sorted(
        (entry.get("nativeClass"), entry.get("nativeMethod"))
        for entry in entries
        if entry.get("policy") == "UNKNOWN"
    )

    errors.extend(
        "missing manifest entry: {}#{}".format(native_class, native_method)
        for native_class, native_method in missing
    )
    errors.extend(
        "manifest entry not found in static scan: {}#{}".format(
            native_class, native_method
        )
        for native_class, native_method in stale
    )
    if strict_unknown:
        errors.extend(
            "manifest UNKNOWN policy: {}#{}".format(native_class, native_method)
            for native_class, native_method in unknown
        )

    return {
        "schema": data.get("schema", ""),
        "manifestStatus": data.get("status", ""),
        "staticPathCount": len(static_paths),
        "manifestEntryCount": len(entries),
        "missing": missing,
        "stale": stale,
        "unknown": unknown,
        "errors": errors,
        "ok": not errors,
    }


def check_patch_ownership(report, manifest_path):
    """Return errors for any render/draw patch that suppresses native rendering
    without an ART_DELEGATED manifest entry that includes an ownership
    justification and a test reference.

    `report` is the static scan result produced by `scan_sts_render.py`.
    """
    data, entries = load_manifest(manifest_path)
    errors = []
    manifest_by_key = {
        (entry.get("nativeClass"), entry.get("nativeMethod")): entry
        for entry in entries
        if isinstance(entry, dict)
    }

    for patch in report.get("patches", []):
        if not patch.get("hasSpireReturn"):
            continue
        method = patch.get("targetMethod", "")
        # Only render/draw invocations can be pixel owners.
        if method not in {
            "render", "draw", "renderHand", "renderRelics", "renderPowers",
            "renderTip", "renderIntent",
        }:
            continue
        key = (patch.get("targetClass"), method)
        entry = manifest_by_key.get(key)
        if entry is None:
            errors.append(
                "patch {} -> {}#{} suppresses native draw but has no manifest entry".format(
                    patch.get("source"), key[0], key[1]
                )
            )
            continue
        if effective_policy(entry) != "ART_DELEGATED":
            errors.append(
                "patch {} -> {}#{} suppresses native draw but manifest policy is {}".format(
                    patch.get("source"), key[0], key[1], effective_policy(entry)
                )
            )
        for field in ART_DELEGATED_REQUIRED:
            if not entry.get(field):
                errors.append(
                    "ART_DELEGATED entry {}#{} missing {}".format(
                        key[0], key[1], field
                    )
                )
    return errors


def _owner_id(native_class, native_method):
    value = "{}.{}".format(native_class, native_method).lower()
    value = re.sub(r"[^a-z0-9]+", ".", value).strip(".")
    return "sts1.inventory." + value


def inventory_entries(report, existing_entries=None):
    """Materialize every static path as an explicit development manifest entry.

    Existing entries are preserved verbatim by native path. Newly discovered paths
    are deliberately marked UNKNOWN; generation closes inventory bookkeeping without claiming
    runtime ownership or delegation. Every entry records its semantic family;
    the written policy is the curated override when present, otherwise an
    existing explicit (non-UNKNOWN) annotation, otherwise the family default.
    """
    existing = {}
    for entry in existing_entries or []:
        if isinstance(entry, dict):
            existing[(entry.get("nativeClass"), entry.get("nativeMethod"))] = entry

    def resolve_policy(key, family, existing_entry):
        if key in known_policy:
            return known_policy[key]
        explicit = existing_entry.get("policy") if existing_entry else None
        if explicit not in (None, "UNKNOWN"):
            # A human annotation always beats generated defaults; UNKNOWN is a
            # placeholder, not a decision, so it is re-derived instead.
            return explicit
        return FAMILY_DEFAULT_POLICY.get(family, "UNKNOWN")

    known_policy = {
        ("com.megacrit.cardcrawl.characters.AbstractPlayer", "renderHand"): "ART_DELEGATED",
        ("com.esotericsoftware.spine.SkeletonMeshRenderer", "draw"): "ART_DELEGATED",
        # Not patched: card pixels are drawn by the live, un-intercepted
        # AbstractCard.render call; ART owns hand pose/layout only.
        ("com.megacrit.cardcrawl.cards.AbstractCard", "render"): "OUT_OF_SCOPE",
        ("com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.monsters.AbstractMonster", "renderIntent"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.screens.DungeonMapScreen", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.events.GenericEventDialog", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.screens.select.GridCardSelectScreen", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.screens.select.HandCardSelectScreen", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.screens.CombatRewardScreen", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.rooms.CampfireUI", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.shop.ShopScreen", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.rooms.TreasureRoom", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.ui.buttons.ProceedButton", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.ui.panels.TopPanel", "render"): "ART_DELEGATED",
        ("com.megacrit.cardcrawl.vfx.AbstractGameEffect", "render"): "ART_DELEGATED",
    }
    known_surface_id = {
        ("com.megacrit.cardcrawl.characters.AbstractPlayer", "renderHand"): "sts1.combat.hand",
        ("com.esotericsoftware.spine.SkeletonMeshRenderer", "draw"): "sts1.skeleton",
        ("com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render"): "sts1.combat.controls",
        ("com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render"): "sts1.combat.energy",
        ("com.megacrit.cardcrawl.monsters.AbstractMonster", "renderIntent"): "sts1.combat.intents",
        ("com.megacrit.cardcrawl.screens.DungeonMapScreen", "render"): "sts1.map",
        ("com.megacrit.cardcrawl.events.GenericEventDialog", "render"): "sts1.event",
        ("com.megacrit.cardcrawl.screens.select.GridCardSelectScreen", "render"): "sts1.select.grid",
        ("com.megacrit.cardcrawl.screens.select.HandCardSelectScreen", "render"): "sts1.select.hand",
        ("com.megacrit.cardcrawl.screens.CombatRewardScreen", "render"): "sts1.reward.combat",
        ("com.megacrit.cardcrawl.rooms.CampfireUI", "render"): "sts1.rest",
        ("com.megacrit.cardcrawl.shop.ShopScreen", "render"): "sts1.shop",
        ("com.megacrit.cardcrawl.rooms.TreasureRoom", "render"): "sts1.treasure",
        ("com.megacrit.cardcrawl.ui.buttons.ProceedButton", "render"): "sts1.combat.proceed",
        ("com.megacrit.cardcrawl.ui.panels.TopPanel", "render"): "sts1.top_panel",
        ("com.megacrit.cardcrawl.vfx.AbstractGameEffect", "render"): "",
    }
    known_justification = {
        ("com.megacrit.cardcrawl.characters.AbstractPlayer", "renderHand"): (
            "Combat hand is an ART full-present surface; card pixels are still drawn by live "
            "AbstractCard.render calls, not by hand-built card art."
        ),
        ("com.esotericsoftware.spine.SkeletonMeshRenderer", "draw"): (
            "Only individual skeleton instances claimed by ART through Sts1SkeletonBridge are "
            "suppressed; unclaimed skeletons continue through the native renderer."
        ),
        ("com.megacrit.cardcrawl.vfx.AbstractGameEffect", "render"): (
            "Only individual effect instances claimed by ART through NativeRenderBridge.beginEffectRender "
            "are suppressed; the native effect queue remains authoritative."
        ),
        ("com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render"): (
            "Combat controls is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; current pixel supply is text "
            "chrome pending full reproduction."
        ),
        ("com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render"): (
            "Combat energy is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; current pixel supply is text "
            "chrome pending full reproduction."
        ),
        ("com.megacrit.cardcrawl.monsters.AbstractMonster", "renderIntent"): (
            "Combat intents is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; current pixel supply is "
            "projection chrome pending full reproduction."
        ),
        ("com.megacrit.cardcrawl.screens.DungeonMapScreen", "render"): (
            "Map is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART only when "
            "FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; HostAssets supplies the node "
            "draw path while full native parity is pending."
        ),
        ("com.megacrit.cardcrawl.events.GenericEventDialog", "render"): (
            "Event dialog is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; base pixels are not reproduced "
            "yet and stay visible there."
        ),
        ("com.megacrit.cardcrawl.screens.select.GridCardSelectScreen", "render"): (
            "Grid select is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; base pixels are not reproduced "
            "yet and stay visible there."
        ),
        ("com.megacrit.cardcrawl.screens.select.HandCardSelectScreen", "render"): (
            "Hand select is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; base pixels are not reproduced "
            "yet, and the referenced test gates the shared plan-level policy until a dedicated prefix "
            "suppression test exists."
        ),
        ("com.megacrit.cardcrawl.screens.CombatRewardScreen", "render"): (
            "Combat rewards is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; reward item sync and base "
            "pixels are incomplete."
        ),
        ("com.megacrit.cardcrawl.rooms.CampfireUI", "render"): (
            "Rest room is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART only "
            "when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; campfire options are not synced "
            "and base pixels are absent."
        ),
        ("com.megacrit.cardcrawl.shop.ShopScreen", "render"): (
            "Shop is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART only when "
            "FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; item sync and base pixels are "
            "incomplete."
        ),
        ("com.megacrit.cardcrawl.rooms.TreasureRoom", "render"): (
            "Treasure room is an ART full-present surface; NativeRenderBridge returns DELEGATE_TO_ART "
            "only when FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; chest and item sync are absent."
        ),
        ("com.megacrit.cardcrawl.ui.buttons.ProceedButton", "render"): (
            "Proceed button is an ART combat-controls extension surface; NativeRenderBridge returns "
            "DELEGATE_TO_ART only when FULL_READY and panic or unknown owners fail open. Delegation "
            "must close PresentationDrawEvidence or count as a strict report gap; current pixel supply "
            "is text chrome."
        ),
        ("com.megacrit.cardcrawl.ui.panels.TopPanel", "render"): (
            "Top panel is an ART HUD surface; NativeRenderBridge returns DELEGATE_TO_ART only when "
            "FULL_READY and panic or unknown owners fail open. Delegation must close "
            "PresentationDrawEvidence or count as a strict report gap; current pixel supply is text "
            "chrome."
        ),
    }
    known_test = {
        ("com.megacrit.cardcrawl.characters.AbstractPlayer", "renderHand"): (
            "artframework.sts1.render.NativeRenderBridgeTest.fullReadyDelegatesWithoutNativeContinuation"
        ),
        ("com.esotericsoftware.spine.SkeletonMeshRenderer", "draw"): (
            "artframework.sts1.skeleton.Sts1SkeletonBridgeTest.claimedSkeletonDelegatesWithoutNativeContinuation"
        ),
        ("com.megacrit.cardcrawl.vfx.AbstractGameEffect", "render"): (
            "artframework.sts1.render.NativeRenderBridgeTest.transientEffectRenderAlwaysCapturesAndPasses"
        ),
        ("com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render"): (
            "artframework.sts1.render.CombatControlsRenderPatchesTest.fullReadySuppressesNativeEndTurnRender"
        ),
        ("com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render"): (
            "artframework.sts1.render.CombatEnergyRenderPatchesTest.fullReadySuppressesNativeEnergyRender"
        ),
        ("com.megacrit.cardcrawl.monsters.AbstractMonster", "renderIntent"): (
            "artframework.sts1.render.CombatIntentRenderPatchesTest.fullReadySuppressesNativeIntentRender"
        ),
        ("com.megacrit.cardcrawl.screens.DungeonMapScreen", "render"): (
            "artframework.sts1.render.MapDrawPathTest.suppressesNativeMapOnlyWhenFullReady"
        ),
        ("com.megacrit.cardcrawl.events.GenericEventDialog", "render"): (
            "artframework.sts1.render.EventRenderPatchesTest.fullReadySuppressesNativeEventRender"
        ),
        ("com.megacrit.cardcrawl.screens.select.GridCardSelectScreen", "render"): (
            "artframework.sts1.render.SelectRenderPatchesTest.fullReadySuppressesNativeGridSelectRender"
        ),
        ("com.megacrit.cardcrawl.screens.select.HandCardSelectScreen", "render"): (
            "artframework.sts1.render.RenderPatchOwnershipTest.delegatedSurfacesAreNotNativeAuthoritative"
        ),
        ("com.megacrit.cardcrawl.screens.CombatRewardScreen", "render"): (
            "artframework.sts1.render.RoomRenderPatchesTest.fullReadySuppressesNativeRewardRender"
        ),
        ("com.megacrit.cardcrawl.rooms.CampfireUI", "render"): (
            "artframework.sts1.render.RoomRenderPatchesTest.fullReadySuppressesNativeRestRender"
        ),
        ("com.megacrit.cardcrawl.shop.ShopScreen", "render"): (
            "artframework.sts1.render.RoomRenderPatchesTest.fullReadySuppressesNativeShopRender"
        ),
        ("com.megacrit.cardcrawl.rooms.TreasureRoom", "render"): (
            "artframework.sts1.render.RoomRenderPatchesTest.fullReadySuppressesNativeTreasureRender"
        ),
        ("com.megacrit.cardcrawl.ui.buttons.ProceedButton", "render"): (
            "artframework.sts1.render.ProceedButtonRenderPatchesTest.fullReadySuppressesNativeProceedButtonRender"
        ),
        ("com.megacrit.cardcrawl.ui.panels.TopPanel", "render"): (
            "artframework.sts1.render.TopPanelRenderPatchesTest.fullReadySuppressesNativeTopPanelRender"
        ),
    }
    result = []
    for path in sorted(
        report.get("paths", []),
        key=lambda item: (item.get("nativeClass", ""), item.get("nativeMethod", "")),
    ):
        key = (path.get("nativeClass"), path.get("nativeMethod"))
        existing_entry = existing.get(key)
        family = family_for(*key)
        policy = resolve_policy(key, family, existing_entry)
        if existing_entry is not None and existing_entry.get("policy") not in (None, "UNKNOWN"):
            # Preserve the existing entry but overlay any known ownership metadata.
            entry = dict(existing_entry)
            entry["family"] = family
            entry["policy"] = policy
            if policy == "ART_DELEGATED":
                entry["justification"] = known_justification.get(key, "")
                entry["test"] = known_test.get(key, "")
            if key in known_surface_id:
                entry["surfaceId"] = known_surface_id[key]
            result.append(entry)
            continue
        patches = path.get("artPatches") or []
        hook = patches[0].get("source", "") if patches else ""
        entry = {
            "ownerId": _owner_id(*key),
            "nativeClass": key[0],
            "nativeMethod": key[1],
            "family": family,
            "pathKind": path.get("kind", "unknown"),
            "surfaceId": known_surface_id.get(key, ""),
            "effectFamily": "abstract_game_effect" if path.get("kind") == "transient-effect" else "none",
            "hook": hook,
            "policy": policy,
        }
        if policy == "ART_DELEGATED":
            entry["justification"] = known_justification.get(key, "")
            entry["test"] = known_test.get(key, "")
        result.append(entry)
    current_keys = set(
        (path.get("nativeClass"), path.get("nativeMethod"))
        for path in report.get("paths", [])
    )
    for key, entry in sorted(existing.items()):
        if key not in current_keys:
            result.append(entry)
    return result


def write_inventory_manifest(report, path, existing_path=None):
    existing_data = {}
    existing_entries = []
    if existing_path:
        existing_data, existing_entries = load_manifest(existing_path)
    data = {
        "schema": existing_data.get("schema", "nrcc.coverage-manifest.v1"),
        "status": "inventory",
        "notes": [
            "Generated from the current static scan; UNKNOWN entries require explicit policy review.",
            "Generation does not prove patch loading or runtime delegation.",
            "Entries may omit policy to inherit their family default; see tools/nrcc/families.py.",
        ],
        "entries": inventory_entries(report, existing_entries),
    }
    with open(path, "w", encoding="utf-8") as handle:
        yaml.safe_dump(data, handle, sort_keys=False, allow_unicode=False)
    return data
