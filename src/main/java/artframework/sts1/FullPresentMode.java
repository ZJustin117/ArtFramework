package artframework.sts1;

/**
 * Explicit STS1 full-present feature switches. All switches default off so unsupported hosts
 * retain native UI. Surface mount alone never hides native UI.
 */
public final class FullPresentMode {

    private static boolean combatHand;

    private FullPresentMode() {}

    public static boolean isCombatHandEnabled() {
        return combatHand;
    }

    public static void setCombatHandEnabled(boolean enabled) {
        combatHand = enabled;
    }

    public static void resetForTests() {
        combatHand = false;
    }
}
