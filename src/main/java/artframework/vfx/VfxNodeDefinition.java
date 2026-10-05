package artframework.vfx;

public final class VfxNodeDefinition {
    /** The single recognized restricted sub-emitter trigger; any other value is treated as none. */
    public static final String TRIGGER_ON_PARENT_COMPLETE = "onParentComplete";

    public final String id, nodePath, parentId, nodeType;
    public final VfxVec2 position, scale;
    public final Float rotationDegrees, zIndex;
    public final Boolean visible;
    public final VfxColor modulate, selfModulate;
    public final ParticleEmitterDefinition particleEmitter;
    /**
     * Optional restricted sub-emitter trigger. Empty means the node emits immediately (no trigger).
     * Only {@link #TRIGGER_ON_PARENT_COMPLETE} is recognized; unknown values are fail-open to empty.
     */
    public final String emissionTrigger;

    public VfxNodeDefinition(String id, String nodePath, String parentId, String nodeType,
            VfxVec2 position, VfxVec2 scale, Float rotationDegrees, Float zIndex, Boolean visible,
            VfxColor modulate, VfxColor selfModulate, ParticleEmitterDefinition particleEmitter) {
        this(id, nodePath, parentId, nodeType, position, scale, rotationDegrees, zIndex, visible,
                modulate, selfModulate, particleEmitter, "");
    }

    public VfxNodeDefinition(String id, String nodePath, String parentId, String nodeType,
            VfxVec2 position, VfxVec2 scale, Float rotationDegrees, Float zIndex, Boolean visible,
            VfxColor modulate, VfxColor selfModulate, ParticleEmitterDefinition particleEmitter,
            String emissionTrigger) {
        this.id = id; this.nodePath = nodePath; this.parentId = parentId; this.nodeType = nodeType;
        this.position = position; this.scale = scale; this.rotationDegrees = rotationDegrees;
        this.zIndex = zIndex; this.visible = visible; this.modulate = modulate;
        this.selfModulate = selfModulate; this.particleEmitter = particleEmitter;
        // Fail-open: only the one recognized trigger is retained; absent/unknown collapses to empty.
        this.emissionTrigger = TRIGGER_ON_PARENT_COMPLETE.equals(emissionTrigger)
                ? TRIGGER_ON_PARENT_COMPLETE : "";
    }
}
