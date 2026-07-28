package artframework.assets;

/**
 * Test helper: dedicated {@link HostAssets} instance (not the process global).
 * Prefer constructing {@link HostAssets} directly; this alias documents the fake role.
 */
public final class FakeHostAssets {

    private FakeHostAssets() {}

    public static HostAssets create() {
        HostAssets a = new HostAssets();
        a.loadMinimalVanillaCatalog();
        return a;
    }

    public static HostAssets empty() {
        return new HostAssets();
    }
}
