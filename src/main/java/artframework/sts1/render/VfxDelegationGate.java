package artframework.sts1.render;

/**
 * Default-off switch for the family-neutral per-instance transient-effect claim seam (current
 * members are the {@code vfx-stance-aura} FQNs).
 *
 * <p>While inactive (the default), the native effect pixels continue unchanged. Even when
 * active, a claim fires only for the exact supported classes {@link VfxClaimPolicy} recognizes
 * and only when {@link VfxArtRenderer} is ready; F2 supplies that real atlas draw. This slice
 * adds only the plumbing, so no pixel changes.
 */
public final class VfxDelegationGate {

    private static volatile boolean active = false;

    private VfxDelegationGate() {}

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean active) {
        VfxDelegationGate.active = active;
    }

    public static void resetForTests() {
        active = false;
    }
}
