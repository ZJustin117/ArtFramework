package artframework.sts1.backend;

import artframework.context.RelicPotionBlightView;

/** Host-neutral current projection; native STS remains the pixel authority. */
public final class Sts1RelicPotionBlightProjection {
    private static volatile RelicPotionBlightView current = RelicPotionBlightView.empty();

    private Sts1RelicPotionBlightProjection() {}

    public static RelicPotionBlightView current() { return current; }

    public static void publish(RelicPotionBlightView view) {
        current = view != null ? view : RelicPotionBlightView.empty();
    }

    /** Clears the non-authoritative observation cache during panic/host recovery. */
    public static void clear() { current = RelicPotionBlightView.empty(); }

    public static void resetForTests() { clear(); }
}
