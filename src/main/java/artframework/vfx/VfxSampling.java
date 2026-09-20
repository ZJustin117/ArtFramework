package artframework.vfx;

import java.util.List;

/** Deterministic linear baseline samplers for converted curves and gradients. */
public final class VfxSampling {
    private VfxSampling() {}

    public static float curve(VfxCurveDefinition curve, float position, float fallback) {
        if (curve == null || curve.controlPoints.isEmpty()) return fallback;
        List<VfxCurveDefinition.VfxCurvePoint> points = curve.controlPoints;
        if (position <= points.get(0).position) return points.get(0).value;
        for (int i = 1; i < points.size(); i++) {
            VfxCurveDefinition.VfxCurvePoint right = points.get(i);
            VfxCurveDefinition.VfxCurvePoint left = points.get(i - 1);
            if (position <= right.position) {
                float width = right.position - left.position;
                float t = width <= 0f ? 1f : (position - left.position) / width;
                return left.value + (right.value - left.value) * t;
            }
        }
        return points.get(points.size() - 1).value;
    }

    public static VfxColor gradient(VfxGradientDefinition gradient, float position, VfxColor fallback) {
        if (gradient == null || gradient.stops.isEmpty()) return fallback;
        List<VfxGradientDefinition.VfxGradientStop> stops = gradient.stops;
        if (position <= stops.get(0).offset) return stops.get(0).color;
        for (int i = 1; i < stops.size(); i++) {
            VfxGradientDefinition.VfxGradientStop right = stops.get(i);
            VfxGradientDefinition.VfxGradientStop left = stops.get(i - 1);
            if (position <= right.offset) {
                float width = right.offset - left.offset;
                float t = width <= 0f ? 1f : (position - left.offset) / width;
                return new VfxColor(lerp(left.color.r, right.color.r, t),
                        lerp(left.color.g, right.color.g, t), lerp(left.color.b, right.color.b, t),
                        lerp(left.color.a, right.color.a, t));
            }
        }
        return stops.get(stops.size() - 1).color;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
