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
        put(m, ResourceIds.UI_PREFIX + "endturn", "images/ui/topPanel/endTurnButton.png");
        put(m, ResourceIds.UI_PREFIX + "energy_red", "images/ui/topPanel/red/energy.png");
        put(m, ResourceIds.UI_PREFIX + "energy_green", "images/ui/topPanel/green/energy.png");
        put(m, ResourceIds.UI_PREFIX + "energy_blue", "images/ui/topPanel/blue/energy.png");
        put(m, ResourceIds.UI_PREFIX + "energy_purple", "images/ui/topPanel/purple/energy.png");
    }

    private static void putCardFrames(Map<String, String> m) {
        put(m, ResourceIds.CARD_FRAME_RED, "images/512/frame_attack_red.png");
        put(m, ResourceIds.CARD_FRAME_GREEN, "images/512/frame_attack_green.png");
        put(m, ResourceIds.CARD_FRAME_BLUE, "images/512/frame_attack_blue.png");
        put(m, ResourceIds.CARD_FRAME_PURPLE, "images/512/frame_attack_purple.png");
        put(m, ResourceIds.CARD_FRAME_COLORLESS, "images/512/frame_attack_colorless.png");
        put(m, ResourceIds.CARD_FRAME_PREFIX + "skill_red", "images/512/frame_skill_red.png");
        put(m, ResourceIds.CARD_FRAME_PREFIX + "power_red", "images/512/frame_power_red.png");
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
