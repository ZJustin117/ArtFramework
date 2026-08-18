package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable surface-profile binding contribution installed by one enabled PresentPack. */
public final class PackSurfaceBindingsComponent {
    public final String packId;
    public final String profileId;
    private final List<String> surfaceIds;

    public PackSurfaceBindingsComponent(String packId, String profileId, List<String> surfaceIds) {
        if (packId == null || packId.isEmpty()) throw new IllegalArgumentException("pack id required");
        if (profileId == null || profileId.trim().isEmpty()) {
            throw new IllegalArgumentException("profile id required");
        }
        this.packId = packId;
        this.profileId = profileId.trim();
        this.surfaceIds = surfaceIds == null || surfaceIds.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(surfaceIds));
    }

    public List<String> surfaceIds() {
        return surfaceIds;
    }
}
