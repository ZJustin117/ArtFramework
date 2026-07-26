package artframework.core;

/**
 * Process-global {@link HostBackend} (STS1 Stage adapter installed at runtime).
 */
public final class HostBackends {

    private static HostBackend backend = NoOpHostBackend.INSTANCE;

    private HostBackends() {}

    public static HostBackend get() {
        return backend;
    }

    public static void set(HostBackend host) {
        backend = host != null ? host : NoOpHostBackend.INSTANCE;
    }

    public static void resetForTests() {
        backend = NoOpHostBackend.INSTANCE;
    }
}
