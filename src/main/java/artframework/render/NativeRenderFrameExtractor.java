package artframework.render;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stateless extraction of native render inputs into a deterministic immutable frame snapshot. */
public final class NativeRenderFrameExtractor {
    public NativeRenderFrameSnapshot extract(PresentationWorld world) {
        if (world == null) throw new IllegalArgumentException("world required");

        List<NativeRenderInputSnapshot> snapshots = new ArrayList<NativeRenderInputSnapshot>();
        Set<String> stableKeys = new HashSet<String>();
        for (EntityId id : world.query(NativeRenderInputComponent.class)) {
            NativeRenderInputComponent input = world.get(id, NativeRenderInputComponent.class);
            if (!stableKeys.add(input.stableKey())) {
                throw new IllegalStateException("duplicate native render stable key: "
                        + input.stableKey());
            }
            snapshots.add(new NativeRenderInputSnapshot(input));
        }
        Collections.sort(snapshots, (a, b) -> RenderOrder.COMPARATOR.compare(a.order(), b.order()));
        return new NativeRenderFrameSnapshot(snapshots);
    }
}
