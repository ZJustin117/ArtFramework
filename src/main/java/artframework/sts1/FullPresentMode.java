package artframework.sts1;

import artframework.context.SurfaceIds;

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
        }
    }

    /**
     * FULL + explicit enable is required before ART may suppress native UI for a surface. OBSERVE
     * never suppresses.
     */
    public static boolean maySuppressNative(String surfaceId) {
        if (PresentSafety.isPanic()) {
            return false;
        }
        return levelOf(surfaceId).allowsFullPresent();
    }

    public static boolean mayOwnInput(String surfaceId) {
        if (PresentSafety.isPanic()) {
            return false;
        }
        return levelOf(surfaceId).allowsFullPresent();
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("combatHand", combatHand.name());
        m.put("combatControls", combatControls.name());
        m.put("map", map.name());
        m.put("skeleton", skeleton.name());
        m.put("combatHandFull", Boolean.valueOf(combatHand.allowsFullPresent() && !PresentSafety.isPanic()));
        m.put("maySuppressNativeHand", Boolean.valueOf(maySuppressNative(SurfaceIds.COMBAT_HAND)));
        m.put("panic", Boolean.valueOf(PresentSafety.isPanic()));
        return m;
    }

    public static void resetForTests() {
        combatHand = PresentLevel.OFF;
        combatControls = PresentLevel.OFF;
        map = PresentLevel.OFF;
        skeleton = PresentLevel.OFF;
    }
}
