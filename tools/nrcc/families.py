#!/usr/bin/env python3
"""Semantic families for NRCC static native-render paths.

A family groups native render paths that belong to the same STS1 UI area so
the coverage manifest can inherit a default policy per group instead of
annotating hundreds of entries by hand.  Rules are evaluated first-match-wins
in declaration order; anything unmatched must fail loudly so new scan output
forces an explicit family decision instead of silently landing in a group.
"""

import re


UNKNOWN_POLICY = "UNKNOWN"

# Family -> default manifest policy.  Every scanned path resolves to a
# determinate policy through this table; only `overlay-targeting` deliberately
# stays UNKNOWN until its slice lands, so new paths there keep failing the
# strict manifest gate loudly instead of silently counting as covered.
FAMILY_DEFAULT_POLICY = {
    # Transient effects: native pixel authority is retained and ART only
    # observes through the shared container/base-class observation entries.
    "vfx-combat": "OBSERVED",
    "vfx-scene-world": "OBSERVED",
    "vfx-campfire-rest": "OBSERVED",
    "vfx-card-manipulation": "OBSERVED",
    "vfx-stance-aura": "OBSERVED",
    "vfx-misc-root": "OBSERVED",
    # Semantic fallback note only: the single member carries an explicit
    # ART_DELEGATED per-instance claim policy.
    "skeleton-runtime": "OBSERVED",
    # Structural drawing helpers that ART never intercepts.
    "draw-primitives-tips": "NATIVE_PASSTHROUGH",
    "word-tip-ui": "NATIVE_PASSTHROUGH",
    "core-game-root": "NATIVE_PASSTHROUGH",
    # Native authority retained; roadmap candidates until individually triaged
    # for delegation or a full-present surface.
    "monsters-bosses": "NATIVE_WITH_ART_OVERLAY",
    "player-character": "NATIVE_WITH_ART_OVERLAY",
    "orbs": "NATIVE_WITH_ART_OVERLAY",
    "relics-blights-potions": "NATIVE_WITH_ART_OVERLAY",
    "stances-state": "NATIVE_WITH_ART_OVERLAY",
    "map-graph": "NATIVE_WITH_ART_OVERLAY",
    "cards-piles-soul": "NATIVE_WITH_ART_OVERLAY",
    "room-shells": "NATIVE_WITH_ART_OVERLAY",
    "event-dialogs": "NATIVE_WITH_ART_OVERLAY",
    "shop-rewards-chests": "NATIVE_WITH_ART_OVERLAY",
    "hud-top-panel": "NATIVE_WITH_ART_OVERLAY",
    "buttons-controls": "NATIVE_WITH_ART_OVERLAY",
    "inrun-fullscreens": "NATIVE_WITH_ART_OVERLAY",
    # Deliberate boundary: out-of-run meta screens stay native and unhooked.
    "meta-outofrun-screens": "OUT_OF_SCOPE",
    # Slice D: observe-only targeting family (AbstractPlayer/PotionPopUp renderTargetingUi).
    "overlay-targeting": "OBSERVED",
}

# Shared rationale texts for family defaults.  Policies that keep native
# pixels authoritative (OBSERVED / NATIVE_PASSTHROUGH) must document why;
# entries without their own justification inherit these.  OBSERVED texts cite
# the observation entry (patch file) so the validator can enforce it.
_VFX_OBSERVED_JUSTIFICATION = (
    "Native pixel authority retained; ART only observes transient effect renders "
    "through the shared observation entries (the AbstractGameEffect.render hook in "
    "artframework/sts1/patch/TransientEffectRenderPatches.java and the "
    "AbstractDungeon.render container instrument in "
    "artframework/sts1/patch/TransientEffectContainerPatches.java); concrete "
    "effects are covered by virtual dispatch at those sites, never by per-class hooks."
)
_ROADMAP_OVERLAY_JUSTIFICATION = (
    "Future delegation candidate; native render authority remains with STS until "
    "the path is triaged for delegation or a full-present surface. ART may add "
    "non-authoritative overlays only."
)

FAMILY_DEFAULT_JUSTIFICATION = {
    "vfx-combat": _VFX_OBSERVED_JUSTIFICATION,
    "vfx-scene-world": _VFX_OBSERVED_JUSTIFICATION,
    "vfx-campfire-rest": _VFX_OBSERVED_JUSTIFICATION,
    "vfx-card-manipulation": _VFX_OBSERVED_JUSTIFICATION,
    "vfx-stance-aura": _VFX_OBSERVED_JUSTIFICATION,
    "vfx-misc-root": (
        "Native pixel authority retained; this group contains the hooked "
        "AbstractGameEffect base class itself plus misc effects, all observed "
        "through artframework/sts1/patch/TransientEffectRenderPatches.java and the "
        "artframework/sts1/patch/TransientEffectContainerPatches.java container "
        "instrument; the native effect queue stays authoritative."
    ),
    "skeleton-runtime": (
        "Semantic note only: unclaimed skeletons always continue through the native "
        "renderer (artframework/sts1/patch/SkeletonRenderPatches.java is the single "
        "observation/suppression entry); the one member carries an explicit "
        "ART_DELEGATED per-instance claim policy."
    ),
    "draw-primitives-tips": (
        "Structural drawing helpers (Hitbox, TipHelper, DrawMaster, Label, Sprite, "
        "AbstractDrawable) invoked inside owner draws; ART never intercepts them."
    ),
    "word-tip-ui": (
        "DialogWord/SpeechWord text layout helpers are structural drawing aids that "
        "ART never intercepts; FtueTip/MultiPageFtue belong to the out-of-run Ftue "
        "meta interface, recorded here as a note only."
    ),
    "core-game-root": (
        "Structural roots (CardCrawlGame, OverlayMenu, GameCursor, AbstractCreature) "
        "orchestrate frame rendering and are never intercepted per pixel; member "
        "exceptions (TestGame, AbstractDungeon.render) carry explicit entry policies."
    ),
    "monsters-bosses": _ROADMAP_OVERLAY_JUSTIFICATION,
    "player-character": _ROADMAP_OVERLAY_JUSTIFICATION,
    "orbs": _ROADMAP_OVERLAY_JUSTIFICATION,
    "relics-blights-potions": _ROADMAP_OVERLAY_JUSTIFICATION,
    "stances-state": _ROADMAP_OVERLAY_JUSTIFICATION,
    "map-graph": _ROADMAP_OVERLAY_JUSTIFICATION,
    "cards-piles-soul": _ROADMAP_OVERLAY_JUSTIFICATION,
    "room-shells": _ROADMAP_OVERLAY_JUSTIFICATION,
    "event-dialogs": _ROADMAP_OVERLAY_JUSTIFICATION,
    "shop-rewards-chests": _ROADMAP_OVERLAY_JUSTIFICATION,
    "hud-top-panel": _ROADMAP_OVERLAY_JUSTIFICATION,
    "buttons-controls": _ROADMAP_OVERLAY_JUSTIFICATION,
    "inrun-fullscreens": _ROADMAP_OVERLAY_JUSTIFICATION,
    "meta-outofrun-screens": (
        "Out-of-run meta screens are C1 synthetic-window territory; the native "
        "screens stay authoritative and unhooked."
    ),
    "overlay-targeting": (
        "Native pixel authority retained; ART only observes card/potion targeting renders "
        "through CombatTargetingRenderPatches.java and projects the session into the "
        "c2-projection metadata entity; self-draw is optional at sts1.combat.targeting FULL."
    ),
}

_PREFIX = "com.megacrit.cardcrawl."

_META_SCREEN_SUBPACKAGES = (
    _PREFIX + "screens.charSelect.",
    _PREFIX + "screens.compendium.",
    _PREFIX + "screens.custom.",
    _PREFIX + "screens.leaderboards.",
    _PREFIX + "screens.mainMenu.",
    _PREFIX + "screens.options.",
    _PREFIX + "screens.runHistory.",
    _PREFIX + "screens.splash.",
    _PREFIX + "screens.stats.",
)

_WORD_TIP_UI = (
    _PREFIX + "ui.DialogWord",
    _PREFIX + "ui.FtueTip",
    _PREFIX + "ui.MultiPageFtue",
    _PREFIX + "ui.SpeechWord",
)

_DRAW_PRIMITIVES = (
    _PREFIX + "helpers.AbstractDrawable",
    _PREFIX + "helpers.DrawMaster",
    _PREFIX + "helpers.Hitbox",
    _PREFIX + "helpers.Label",
    _PREFIX + "helpers.Sprite",
    _PREFIX + "helpers.TipHelper",
)

_META_OUT_OF_RUN_PACKAGES = (
    _PREFIX + "credits.",
    _PREFIX + "cutscenes.",
    _PREFIX + "daily.",
    _PREFIX + "neow.",
    _PREFIX + "scenes.",
    _PREFIX + "unlock.",
)

# Ordered rules: (matcher, family id).  First match wins.  A matcher is a
# mapping of constraints ("method" regex on the native method name, "exact"
# FQN set, "prefix" FQN tuple); every constraint present must hold.
_RULES = (
    ({"method": re.compile(r"^renderTargetingUi$")}, "overlay-targeting"),
    (
        {"exact": ("com.esotericsoftware.spine.SkeletonMeshRenderer",)},
        "skeleton-runtime",
    ),
    ({"prefix": (_PREFIX + "vfx.combat.",)}, "vfx-combat"),
    ({"prefix": (_PREFIX + "vfx.scene.",)}, "vfx-scene-world"),
    ({"prefix": (_PREFIX + "vfx.campfire.",)}, "vfx-campfire-rest"),
    ({"prefix": (_PREFIX + "vfx.cardManip.",)}, "vfx-card-manipulation"),
    ({"prefix": (_PREFIX + "vfx.stance.",)}, "vfx-stance-aura"),
    # vfx root package plus deprecated/shader subpackages: misc effects,
    # AbstractGameEffect base class, speech bubbles, etc.
    ({"prefix": (_PREFIX + "vfx.",)}, "vfx-misc-root"),
    ({"prefix": (_PREFIX + "cards.",)}, "cards-piles-soul"),
    ({"prefix": (_PREFIX + "characters.",)}, "player-character"),
    ({"prefix": (_PREFIX + "core.", _PREFIX + "dungeons.")}, "core-game-root"),
    ({"prefix": (_PREFIX + "monsters.",)}, "monsters-bosses"),
    ({"prefix": (_PREFIX + "orbs.",)}, "orbs"),
    (
        {"prefix": (_PREFIX + "blights.", _PREFIX + "potions.", _PREFIX + "relics.")},
        "relics-blights-potions",
    ),
    ({"prefix": (_PREFIX + "stances.",)}, "stances-state"),
    ({"prefix": (_PREFIX + "map.",)}, "map-graph"),
    # Neow ships one AbstractRoom and one AbstractEvent outside their usual
    # packages; group them structurally, the remaining Neow screens are meta.
    ({"exact": (_PREFIX + "neow.NeowRoom",)}, "room-shells"),
    ({"exact": (_PREFIX + "neow.NeowEvent",)}, "event-dialogs"),
    ({"prefix": (_PREFIX + "rooms.",)}, "room-shells"),
    ({"prefix": (_PREFIX + "events.",)}, "event-dialogs"),
    ({"prefix": (_PREFIX + "rewards.", _PREFIX + "shop.")}, "shop-rewards-chests"),
    ({"exact": _WORD_TIP_UI}, "word-tip-ui"),
    ({"prefix": (_PREFIX + "ui.buttons.",)}, "buttons-controls"),
    (
        {"exact": (_PREFIX + "ui.campfire.AbstractCampfireOption",)},
        "buttons-controls",
    ),
    ({"prefix": (_PREFIX + "ui.panels.",)}, "hud-top-panel"),
    ({"prefix": _META_SCREEN_SUBPACKAGES}, "meta-outofrun-screens"),
    ({"exact": (_PREFIX + "screens.DoorUnlockScreen",)}, "meta-outofrun-screens"),
    ({"prefix": (_PREFIX + "screens.",)}, "inrun-fullscreens"),
    ({"prefix": _META_OUT_OF_RUN_PACKAGES}, "meta-outofrun-screens"),
    ({"exact": _DRAW_PRIMITIVES}, "draw-primitives-tips"),
)


def _rule_matches(rule, native_class, native_method):
    if "method" in rule and not rule["method"].match(native_method or ""):
        return False
    if "exact" in rule and native_class not in rule["exact"]:
        return False
    if "prefix" in rule and not native_class.startswith(rule["prefix"]):
        return False
    return True


def family_for(native_class, native_method):
    """Return the semantic family id for one native render path.

    Raises ValueError when no rule matches so new scan output can never be
    silently grouped; the message lists the offending class name.
    """
    for rule, family in _RULES:
        if _rule_matches(rule, native_class, native_method):
            return family
    raise ValueError(
        "unclassified native render path (no family rule matched): {}#{}; "
        "add an explicit rule to tools/nrcc/families.py".format(
            native_class, native_method
        )
    )


# All family ids in first-match declaration order (deduplicated).
FAMILY_IDS = []
for _rule, _family in _RULES:
    if _family not in FAMILY_IDS:
        FAMILY_IDS.append(_family)
FAMILY_IDS = tuple(FAMILY_IDS)
