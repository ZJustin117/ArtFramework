package artframework.core;

/** Data-only runtime selection for the project fallback present. */
public final class PresentSelectionComponent {
    public final String profileId;

    public PresentSelectionComponent(String profileId) {
        if (profileId == null || profileId.trim().isEmpty()) {
            throw new IllegalArgumentException("profileId required");
        }
        this.profileId = profileId;
    }
}
