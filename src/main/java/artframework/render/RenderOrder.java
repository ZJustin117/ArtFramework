package artframework.render;

import java.util.Comparator;

/** Immutable deterministic ordering key for one render item. */
public final class RenderOrder {
    public static final Comparator<RenderOrder> COMPARATOR = new Comparator<RenderOrder>() {
        @Override public int compare(RenderOrder a, RenderOrder b) {
            int result = Integer.compare(a.phase.rank, b.phase.rank);
            if (result != 0) return result;
            result = Float.compare(a.z, b.z);
            if (result != 0) return result;
            return a.stableKey.compareTo(b.stableKey);
        }
    };

    public final RenderPhase phase;
    public final float z;
    public final String stableKey;

    public RenderOrder(RenderPhase phase, float z, String stableKey) {
        if (phase == null) throw new IllegalArgumentException("render phase required");
        if (Float.isNaN(z) || Float.isInfinite(z)) {
            throw new IllegalArgumentException("render z must be finite");
        }
        if (stableKey == null || stableKey.isEmpty()) {
            throw new IllegalArgumentException("render stable key required");
        }
        this.phase = phase;
        this.z = z;
        this.stableKey = stableKey;
    }
}
