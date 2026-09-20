package artframework.vfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VfxGradientDefinition {
    public final List<VfxGradientStop> stops;

    public VfxGradientDefinition(List<VfxGradientStop> stops) {
        this.stops = Collections.unmodifiableList(new ArrayList<VfxGradientStop>(stops));
    }

    public static final class VfxGradientStop {
        public final float offset;
        public final VfxColor color;

        public VfxGradientStop(float offset, VfxColor color) {
            this.offset = offset; this.color = color;
        }
    }
}
