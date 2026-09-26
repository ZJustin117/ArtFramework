package artframework.sts1.render;

/**
 * Readiness seam for ART stance drawing.
 *
 * <p>S2b-2 will implement the actual ART stance pixels. Until then {@link #isReady()}
 * is always {@code false}, so any delegation attempt fails open and the native
 * stance render continues.
 */
public final class StanceArtRenderer {

    private static volatile boolean ready = false;

    private StanceArtRenderer() {}

    public static boolean isReady() {
        return ready;
    }

    /** Test-only hook to force the ready flag. */
    static void setReadyForTests(boolean ready) {
        StanceArtRenderer.ready = ready;
    }

    public static void resetForTests() {
        ready = false;
    }
}
