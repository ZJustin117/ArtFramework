package artframework.vfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable deterministic projection published into the ECS. */
public final class VfxDrawList {
    public final List<VfxParticleDraw> draws;

    public VfxDrawList(List<VfxParticleDraw> draws) {
        this.draws = Collections.unmodifiableList(new ArrayList<VfxParticleDraw>(draws));
    }

    public static VfxDrawList empty() { return new VfxDrawList(Collections.<VfxParticleDraw>emptyList()); }
}
