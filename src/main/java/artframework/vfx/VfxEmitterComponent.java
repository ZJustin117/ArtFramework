package artframework.vfx;

/** Normalized immutable emitter definition used by the pure CPU runtime. */
public final class VfxEmitterComponent {
    /** Finite per-emitter allocation bound. Excess source amounts are consistently clamped. */
    public static final int MAX_PARTICLES = 4096;

    public final int amount;
    public final float lifetime, lifetimeRandomness, spreadDegrees;
    public final VfxVec2 direction, gravity;
    public final VfxRange initialVelocity, scale, angularVelocity;
    public final VfxCurveDefinition scaleCurve, alphaCurve;
    public final VfxGradientDefinition colorRamp;
    public final long randomSeed;
    public final VfxFlipbookDefinition flipbook;
    public final String blendMode, textureResourceId;

    private VfxEmitterComponent(int amount, float lifetime, float lifetimeRandomness,
            VfxVec2 direction, float spreadDegrees, VfxRange initialVelocity, VfxVec2 gravity,
            VfxRange scale, VfxCurveDefinition scaleCurve, VfxCurveDefinition alphaCurve,
            VfxGradientDefinition colorRamp, VfxRange angularVelocity, long randomSeed) {
        this(amount, lifetime, lifetimeRandomness, direction, spreadDegrees, initialVelocity, gravity,
                scale, scaleCurve, alphaCurve, colorRamp, angularVelocity, randomSeed, null, null, null);
    }

    private VfxEmitterComponent(int amount, float lifetime, float lifetimeRandomness,
            VfxVec2 direction, float spreadDegrees, VfxRange initialVelocity, VfxVec2 gravity,
            VfxRange scale, VfxCurveDefinition scaleCurve, VfxCurveDefinition alphaCurve,
            VfxGradientDefinition colorRamp, VfxRange angularVelocity, long randomSeed,
            VfxFlipbookDefinition flipbook, String blendMode, String textureResourceId) {
        this.amount = amount;
        this.lifetime = lifetime;
        this.lifetimeRandomness = lifetimeRandomness;
        this.direction = direction;
        this.spreadDegrees = spreadDegrees;
        this.initialVelocity = initialVelocity;
        this.gravity = gravity;
        this.scale = scale;
        this.scaleCurve = scaleCurve;
        this.alphaCurve = alphaCurve;
        this.colorRamp = colorRamp;
        this.angularVelocity = angularVelocity;
        this.randomSeed = randomSeed;
        this.flipbook = flipbook;
        this.blendMode = blendMode;
        this.textureResourceId = textureResourceId;
    }

    public static VfxEmitterComponent from(ParticleEmitterDefinition value) {
        int amount = value.amount == null ? 0 : Math.max(0, Math.min(MAX_PARTICLES, value.amount));
        float lifetime = positive(value.lifetime, 1f);
        float randomness = clamp(value.lifetimeRandomness == null ? 0f : value.lifetimeRandomness, 0f, 1f);
        return new VfxEmitterComponent(amount, lifetime, randomness,
                value.direction == null ? new VfxVec2(1f, 0f) : value.direction,
                Math.max(0f, value.spreadDegrees == null ? 0f : value.spreadDegrees),
                range(value.initialVelocity, 0f), value.gravity == null ? new VfxVec2(0f, 0f) : value.gravity,
                range(value.scale, 1f), value.scaleCurve, value.alphaCurve, value.colorRamp,
                range(value.angularVelocity, 0f), value.randomSeed, value.flipbook,
                value.blendMode, value.textureResourceId);
    }

    private static float positive(Float value, float fallback) {
        return value == null || value <= 0f ? fallback : value;
    }

    private static VfxRange range(VfxRange value, float fallback) {
        if (value == null) return new VfxRange(fallback, fallback);
        return value.min <= value.max ? value : new VfxRange(value.max, value.min);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
