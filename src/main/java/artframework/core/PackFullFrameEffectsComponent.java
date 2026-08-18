package artframework.core;

import artframework.component.EffectDecl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable full-frame effect contribution installed by one enabled PresentPack. */
public final class PackFullFrameEffectsComponent {
    public final String packId;
    private final List<EffectDecl> effects;

    public PackFullFrameEffectsComponent(String packId, List<EffectDecl> effects) {
        if (packId == null || packId.isEmpty()) throw new IllegalArgumentException("pack id required");
        this.packId = packId;
        this.effects = effects == null || effects.isEmpty()
                ? Collections.<EffectDecl>emptyList()
                : Collections.unmodifiableList(new ArrayList<EffectDecl>(effects));
    }

    public List<EffectDecl> effects() {
        return effects;
    }
}
