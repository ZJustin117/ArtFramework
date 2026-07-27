package artframework.skeleton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Declarative skeleton asset reference (no format-specific types).
 */
public final class SkeletonSource {

    public final String skeletonId;
    public final String atlasPath;
    public final String skeletonPath;
    public final Map<String, Object> params;

    public SkeletonSource(
            String skeletonId, String atlasPath, String skeletonPath, Map<String, Object> params) {
        if (skeletonId == null || skeletonId.isEmpty()) {
            throw new IllegalArgumentException("skeletonId required");
        }
        this.skeletonId = skeletonId;
        this.atlasPath = atlasPath != null ? atlasPath : "";
        this.skeletonPath = skeletonPath != null ? skeletonPath : "";
        if (params == null || params.isEmpty()) {
            this.params = Collections.emptyMap();
        } else {
            this.params = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(params));
        }
    }
}
