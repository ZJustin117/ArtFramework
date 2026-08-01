package artframework.core;

/** Stable identity and window scope for a mounted UI tree. */
public final class TreeIdentityComponent {
    public final String windowId;

    public TreeIdentityComponent(String windowId) {
        this.windowId = windowId;
    }
}
