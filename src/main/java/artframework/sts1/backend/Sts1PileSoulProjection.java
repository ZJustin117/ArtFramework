package artframework.sts1.backend;

import artframework.context.PileSoulView;

/** Host-neutral current projection; native card pixels and pile/soul renderers remain authoritative. */
public final class Sts1PileSoulProjection {
    private static volatile PileSoulView current = PileSoulView.empty();

    private Sts1PileSoulProjection() {}

    public static PileSoulView current() { return current; }

    public static void publish(PileSoulView view) {
        current = view != null ? view : PileSoulView.empty();
    }

    /** Clears the non-authoritative observation cache during panic/host recovery. */
    public static void clear() { current = PileSoulView.empty(); }

    public static void resetForTests() { clear(); }
}
