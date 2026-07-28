package artframework.assets;

/** Process-global {@link HostAssets}. */
public final class HostAssetsHolder {

    private static final HostAssets INSTANCE = new HostAssets();

    static {
        INSTANCE.loadMinimalVanillaCatalog();
    }

    private HostAssetsHolder() {}

    public static HostAssets get() {
        return INSTANCE;
    }

    public static void resetForTests() {
        INSTANCE.reset();
        INSTANCE.loadMinimalVanillaCatalog();
    }
}
