package artframework.core;

/** Data-only profile selection for one C2 surface. */
public final class SurfacePresentComponent {
    public final String surfaceId;
    public final String profileId;

    public SurfacePresentComponent(String surfaceId, String profileId) {
        if (surfaceId == null || surfaceId.trim().isEmpty()) {
            throw new IllegalArgumentException("surfaceId required");
        }
        if (profileId == null || profileId.trim().isEmpty()) {
            throw new IllegalArgumentException("profileId required");
        }
        this.surfaceId = surfaceId;
        this.profileId = profileId;
    }
}
