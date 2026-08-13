package artframework.presentation;

import artframework.component.Rect;
import artframework.ecs.EntityId;
import java.util.Collections;
import java.util.List;

/** Immutable visual item consumed by C1/C2 render adapters. */
public final class PresentationDrawItem {
    public final EntityId entity;
    public final PresentationKey key;
    public final Rect bounds;
    public final float z;
    public final DrawComponent draw;
    public final ChromeComponent chrome;
    public final List<EffectAttachment> effects;
    public final HostBindingComponent hostBinding;
    public final boolean root;

    PresentationDrawItem(EntityId entity, PresentationKey key, Rect bounds, float z,
            DrawComponent draw, ChromeComponent chrome, List<EffectAttachment> effects,
            HostBindingComponent hostBinding, boolean root) {
        this.entity = entity;
        this.key = key;
        this.bounds = bounds;
        this.z = z;
        this.draw = draw;
        this.chrome = chrome;
        this.effects = Collections.unmodifiableList(effects);
        this.hostBinding = hostBinding;
        this.root = root;
    }
}
