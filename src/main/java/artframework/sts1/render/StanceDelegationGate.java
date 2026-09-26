package artframework.sts1.render;

/**
 * Default-off switch for the {@code stances-state} family ART takeover.
 *
 * <p>While inactive (the default), the native {@code AbstractStance.render} pixels
 * continue unchanged. This is the ownership seam S2b-2 will drive once real ART
 * stance drawing exists; this slice adds only the gate.
 */
public final class StanceDelegationGate {

    private static volatile boolean active = false;

    private StanceDelegationGate() {}

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean active) {
        StanceDelegationGate.active = active;
    }

    public static void resetForTests() {
        active = false;
    }
}
