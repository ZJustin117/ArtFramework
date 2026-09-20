package artframework.vfx;

/** Immutable local transform state with explicit loader-null defaults. */
public final class VfxTransformComponent {
    public final VfxVec2 position;
    public final VfxVec2 scale;
    public final float rotationDegrees;
    public final float zIndex;
    public final boolean visible;
    public final VfxColor color;

    public VfxTransformComponent(VfxVec2 position, VfxVec2 scale, float rotationDegrees,
            float zIndex, boolean visible) {
        this(position, scale, rotationDegrees, zIndex, visible, new VfxColor(1f, 1f, 1f, 1f));
    }

    public VfxTransformComponent(VfxVec2 position, VfxVec2 scale, float rotationDegrees,
            float zIndex, boolean visible, VfxColor color) {
        this.position = position;
        this.scale = scale;
        this.rotationDegrees = rotationDegrees;
        this.zIndex = zIndex;
        this.visible = visible;
        this.color = color == null ? new VfxColor(1f, 1f, 1f, 1f) : color;
    }

    public static VfxTransformComponent from(VfxNodeDefinition node) {
        VfxColor modulate = node.modulate == null ? new VfxColor(1f, 1f, 1f, 1f) : node.modulate;
        VfxColor self = node.selfModulate == null ? new VfxColor(1f, 1f, 1f, 1f) : node.selfModulate;
        return new VfxTransformComponent(node.position == null ? new VfxVec2(0f, 0f) : node.position,
                node.scale == null ? new VfxVec2(1f, 1f) : node.scale,
                node.rotationDegrees == null ? 0f : node.rotationDegrees,
                node.zIndex == null ? 0f : node.zIndex,
                node.visible == null || node.visible,
                new VfxColor(modulate.r * self.r, modulate.g * self.g, modulate.b * self.b,
                        modulate.a * self.a));
    }
}
