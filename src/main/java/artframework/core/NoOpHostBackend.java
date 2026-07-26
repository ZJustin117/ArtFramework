package artframework.core;

/** Default host for JUnit / headless. */
public final class NoOpHostBackend implements HostBackend {

    public static final NoOpHostBackend INSTANCE = new NoOpHostBackend();

    private NoOpHostBackend() {}

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void attach(UiTree tree) {}

    @Override
    public void detach(UiTree tree) {}

    @Override
    public void applyLayout(UiTree tree) {}
}
