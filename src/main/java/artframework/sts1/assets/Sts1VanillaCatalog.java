package artframework.sts1.assets;

import artframework.assets.ResourceIds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * STS1 vanilla ResourceId → logical source path catalog (milestone 16.2). Pure map; no GL.
 * Sources use {@code sts1:} prefix so HostAssets can distinguish host paths from pack files.
 */
public final class Sts1VanillaCatalog {

    public static final String SOURCE_PREFIX = "sts1:";

    private Sts1VanillaCatalog() {}

    /** Full catalog map for {@link artframework.assets.HostAssets#registerVanillaCatalog}. */
    public static Map<String, String> catalog() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        putUi(m);
        putCardFrames(m);
        putMapNodes(m);
        putCommonCardArt(m);
        putAudio(m);
        return Collections.unmodifiableMap(m);
    }

    public static String sourceFor(String resourceId) {
        if (resourceId == null) {
            return "";
        }
        String s = catalog().get(resourceId);
        return s != null ? s : SOURCE_PREFIX + resourceId;
    }

    public static boolean isKnown(String resourceId) {
        return resourceId != null && catalog().containsKey(resourceId);
    }

    private static void putUi(Map<String, String> m) {
        put(m, ResourceIds.UI_BUTTON_DEFAULT, "images/ui/topPanel/buttonBlue.png");
        put(m, ResourceIds.UI_PANEL_DEFAULT, "images/ui/reward/rewardList.png");
        put(m, ResourceIds.UI_WINDOW_DEFAULT, "images/ui/tip.png");
        put(m, ResourceIds.UI_BUTTON_END_TURN, "images/ui/topPanel/endTurnButton.png");
        // Historical C2 draw paths resolve this key; retain it while public nodes use button.end_turn.
        put(m, ResourceIds.UI_PREFIX + "endturn", "images/ui/topPanel/endTurnButton.png");
        put(m, ResourceIds.UI_BUTTON_END_TURN_HOVER, "images/ui/topPanel/endTurnHover.png");
        put(m, ResourceIds.energyOrb("red"), "images/ui/topPanel/red/layer1.png");
        put(m, ResourceIds.energyOrbLayer("red", 1), "images/ui/topPanel/red/layer1.png");
        put(m, ResourceIds.energyOrbLayer("red", 2), "images/ui/topPanel/red/layer2.png");
        put(m, ResourceIds.energyOrbLayer("red", 3), "images/ui/topPanel/red/layer3.png");
        put(m, ResourceIds.energyOrb("green"), "images/ui/topPanel/green/layer1.png");
        put(m, ResourceIds.energyOrbLayer("green", 1), "images/ui/topPanel/green/layer1.png");
        put(m, ResourceIds.energyOrbLayer("green", 2), "images/ui/topPanel/green/layer2.png");
        put(m, ResourceIds.energyOrbLayer("green", 3), "images/ui/topPanel/green/layer3.png");
        put(m, ResourceIds.energyOrb("blue"), "images/ui/topPanel/blue/layer1.png");
        put(m, ResourceIds.energyOrb("purple"), "images/ui/topPanel/purple/l1.png");
        put(m, ResourceIds.UI_EVENT_BUTTON_ENABLED, "images/ui/event/enabledButton.png");
        put(m, ResourceIds.UI_EVENT_BUTTON_DISABLED, "images/ui/event/disabledButton.png");
        put(m, ResourceIds.UI_EVENT_PANEL, "images/ui/event/panel.png");
        put(m, ResourceIds.UI_REWARD_PANEL, "images/ui/reward/rewardList.png");
        put(m, ResourceIds.UI_REWARD_CARD, "images/ui/reward/normalCardReward.png");
        put(m, ResourceIds.UI_CAMPFIRE_OUTLINE, "images/ui/campfire/outline.png");
        put(m, ResourceIds.UI_CAMPFIRE_SLEEP, "images/ui/campfire/sleep.png");
        put(m, ResourceIds.UI_CAMPFIRE_SMITH, "images/ui/campfire/smith.png");
        put(m, ResourceIds.UI_COMBAT_BLOCK, "images/ui/combat/block.png");
        put(m, ResourceIds.UI_COMBAT_INTENT_UNKNOWN, "images/ui/combat/reticleBlock.png");
        putIntent(m, "attackBuff", "images/ui/intent/attackBuff.png");
        putIntent(m, "attackDebuff", "images/ui/intent/attackDebuff.png");
        putIntent(m, "attackDefend", "images/ui/intent/attackDefend.png");
        putIntent(m, "defend", "images/ui/intent/defend.png");
        putIntent(m, "defendBuff", "images/ui/intent/defendBuff.png");
        putIntent(m, "buff1", "images/ui/intent/buff1.png");
        putIntent(m, "debuff1", "images/ui/intent/debuff1.png");
        putIntent(m, "magic", "images/ui/intent/magic.png");
        putIntent(m, "sleep", "images/ui/intent/sleep.png");
        putIntent(m, "stun", "images/ui/intent/stun.png");
        putIntent(m, "escape", "images/ui/intent/escape.png");
        putIntent(m, "special", "images/ui/intent/special.png");
        for (int i = 1; i <= 7; i++) {
            putIntent(m, "attack/attack_intent_" + i,
                    "images/ui/intent/attack/attack_intent_" + i + ".png");
        }
        put(m, ResourceIds.UI_REWARD_TAKE_ALL, "images/ui/reward/takeAll.png");
        put(m, ResourceIds.UI_REWARD_ITEM_PANEL, "images/ui/reward/rewardListItemPanel.png");
        put(m, ResourceIds.UI_MAP_SELECT, "images/ui/map/selectBox.png");
        put(m, ResourceIds.UI_TOP_PANEL_BAR, "images/ui/topPanel/bar.png");
        put(m, ResourceIds.UI_TOP_PANEL_GOLD, "images/ui/topPanel/gold.png");
    }

    private static void putCardFrames(Map<String, String> m) {
        // Card frames are regions in STS's cardui atlas, not files below images/.
        put(m, ResourceIds.CARD_FRAME_RED, "cardui/frame");
        put(m, ResourceIds.CARD_FRAME_GREEN, "cardui/frame");
        put(m, ResourceIds.CARD_FRAME_BLUE, "cardui/frame");
        put(m, ResourceIds.CARD_FRAME_PURPLE, "cardui/frame");
        put(m, ResourceIds.CARD_FRAME_COLORLESS, "cardui/frame");
        put(m, ResourceIds.CARD_FRAME_PREFIX + "skill_red", "cardui/frame");
        put(m, ResourceIds.CARD_FRAME_PREFIX + "power_red", "cardui/frame");
    }

    private static void putIntent(Map<String, String> m, String id, String source) {
        put(m, ResourceIds.intent(id), source);
    }

    private static void putMapNodes(Map<String, String> m) {
        put(m, ResourceIds.MAP_NODE_MONSTER, "images/ui/map/monster.png");
        put(m, ResourceIds.MAP_NODE_ELITE, "images/ui/map/elite.png");
        put(m, ResourceIds.MAP_NODE_REST, "images/ui/map/rest.png");
        put(m, ResourceIds.MAP_NODE_SHOP, "images/ui/map/shop.png");
        put(m, ResourceIds.MAP_NODE_TREASURE, "images/ui/map/chest.png");
        put(m, ResourceIds.MAP_NODE_EVENT, "images/ui/map/event.png");
        put(m, ResourceIds.MAP_NODE_BOSS, "images/ui/map/boss.png");
        put(m, ResourceIds.MAP_BG_PREFIX + "act1", "images/ui/map/mapBg.png");
        put(m, ResourceIds.mapOutline("monster"), "images/ui/map/monsterOutline.png");
        put(m, ResourceIds.mapOutline("elite"), "images/ui/map/eliteOutline.png");
        put(m, ResourceIds.mapOutline("rest"), "images/ui/map/restOutline.png");
        put(m, ResourceIds.mapOutline("shop"), "images/ui/map/shopOutline.png");
        put(m, ResourceIds.mapOutline("treasure"), "images/ui/map/chestOutline.png");
        put(m, ResourceIds.mapOutline("event"), "images/ui/map/eventOutline.png");
    }

    private static void putCommonCardArt(Map<String, String> m) {
        // Logical atlas keys; host texture resolver maps cardId → ImageMaster / CardLibrary later.
        String[] red = new String[] {"Strike_R", "Defend_R", "Bash", "Anger", "Cleave", "Clothesline"};
        String[] green = new String[] {"Strike_G", "Defend_G", "Neutralize", "Survivor"};
        String[] blue = new String[] {"Strike_B", "Defend_B", "Zap", "Dualcast"};
        String[] purple = new String[] {"Strike_P", "Defend_P", "Eruption", "Vigilance"};
        for (String id : red) {
            put(m, ResourceIds.cardArt(id), "card/art/" + id);
        }
        for (String id : green) {
            put(m, ResourceIds.cardArt(id), "card/art/" + id);
        }
        for (String id : blue) {
            put(m, ResourceIds.cardArt(id), "card/art/" + id);
        }
        for (String id : purple) {
            put(m, ResourceIds.cardArt(id), "card/art/" + id);
        }
    }

    private static void putAudio(Map<String, String> m) {
        put(m, ResourceIds.AUDIO_SFX_PREFIX + "card_select", "audio/sound/card_select.ogg");
        put(m, ResourceIds.AUDIO_SFX_PREFIX + "end_turn", "audio/sound/end_turn.ogg");
        put(m, ResourceIds.AUDIO_BGM_PREFIX + "exordium", "audio/music/Exordium.ogg");
    }

    private static void put(Map<String, String> m, String resourceId, String path) {
        m.put(resourceId, SOURCE_PREFIX + path);
    }
}
