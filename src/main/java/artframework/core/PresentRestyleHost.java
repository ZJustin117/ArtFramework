package artframework.core;

/** Optional host cache adapter for applying already-resolved present changes. */
public final class PresentRestyleHost {

    public interface Adapter {
        void reattach(String windowId);

        void refreshDefaultSkin();
    }

    private static Adapter adapter;

    private PresentRestyleHost() {}

    public static void install(Adapter value) {
        adapter = value;
    }

    public static void reattach(String windowId) {
        if (adapter != null) adapter.reattach(windowId);
    }

    public static void refreshDefaultSkin() {
        if (adapter != null) adapter.refreshDefaultSkin();
    }

    public static void resetForTests() {
        adapter = null;
    }
}
