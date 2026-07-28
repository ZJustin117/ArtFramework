package artframework.assets;

/**
 * ResourceId conventions and minimal vanilla catalog keys (milestone 15.3).
 */
public final class ResourceIds {

    public static final String CARD_ART_PREFIX = "card.art.";
    public static final String CARD_FRAME_PREFIX = "card.frame.";
    public static final String MAP_NODE_PREFIX = "map.node.";
    public static final String MAP_BG_PREFIX = "map.bg.";
    public static final String UI_PREFIX = "ui.";
    public static final String CHAR_PREFIX = "char.";
    public static final String AUDIO_SFX_PREFIX = "audio.sfx.";
    public static final String AUDIO_BGM_PREFIX = "audio.bgm.";

    public static final String UI_BUTTON_DEFAULT = "ui.button.default";
    public static final String UI_PANEL_DEFAULT = "ui.panel.default";
    public static final String UI_WINDOW_DEFAULT = "ui.window.default";
    public static final String CARD_FRAME_RED = "card.frame.red";
    public static final String CARD_FRAME_GREEN = "card.frame.green";
    public static final String CARD_FRAME_BLUE = "card.frame.blue";
    public static final String CARD_FRAME_PURPLE = "card.frame.purple";
    public static final String CARD_FRAME_COLORLESS = "card.frame.colorless";
    public static final String MAP_NODE_MONSTER = "map.node.monster";
    public static final String MAP_NODE_ELITE = "map.node.elite";
    public static final String MAP_NODE_REST = "map.node.rest";
    public static final String MAP_NODE_SHOP = "map.node.shop";
    public static final String MAP_NODE_TREASURE = "map.node.treasure";
    public static final String MAP_NODE_EVENT = "map.node.event";
    public static final String MAP_NODE_BOSS = "map.node.boss";

    private ResourceIds() {}

    public static String cardArt(String cardId) {
        return CARD_ART_PREFIX + (cardId != null ? cardId : "");
    }

    public static String cardFrame(String color) {
        return CARD_FRAME_PREFIX + (color != null ? color : "colorless");
    }

    public static String mapNode(String kind) {
        return MAP_NODE_PREFIX + (kind != null ? kind : "unknown");
    }

    public static boolean isValid(String resourceId) {
        if (resourceId == null || resourceId.isEmpty()) {
            return false;
        }
        return resourceId.indexOf('.') > 0;
    }

    /** Minimal vanilla catalog keys for tests and default resolve. */
    public static String[] minimalVanillaKeys() {
        return new String[] {
            UI_BUTTON_DEFAULT,
            UI_PANEL_DEFAULT,
            UI_WINDOW_DEFAULT,
            CARD_FRAME_RED,
            CARD_FRAME_GREEN,
            CARD_FRAME_BLUE,
            CARD_FRAME_PURPLE,
            CARD_FRAME_COLORLESS,
            MAP_NODE_MONSTER,
            MAP_NODE_ELITE,
            MAP_NODE_REST,
            MAP_NODE_SHOP,
            MAP_NODE_TREASURE,
            MAP_NODE_EVENT,
            MAP_NODE_BOSS,
            cardArt("Strike_R"),
            cardArt("Defend_R"),
            cardArt("Strike_G"),
            cardArt("Defend_G"),
            cardArt("Strike_B"),
            cardArt("Defend_B"),
        };
    }
}
