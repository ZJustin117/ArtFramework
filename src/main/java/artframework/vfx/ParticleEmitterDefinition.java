package artframework.vfx;

public final class ParticleEmitterDefinition {
    public final Integer amount;
    public final Float lifetime, lifetimeRandomness, spreadDegrees;
    public final VfxVec2 direction, gravity;
    public final VfxRange initialVelocity, scale, angularVelocity;
    public final VfxCurveDefinition scaleCurve, alphaCurve;
    public final VfxGradientDefinition colorRamp;
    public final VfxFlipbookDefinition flipbook;
    public final String blendMode;
    public final String textureResourceId;
    public final long randomSeed;

    public ParticleEmitterDefinition(Integer amount, Float lifetime, Float lifetimeRandomness,
            VfxVec2 direction, Float spreadDegrees, VfxRange initialVelocity, VfxVec2 gravity,
            VfxRange scale, VfxCurveDefinition scaleCurve, VfxCurveDefinition alphaCurve,
            VfxGradientDefinition colorRamp, VfxRange angularVelocity,
            VfxFlipbookDefinition flipbook, String blendMode, Long randomSeed) {
        this(amount, lifetime, lifetimeRandomness, direction, spreadDegrees, initialVelocity, gravity,
                scale, scaleCurve, alphaCurve, colorRamp, angularVelocity, flipbook, blendMode,
                randomSeed, null);
    }

    public ParticleEmitterDefinition(Integer amount, Float lifetime, Float lifetimeRandomness,
            VfxVec2 direction, Float spreadDegrees, VfxRange initialVelocity, VfxVec2 gravity,
            VfxRange scale, VfxCurveDefinition scaleCurve, VfxCurveDefinition alphaCurve,
            VfxGradientDefinition colorRamp, VfxRange angularVelocity,
            VfxFlipbookDefinition flipbook, String blendMode, Long randomSeed,
            String textureResourceId) {
        this.amount = amount; this.lifetime = lifetime; this.lifetimeRandomness = lifetimeRandomness;
        this.direction = direction; this.spreadDegrees = spreadDegrees; this.initialVelocity = initialVelocity;
        this.gravity = gravity; this.scale = scale; this.scaleCurve = scaleCurve; this.alphaCurve = alphaCurve;
        this.colorRamp = colorRamp; this.angularVelocity = angularVelocity; this.flipbook = flipbook;
        this.blendMode = blendMode; this.randomSeed = randomSeed == null ? 0L : randomSeed;
        this.textureResourceId = textureResourceId;
    }
}
