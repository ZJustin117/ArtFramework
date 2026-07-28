package artframework.context;

/** Process-global {@link FrameRuntime}. */
public final class FrameRuntimes {

    private static final FrameRuntime RUNTIME = new FrameRuntime();

    private FrameRuntimes() {}

    public static FrameRuntime get() {
        return RUNTIME;
    }

    public static void resetForTests() {
        RUNTIME.reset();
        PresentBackends.resetForTests();
    }
}
