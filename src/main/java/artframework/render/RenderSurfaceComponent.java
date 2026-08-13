package artframework.render;

import artframework.component.Rect;
import artframework.presentation.EffectAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable ECS data for one C2 surface render region and its ambient effects. */
public final class RenderSurfaceComponent {
    public final String surfaceId;
    public final Rect bounds;
    public final boolean enabled;
    public final float z;
    private final List<EffectAttachment> effects;

    public RenderSurfaceComponent(String surfaceId, Rect bounds, boolean enabled, float z,
            List<EffectAttachment> effects) {
        if (surfaceId == null || surfaceId.trim().isEmpty()) throw new IllegalArgumentException("surfaceId required");
        this.surfaceId = surfaceId.trim();
        this.bounds = bounds != null ? bounds : new Rect(0f, 0f, 0f, 0f);
        this.enabled = enabled;
        this.z = z;
        this.effects = effects == null || effects.isEmpty()
                ? Collections.<EffectAttachment>emptyList()
                : Collections.unmodifiableList(new ArrayList<EffectAttachment>(effects));
    }

    public List<EffectAttachment> effects() { return effects; }
}
