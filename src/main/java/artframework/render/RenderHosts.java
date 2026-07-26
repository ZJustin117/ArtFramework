package artframework.render;

/**
 * Process-global {@link RenderHost}.
 */
public final class RenderHosts {

    private static final RenderHost GLOBAL = new RenderHost();

    private RenderHosts() {}

    public static RenderHost get() {
        return GLOBAL;
    }

    public static void resetForTests() {
        GLOBAL.resetForTests();
    }
}
