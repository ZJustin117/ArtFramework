package artframework.sts1.render;

/**
 * Draw order layers for C2 full-present (milestone 16.3). Host SpriteBatch draws in ordinal
 * order; higher layers paint later.
 */
public enum PresentLayer {
    WORLD_UNDERLAY,
    MAP,
    ROOM,
    EVENT,
    SELECT,
    COMBAT_SLOTS,
    COMBAT_HAND,
    COMBAT_CONTROLS,
    COMBAT_INTENTS,
    SKELETON,
    TOP_PANEL,
    OVERLAY_FX,
    C1_STAGE
}
