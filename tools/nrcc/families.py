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

# Family -> default manifest policy.  Families without an entry inherit
# UNKNOWN; only deliberate out-of-scope groups carry a default here.
FAMILY_DEFAULT_POLICY = {
    "meta-outofrun-screens": "OUT_OF_SCOPE",
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
