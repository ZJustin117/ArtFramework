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
    public static final String MAP = NativeTemplateIds.MAP;
    public static final String SKELETON = "sts1.skeleton";
    public static final String EVENT = NativeTemplateIds.EVENT;
    public static final String SELECT_GRID = NativeTemplateIds.SELECT_GRID;
    public static final String SELECT_HAND = NativeTemplateIds.SELECT_HAND;
    public static final String END_TURN = NativeTemplateIds.END_TURN;

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
        if (COMBAT_CARD_SLOTS.equals(id) || COMBAT_SURFACE.equals(id) || SKELETON.equals(id)) {
            return id;
        }
        return NativeTemplateIds.canonicalize(id);
    }
}
