package artframework.sts1.backend;

import artframework.context.OrbStanceView;

/** Host-neutral current projection; native STS orb/stance renderers remain pixel authority. */
public final class Sts1OrbStanceProjection {
    private static volatile OrbStanceView current = OrbStanceView.empty();

    private Sts1OrbStanceProjection() {}

    public static OrbStanceView current() { return current; }

    public static void publish(OrbStanceView view) {
        current = view != null ? view : OrbStanceView.empty();
    }

    /** Clears the non-authoritative observation cache during panic/host recovery. */
    public static void clear() { current = OrbStanceView.empty(); }

    public static void resetForTests() { clear(); }
}
