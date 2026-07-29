package artframework.sts1.render;

/**
 * Draw order layers for C2 full-present (milestone 16.3). Host SpriteBatch draws in ordinal
 * order; higher layers paint later.
 */
public enum PresentLayer {
    WORLD_UNDERLAY,
    MAP,
    EVENT,
    SELECT,
    COMBAT_SLOTS,
    COMBAT_HAND,
    COMBAT_CONTROLS,
    SKELETON,
    OVERLAY_FX,
    C1_STAGE
}
