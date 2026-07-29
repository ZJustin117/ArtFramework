package artframework.context;

import artframework.c2.NativeTemplateIds;

/**
 * Canonical full-present surface ids (milestone 15) plus legacy aliases.
 */
public final class SurfaceIds {

    public static final String COMBAT_HAND = "sts1.combat.hand";
    public static final String COMBAT_CARD_SLOTS = "sts1.combat.card_slots";
    public static final String COMBAT_CONTROLS = "sts1.combat.controls";
    public static final String COMBAT_SURFACE = "sts1.combat.surface";
    public static final String COMBAT_PROCEED = "sts1.combat.proceed";
    public static final String COMBAT_ENERGY = "sts1.combat.energy";
    public static final String COMBAT_INTENTS = "sts1.combat.intents";
    public static final String MAP = NativeTemplateIds.MAP;
    public static final String SKELETON = "sts1.skeleton";
    public static final String EVENT = NativeTemplateIds.EVENT;
    public static final String SELECT_GRID = NativeTemplateIds.SELECT_GRID;
    public static final String SELECT_HAND = NativeTemplateIds.SELECT_HAND;
    public static final String END_TURN = NativeTemplateIds.END_TURN;
    public static final String REWARD_COMBAT = "sts1.reward.combat";
    public static final String REWARD_CARD = "sts1.reward.card";
    public static final String REWARD_BOSS_RELIC = "sts1.reward.boss_relic";
    public static final String REST = "sts1.rest";
    public static final String TREASURE = "sts1.treasure";
    public static final String SHOP = "sts1.shop";
    public static final String TOP_PANEL = "sts1.top_panel";

    private SurfaceIds() {}

    public static String canonicalize(String id) {
        if (id == null) {
            return null;
        }
        if (COMBAT_HAND.equals(id)
                || "sts.combat.hand".equals(id)
                || "sts1.hand".equals(id)) {
            return COMBAT_HAND;
        }
        if (COMBAT_CONTROLS.equals(id) || "sts.combat.controls".equals(id)) {
            return COMBAT_CONTROLS;
        }
        if (COMBAT_CARD_SLOTS.equals(id)
                || COMBAT_SURFACE.equals(id)
                || COMBAT_PROCEED.equals(id)
                || COMBAT_ENERGY.equals(id)
                || COMBAT_INTENTS.equals(id)
                || SKELETON.equals(id)
                || REWARD_COMBAT.equals(id)
                || REWARD_CARD.equals(id)
                || REWARD_BOSS_RELIC.equals(id)
                || REST.equals(id)
                || TREASURE.equals(id)
                || SHOP.equals(id)
                || TOP_PANEL.equals(id)) {
            return id;
        }
        return NativeTemplateIds.canonicalize(id);
    }
}
