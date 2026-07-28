package artframework.context;

/**
 * Process-global Primary {@link PresentationBackend} binder.
 */
public final class PresentBackends {

    private static PresentationBackend backend = NoOpPresentationBackend.INSTANCE;

    private PresentBackends() {}

    public static PresentationBackend get() {
        return backend;
    }

    /**
     * Bind primary backend. Replaces any previous primary (no silent multi-primary).
     */
    public static void bind(PresentationBackend primary) {
        backend = primary != null ? primary : NoOpPresentationBackend.INSTANCE;
    }

    public static void resetForTests() {
        backend = NoOpPresentationBackend.INSTANCE;
    }
}
