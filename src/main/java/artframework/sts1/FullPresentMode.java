package artframework.sts1;

import artframework.context.SurfaceIds;
import artframework.sts1.input.CombatInputRouter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explicit STS1 full-present feature switches (milestone 16.0 policy). All surfaces default
 * {@link PresentLevel#OFF} so unsupported hosts retain native UI. Surface mount alone never hides
 * native UI — only {@link PresentLevel#FULL} plus mount may suppress.
 */
public final class FullPresentMode {

    private static PresentLevel combatHand = PresentLevel.OFF;
    private static PresentLevel combatControls = PresentLevel.OFF;
    private static PresentLevel map = PresentLevel.OFF;
    private static PresentLevel skeleton = PresentLevel.OFF;
    private static PresentLevel event = PresentLevel.OFF;
    private static PresentLevel select = PresentLevel.OFF;
    private static PresentLevel reward = PresentLevel.OFF;
    private static PresentLevel rest = PresentLevel.OFF;
    private static PresentLevel treasure = PresentLevel.OFF;
    private static PresentLevel shop = PresentLevel.OFF;
    private static PresentLevel topPanel = PresentLevel.OFF;
    private static PresentLevel intents = PresentLevel.OFF;
    private static PresentLevel targeting = PresentLevel.OFF;
    private static PresentLevel proceed = PresentLevel.OFF;
    private static PresentLevel energy = PresentLevel.OFF;

    private FullPresentMode() {}

    public static PresentLevel combatHandLevel() {
        return combatHand;
    }

    public static PresentLevel combatControlsLevel() {
        return combatControls;
    }

    public static PresentLevel mapLevel() {
        return map;
    }

    public static PresentLevel skeletonLevel() {
        return skeleton;
    }

    public static PresentLevel levelOf(String surfaceId) {
        String id = SurfaceIds.canonicalize(surfaceId);
        if (SurfaceIds.COMBAT_HAND.equals(id)
                || SurfaceIds.COMBAT_CARD_SLOTS.equals(id)
                || SurfaceIds.COMBAT_SURFACE.equals(id)) {
            return combatHand;
        }
        if (SurfaceIds.COMBAT_CONTROLS.equals(id) || SurfaceIds.END_TURN.equals(id)) {
            return combatControls;
        }
        if (SurfaceIds.MAP.equals(id)) {
            return map;
        }
        if (SurfaceIds.SKELETON.equals(id)) {
            return skeleton;
        }
        if (SurfaceIds.EVENT.equals(id)) {
            return event;
        }
        if (SurfaceIds.SELECT_GRID.equals(id) || SurfaceIds.SELECT_HAND.equals(id)) {
            return select;
        }
        if (SurfaceIds.REWARD_COMBAT.equals(id)
                || SurfaceIds.REWARD_CARD.equals(id)
                || SurfaceIds.REWARD_BOSS_RELIC.equals(id)) {
            return reward;
        }
        if (SurfaceIds.REST.equals(id)) {
            return rest;
        }
        if (SurfaceIds.TREASURE.equals(id)) {
            return treasure;
        }
        if (SurfaceIds.SHOP.equals(id)) {
            return shop;
        }
        if (SurfaceIds.TOP_PANEL.equals(id)) {
            return topPanel;
        }
        if (SurfaceIds.COMBAT_INTENTS.equals(id)) {
            return intents;
        }
        if (SurfaceIds.COMBAT_TARGETING.equals(id)) {
            return targeting;
        }
        if (SurfaceIds.COMBAT_PROCEED.equals(id)) {
            return proceed;
        }
        if (SurfaceIds.COMBAT_ENERGY.equals(id)) {
            return energy;
        }
        return PresentLevel.OFF;
    }

    /** True when combat hand is at FULL (compat with pre-16.0 boolean switch). */
    public static boolean isCombatHandEnabled() {
        return combatHand.allowsFullPresent();
    }

    public static void setCombatHandEnabled(boolean enabled) {
        combatHand = enabled ? PresentLevel.FULL : PresentLevel.OFF;
    }

    public static void setCombatHandLevel(PresentLevel level) {
        combatHand = level != null ? level : PresentLevel.OFF;
    }

    public static void setCombatControlsLevel(PresentLevel level) {
        combatControls = level != null ? level : PresentLevel.OFF;
    }

    public static void setMapLevel(PresentLevel level) {
        map = level != null ? level : PresentLevel.OFF;
    }

    public static void setSkeletonLevel(PresentLevel level) {
        skeleton = level != null ? level : PresentLevel.OFF;
    }

    public static void setEventLevel(PresentLevel level) {
        event = level != null ? level : PresentLevel.OFF;
    }

    public static void setSelectLevel(PresentLevel level) {
        select = level != null ? level : PresentLevel.OFF;
    }

    public static void setRewardLevel(PresentLevel level) {
        reward = level != null ? level : PresentLevel.OFF;
    }

    public static void setRestLevel(PresentLevel level) {
        rest = level != null ? level : PresentLevel.OFF;
    }

    public static void setTreasureLevel(PresentLevel level) {
        treasure = level != null ? level : PresentLevel.OFF;
    }

    public static void setShopLevel(PresentLevel level) {
        shop = level != null ? level : PresentLevel.OFF;
    }

    public static void setTopPanelLevel(PresentLevel level) {
        topPanel = level != null ? level : PresentLevel.OFF;
    }

    public static void setIntentsLevel(PresentLevel level) {
        intents = level != null ? level : PresentLevel.OFF;
    }

    public static void setTargetingLevel(PresentLevel level) {
        targeting = level != null ? level : PresentLevel.OFF;
    }

    public static void setProceedLevel(PresentLevel level) {
        proceed = level != null ? level : PresentLevel.OFF;
    }

    public static void setEnergyLevel(PresentLevel level) {
        energy = level != null ? level : PresentLevel.OFF;
    }

    public static PresentLevel eventLevel() {
        return event;
    }

    public static PresentLevel selectLevel() {
        return select;
    }

    public static PresentLevel rewardLevel() {
        return reward;
    }

    public static PresentLevel restLevel() {
        return rest;
    }

    public static PresentLevel treasureLevel() {
        return treasure;
    }

    public static PresentLevel shopLevel() {
        return shop;
    }

    public static PresentLevel topPanelLevel() {
        return topPanel;
    }

    public static PresentLevel intentsLevel() {
        return intents;
    }

    public static PresentLevel targetingLevel() {
        return targeting;
    }

    public static PresentLevel proceedLevel() {
        return proceed;
    }

    public static PresentLevel energyLevel() {
        return energy;
    }

    public static void setLevel(String surfaceId, PresentLevel level) {
        String id = SurfaceIds.canonicalize(surfaceId);
        PresentLevel v = level != null ? level : PresentLevel.OFF;
        if (SurfaceIds.COMBAT_HAND.equals(id)
                || SurfaceIds.COMBAT_CARD_SLOTS.equals(id)
                || SurfaceIds.COMBAT_SURFACE.equals(id)) {
            combatHand = v;
            return;
        }
        if (SurfaceIds.COMBAT_CONTROLS.equals(id) || SurfaceIds.END_TURN.equals(id)) {
            combatControls = v;
            return;
        }
        if (SurfaceIds.MAP.equals(id)) {
            map = v;
            return;
        }
        if (SurfaceIds.SKELETON.equals(id)) {
            skeleton = v;
            return;
        }
        if (SurfaceIds.EVENT.equals(id)) {
            event = v;
            return;
        }
        if (SurfaceIds.SELECT_GRID.equals(id) || SurfaceIds.SELECT_HAND.equals(id)) {
            select = v;
            return;
        }
        if (SurfaceIds.REWARD_COMBAT.equals(id)
                || SurfaceIds.REWARD_CARD.equals(id)
                || SurfaceIds.REWARD_BOSS_RELIC.equals(id)) {
            reward = v;
            return;
        }
        if (SurfaceIds.REST.equals(id)) {
            rest = v;
            return;
        }
        if (SurfaceIds.TREASURE.equals(id)) {
            treasure = v;
            return;
        }
        if (SurfaceIds.SHOP.equals(id)) {
            shop = v;
            return;
        }
        if (SurfaceIds.TOP_PANEL.equals(id)) {
            topPanel = v;
            return;
        }
        if (SurfaceIds.COMBAT_INTENTS.equals(id)) {
            intents = v;
            return;
        }
        if (SurfaceIds.COMBAT_TARGETING.equals(id)) {
            targeting = v;
            return;
        }
        if (SurfaceIds.COMBAT_PROCEED.equals(id)) {
            proceed = v;
            return;
        }
        if (SurfaceIds.COMBAT_ENERGY.equals(id)) {
            energy = v;
        }
    }

    /**
     * Effective suppression requires a ready mounted surface; this is distinct from a requested
     * {@link PresentLevel#FULL} policy.
     */
    public static boolean maySuppressNative(String surfaceId) {
        return CombatInputRouter.capability(surfaceId).shouldSuppressNative();
    }

    public static boolean mayOwnInput(String surfaceId) {
        return CombatInputRouter.capability(surfaceId).ownsInput();
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("combatHand", combatHand.name());
        m.put("combatControls", combatControls.name());
        m.put("map", map.name());
        m.put("skeleton", skeleton.name());
        m.put("event", event.name());
        m.put("select", select.name());
        m.put("reward", reward.name());
        m.put("rest", rest.name());
        m.put("treasure", treasure.name());
        m.put("shop", shop.name());
        m.put("topPanel", topPanel.name());
        m.put("intents", intents.name());
        m.put("targeting", targeting.name());
        m.put("proceed", proceed.name());
        m.put("energy", energy.name());
        m.put("combatHandFull", Boolean.valueOf(combatHand.allowsFullPresent() && !PresentSafety.isPanic()));
        m.put("maySuppressNativeHand", Boolean.valueOf(maySuppressNative(SurfaceIds.COMBAT_HAND)));
        m.put("maySuppressNativeEvent", Boolean.valueOf(maySuppressNative(SurfaceIds.EVENT)));
        m.put("maySuppressNativeSelect", Boolean.valueOf(maySuppressNative(SurfaceIds.SELECT_GRID)));
        m.put("panic", Boolean.valueOf(PresentSafety.isPanic()));
        return m;
    }

    public static void resetForTests() {
        combatHand = PresentLevel.OFF;
        combatControls = PresentLevel.OFF;
        map = PresentLevel.OFF;
        skeleton = PresentLevel.OFF;
        event = PresentLevel.OFF;
        select = PresentLevel.OFF;
        reward = PresentLevel.OFF;
        rest = PresentLevel.OFF;
        treasure = PresentLevel.OFF;
        shop = PresentLevel.OFF;
        topPanel = PresentLevel.OFF;
        intents = PresentLevel.OFF;
        targeting = PresentLevel.OFF;
        proceed = PresentLevel.OFF;
        energy = PresentLevel.OFF;
    }
}
