package artframework.vfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VfxSceneDefinition {
    public final String id;
    public final int schemaVersion;
    public final float duration;
    public final List<VfxNodeDefinition> nodes;
    public final List<VfxResourceRef> resources;
    public final VfxCapability capability;

    public VfxSceneDefinition(String id, int schemaVersion, float duration,
            List<VfxNodeDefinition> nodes, List<VfxResourceRef> resources, VfxCapability capability) {
        this.id = id; this.schemaVersion = schemaVersion; this.duration = duration;
        this.nodes = Collections.unmodifiableList(new ArrayList<VfxNodeDefinition>(nodes));
        this.resources = Collections.unmodifiableList(new ArrayList<VfxResourceRef>(resources));
        this.capability = capability;
    }
}
