package artframework.core;

/**
 * Present profile contribution on a node (from {@code art.present_profile} or root props sugar).
 */
public final class PresentBinding {

    public final String profileId;
    public final PresentMode mode;

    public PresentBinding(String profileId, PresentMode mode) {
        if (profileId == null || profileId.isEmpty()) {
            throw new IllegalArgumentException("profileId required");
        }
        this.profileId = profileId;
        this.mode = mode != null ? mode : PresentMode.OVERRIDE;
    }

    public PresentProfile resolveResource() {
        return PresentProfiles.get(profileId);
    }
}
