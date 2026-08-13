package artframework.render;

import artframework.component.Rect;
import artframework.presentation.EffectAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable ECS data for the optional full-frame render surface. */
public final class FullFrameRenderComponent {
    public final Rect bounds;
    public final boolean enabled;
    private final List<EffectAttachment> effects;

    public FullFrameRenderComponent(Rect bounds, boolean enabled, List<EffectAttachment> effects) {
        this.bounds = bounds != null ? bounds : new Rect(0f, 0f, 0f, 0f);
        this.enabled = enabled;
        this.effects = effects == null || effects.isEmpty()
                ? Collections.<EffectAttachment>emptyList()
                : Collections.unmodifiableList(new ArrayList<EffectAttachment>(effects));
    }

    public List<EffectAttachment> effects() { return effects; }
}
