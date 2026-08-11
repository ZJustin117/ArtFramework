package artframework.core;

import artframework.presentation.NodeTree;

/** Default host for JUnit / headless. */
public final class NoOpHostBackend implements HostBackend {

    public static final NoOpHostBackend INSTANCE = new NoOpHostBackend();

    private NoOpHostBackend() {}

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void attach(NodeTree tree) {}

    @Override
    public void detach(NodeTree tree) {}

    @Override
    public void applyLayout(NodeTree tree) {}
}
