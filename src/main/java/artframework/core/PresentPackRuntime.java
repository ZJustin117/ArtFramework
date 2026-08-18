package artframework.core;

import artframework.assets.HostAssetsHolder;
import artframework.ecs.ArtEcs;

/** Process runtime for enabled pack operations; catalogs remain outside this mutable operation log. */
public final class PresentPackRuntime {
    private static PackWorld world = new PackWorld(ArtEcs.world(), HostAssetsHolder.get());

    private PresentPackRuntime() {}

    public static void enable(PresentPack pack) { world.enable(pack); }

    public static void disable(String packId) { world.disable(packId); }

    static void abort(String packId) { world.abort(packId); }

    public static boolean isEnabled(String packId) { return world.isEnabled(packId); }

    public static void resetForTests() {
        for (String id : new java.util.ArrayList<String>(world.enabledPackIds())) {
            try {
                world.disable(id);
            } catch (RuntimeException ignored) {
                world.discardForReset(id);
            }
        }
        PackSystems.resetForTests();
    }
}
