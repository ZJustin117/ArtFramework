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
    public static final String UI_EVENT_PREFIX = "ui.event.";
    public static final String UI_REWARD_PREFIX = "ui.reward.";
    public static final String UI_CAMPFIRE_PREFIX = "ui.campfire.";
    public static final String UI_COMBAT_PREFIX = "ui.combat.";
    public static final String UI_INTENT_PREFIX = "ui.intent.";
    public static final String CARD_BANNER_PREFIX = "card.banner.";
    public static final String CARD_ORB_PREFIX = "card.orb.";

    public static final String UI_BUTTON_DEFAULT = "ui.button.default";
    public static final String UI_PANEL_DEFAULT = "ui.panel.default";
    public static final String UI_WINDOW_DEFAULT = "ui.window.default";
    public static final String UI_BUTTON_END_TURN = "ui.button.end_turn";
    public static final String UI_BUTTON_END_TURN_DISABLED = "ui.button.end_turn.disabled";
    public static final String UI_BUTTON_END_TURN_HOVER = "ui.button.end_turn.hover";
    public static final String UI_BUTTON_PROCEED_ENABLED = "ui.button.proceed.enabled";
    public static final String UI_BUTTON_PROCEED_DISABLED = "ui.button.proceed.disabled";
    public static final String UI_BUTTON_CANCEL_ENABLED = "ui.button.cancel.enabled";
    public static final String UI_BUTTON_CANCEL_DISABLED = "ui.button.cancel.disabled";
    public static final String UI_EVENT_BUTTON_ENABLED = "ui.event.button.enabled";
    public static final String UI_EVENT_BUTTON_DISABLED = "ui.event.button.disabled";
    public static final String UI_EVENT_PANEL = "ui.event.panel";
    public static final String UI_EVENT_TITLE = "ui.event.title";
    public static final String UI_SELECT_CARD = "ui.select.card";
    public static final String UI_SELECT_CARD_DISABLED = "ui.select.card.disabled";
    public static final String UI_SELECT_CARD_SELECTED = "ui.select.card.selected";
    public static final String UI_SELECT_CARD_FRAME = "ui.select.card.frame";
    public static final String UI_SELECT_CONFIRM = "ui.select.confirm";
    public static final String UI_SELECT_CONFIRM_DISABLED = "ui.select.confirm.disabled";
    public static final String UI_REWARD_PANEL = "ui.reward.panel";
    public static final String UI_REWARD_CARD = "ui.reward.card";
    public static final String UI_REWARD_GOLD = "ui.reward.gold";
    public static final String UI_REWARD_RELIC = "ui.reward.relic";
    public static final String UI_REWARD_BOSS_RELIC = "ui.reward.boss_relic";
    public static final String UI_REWARD_DISABLED = "ui.reward.disabled";
    public static final String UI_CAMPFIRE_PANEL = "ui.campfire.panel";
    public static final String UI_CAMPFIRE_REST_OPTION = "ui.campfire.option.rest";
    public static final String UI_CAMPFIRE_SMITH_OPTION = "ui.campfire.option.smith";
    public static final String UI_CAMPFIRE_DIG_OPTION = "ui.campfire.option.dig";
    public static final String UI_CAMPFIRE_RECALL_OPTION = "ui.campfire.option.recall";
    public static final String UI_CAMPFIRE_TOKE_OPTION = "ui.campfire.option.toke";
    public static final String UI_CAMPFIRE_OTHER_OPTION = "ui.campfire.option.other";
    public static final String UI_CAMPFIRE_DISABLED_OPTION = "ui.campfire.option.disabled";
    public static final String UI_CAMPFIRE_OUTLINE = "ui.campfire.outline";
    public static final String UI_CAMPFIRE_SLEEP = "ui.campfire.sleep";
    public static final String UI_CAMPFIRE_SMITH = "ui.campfire.smith";
    public static final String UI_COMBAT_BLOCK = "ui.combat.block";
    public static final String UI_COMBAT_INTENT_UNKNOWN = "ui.intent.unknown";
    public static final String UI_COMBAT_TARGETING_ARROW = "ui.combat.targeting_arrow";
    public static final String UI_REWARD_TAKE_ALL = "ui.reward.take_all";
    public static final String UI_REWARD_ITEM_PANEL = "ui.reward.item_panel";
    public static final String UI_SHOP_PANEL = "ui.shop.panel";
    public static final String UI_SHOP_MERCHANT = "ui.shop.merchant";
    public static final String UI_SHOP_GOLD = "ui.shop.gold";
    public static final String UI_SHOP_ENTRY_PANEL = "ui.shop.entry_panel";
    public static final String UI_SHOP_PURGE = "ui.shop.purge";
    public static final String UI_SHOP_SOLD_OUT = "ui.shop.sold_out";
    public static final String UI_TREASURE_PANEL = "ui.treasure.panel";
    public static final String UI_TREASURE_CHEST_CLOSED = "ui.treasure.chest.closed";
    public static final String UI_TREASURE_CHEST_OPEN = "ui.treasure.chest.open";
    public static final String UI_TREASURE_RELIC = "ui.treasure.relic";
    public static final String UI_MAP_SELECT = "ui.map.select";
    public static final String UI_MAP_OUTLINE_PREFIX = "ui.map.outline.";
    public static final String UI_TOP_PANEL_BAR = "ui.top_panel.bar";
    public static final String UI_TOP_PANEL_GOLD = "ui.top_panel.gold";
    public static final String UI_TOP_PANEL_HP = "ui.top_panel.hp";
    public static final String UI_TOP_PANEL_FLOOR = "ui.top_panel.floor";
    public static final String UI_TOP_PANEL_ASCENSION = "ui.top_panel.ascension";
    public static final String UI_TOP_PANEL_STATUS = "ui.top_panel.status";
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

    public static String cardBanner(String color) {
        return CARD_BANNER_PREFIX + (color != null ? color : "colorless");
    }

    public static String cardOrb(String color) {
        return CARD_ORB_PREFIX + (color != null ? color : "colorless");
    }

    public static String energyOrb(String color) {
        return UI_PREFIX + "energy." + (color != null ? color : "red");
    }

    public static String energyOrbLayer(String color, int layer) {
        return energyOrb(color) + ".layer" + layer;
    }

    public static String intent(String id) {
        return UI_INTENT_PREFIX + (id != null ? id : "unknown");
    }

    public static String mapOutline(String kind) {
        return UI_MAP_OUTLINE_PREFIX + (kind != null ? kind : "monster");
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
            UI_BUTTON_END_TURN,
            UI_BUTTON_END_TURN_DISABLED,
            UI_BUTTON_END_TURN_HOVER,
            UI_PANEL_DEFAULT,
            UI_WINDOW_DEFAULT,
            UI_EVENT_PANEL,
            UI_EVENT_TITLE,
            UI_EVENT_BUTTON_ENABLED,
            UI_EVENT_BUTTON_DISABLED,
            UI_SELECT_CARD,
            UI_SELECT_CARD_DISABLED,
            UI_SELECT_CARD_SELECTED,
            UI_SELECT_CARD_FRAME,
            UI_SELECT_CONFIRM,
            UI_SELECT_CONFIRM_DISABLED,
            UI_BUTTON_PROCEED_ENABLED,
            UI_BUTTON_PROCEED_DISABLED,
            UI_BUTTON_CANCEL_ENABLED,
            UI_BUTTON_CANCEL_DISABLED,
            UI_REWARD_PANEL,
            UI_REWARD_ITEM_PANEL,
            UI_REWARD_CARD,
            UI_REWARD_GOLD,
            UI_REWARD_RELIC,
            UI_REWARD_BOSS_RELIC,
            UI_REWARD_DISABLED,
            UI_CAMPFIRE_PANEL,
            UI_CAMPFIRE_REST_OPTION,
            UI_CAMPFIRE_SMITH_OPTION,
            UI_CAMPFIRE_DIG_OPTION,
            UI_CAMPFIRE_RECALL_OPTION,
            UI_CAMPFIRE_TOKE_OPTION,
            UI_CAMPFIRE_OTHER_OPTION,
            UI_CAMPFIRE_DISABLED_OPTION,
            UI_CAMPFIRE_OUTLINE,
            UI_CAMPFIRE_SLEEP,
            UI_CAMPFIRE_SMITH,
            UI_SHOP_PANEL,
            UI_SHOP_MERCHANT,
            UI_SHOP_GOLD,
            UI_SHOP_ENTRY_PANEL,
            UI_SHOP_PURGE,
            UI_SHOP_SOLD_OUT,
            UI_TREASURE_PANEL,
            UI_TREASURE_CHEST_CLOSED,
            UI_TREASURE_CHEST_OPEN,
            UI_TREASURE_RELIC,
            UI_TOP_PANEL_BAR,
            UI_TOP_PANEL_HP,
            UI_TOP_PANEL_GOLD,
            UI_TOP_PANEL_FLOOR,
            UI_TOP_PANEL_ASCENSION,
            UI_TOP_PANEL_STATUS,
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
