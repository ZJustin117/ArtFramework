package artframework.sts1.render;

/**
 * Default-off switch for the {@code vfx-stance-aura} per-instance claim.
 *
 * <p>While inactive (the default), the native effect pixels continue unchanged. Even when
 * active, a claim fires only for the exact supported classes {@link AuraClaimPolicy} recognizes
 * and only when {@link AuraArtRenderer} is ready; F2 supplies that real atlas draw. This slice
 * adds only the plumbing, so no pixel changes.
 */
public final class AuraDelegationGate {

    private static volatile boolean active = false;

    private AuraDelegationGate() {}

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean active) {
        AuraDelegationGate.active = active;
    }

    public static void resetForTests() {
        active = false;
    }
}
