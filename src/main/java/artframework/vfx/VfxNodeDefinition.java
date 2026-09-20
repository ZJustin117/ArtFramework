package artframework.vfx;

public final class VfxNodeDefinition {
    public final String id, nodePath, parentId, nodeType;
    public final VfxVec2 position, scale;
    public final Float rotationDegrees, zIndex;
    public final Boolean visible;
    public final VfxColor modulate, selfModulate;
    public final ParticleEmitterDefinition particleEmitter;

    public VfxNodeDefinition(String id, String nodePath, String parentId, String nodeType,
            VfxVec2 position, VfxVec2 scale, Float rotationDegrees, Float zIndex, Boolean visible,
            VfxColor modulate, VfxColor selfModulate, ParticleEmitterDefinition particleEmitter) {
        this.id = id; this.nodePath = nodePath; this.parentId = parentId; this.nodeType = nodeType;
        this.position = position; this.scale = scale; this.rotationDegrees = rotationDegrees;
        this.zIndex = zIndex; this.visible = visible; this.modulate = modulate;
        this.selfModulate = selfModulate; this.particleEmitter = particleEmitter;
    }
}
