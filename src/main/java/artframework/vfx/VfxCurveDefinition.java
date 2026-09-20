package artframework.vfx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VfxCurveDefinition {
    public final List<VfxCurvePoint> controlPoints;
    public final List<Float> limits;

    public VfxCurveDefinition(List<VfxCurvePoint> controlPoints, List<Float> limits) {
        this.controlPoints = Collections.unmodifiableList(new ArrayList<VfxCurvePoint>(controlPoints));
        this.limits = limits == null ? Collections.<Float>emptyList()
                : Collections.unmodifiableList(new ArrayList<Float>(limits));
    }

    public static final class VfxCurvePoint {
        public final float position, value, leftTangent, rightTangent;
        public final int leftMode, rightMode;

        public VfxCurvePoint(float position, float value, float leftTangent, float rightTangent,
                             int leftMode, int rightMode) {
            this.position = position; this.value = value;
            this.leftTangent = leftTangent; this.rightTangent = rightTangent;
            this.leftMode = leftMode; this.rightMode = rightMode;
        }
    }
}
