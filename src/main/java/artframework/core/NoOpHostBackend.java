package artframework.core;

import artframework.presentation.PresentationMount;

/** Default host for JUnit / headless. */
public final class NoOpHostBackend implements HostBackend {

    public static final NoOpHostBackend INSTANCE = new NoOpHostBackend();

    private NoOpHostBackend() {}

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void attach(PresentationMount mount) {}

    @Override
    public void detach(PresentationMount mount) {}

    @Override
    public void applyLayout(PresentationMount mount) {}
}
