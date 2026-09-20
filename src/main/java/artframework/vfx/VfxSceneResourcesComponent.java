package artframework.vfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** ECS-owned resource metadata needed by render projection; no host asset objects are stored. */
public final class VfxSceneResourcesComponent {
    public final List<VfxResourceRef> resources;

    public VfxSceneResourcesComponent(List<VfxResourceRef> resources) {
        this.resources = Collections.unmodifiableList(new ArrayList<VfxResourceRef>(resources));
    }
}
