package artframework.skeleton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default and pair-specific Spine animation mix durations.
 */
public final class SkeletonMixTable {

    private final float defaultMix;
    private final Map<String, Float> mixes;

    public SkeletonMixTable(float defaultMix, Map<String, Float> mixes) {
        this.defaultMix = defaultMix;
        this.mixes =
                mixes == null
                        ? Collections.<String, Float>emptyMap()
                        : Collections.unmodifiableMap(new LinkedHashMap<String, Float>(mixes));
    }

    public static Builder builder(float defaultMix) {
        return new Builder(defaultMix);
    }

    public float defaultMix() {
        return defaultMix;
    }

    public float mix(String from, String to) {
        Float value = mixes.get(key(from, to));
        return value == null ? defaultMix : value.floatValue();
    }

    public Map<String, Float> mixes() {
        return mixes;
    }

    public static String key(String from, String to) {
        return (from != null ? from : "") + "->" + (to != null ? to : "");
    }

    public static final class Builder {
        private final float defaultMix;
        private final Map<String, Float> mixes = new LinkedHashMap<String, Float>();

        private Builder(float defaultMix) {
            this.defaultMix = defaultMix;
        }

        public Builder mix(String from, String to, float seconds) {
            mixes.put(key(from, to), Float.valueOf(seconds));
            return this;
        }

        public SkeletonMixTable build() {
            return new SkeletonMixTable(defaultMix, mixes);
        }
    }
}
