package artframework.vfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable bounded particle buffer; systems replace this component each tick. */
public final class VfxParticleBufferComponent {
    public final List<VfxParticle> particles;

    public VfxParticleBufferComponent(List<VfxParticle> particles) {
        if (particles == null || particles.size() > VfxEmitterComponent.MAX_PARTICLES) {
            throw new IllegalArgumentException("particle buffer exceeds finite capacity");
        }
        this.particles = Collections.unmodifiableList(new ArrayList<VfxParticle>(particles));
    }

    public static VfxParticleBufferComponent empty() {
        return new VfxParticleBufferComponent(Collections.<VfxParticle>emptyList());
    }
}
