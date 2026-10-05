package artframework.vfx;

/** Normalized immutable emitter definition used by the pure CPU runtime. */
public final class VfxEmitterComponent {
    /** Finite per-emitter allocation bound. Excess source amounts are consistently clamped. */
    public static final int MAX_PARTICLES = 4096;
    /**
     * Finite bound for the optional restricted turbulence strength. This is a deliberately bounded
     * CPU approximation of STS2 turbulence, NOT runtime-equivalent. Zero is the default and disables
     * the turbulence term entirely.
     */
    public static final float MAX_TURBULENCE_STRENGTH = 100000f;

    public final int amount;
    public final float lifetime, lifetimeRandomness, spreadDegrees;
    public final VfxVec2 direction, gravity;
    public final VfxRange initialVelocity, scale, angularVelocity;
    public final VfxCurveDefinition scaleCurve, alphaCurve;
    public final VfxGradientDefinition colorRamp;
    public final long randomSeed;
    public final VfxFlipbookDefinition flipbook;
    public final String blendMode, textureResourceId;
    /**
     * Optional restricted turbulence strength (0 means disabled). Always finite and in
     * {@code [0, MAX_TURBULENCE_STRENGTH]}. The integrator only adds its term when this is positive,
     * so the default 0 keeps integration byte-identical to the gravity-only Euler path.
     */
    public final float turbulenceStrength;

    private VfxEmitterComponent(int amount, float lifetime, float lifetimeRandomness,
            VfxVec2 direction, float spreadDegrees, VfxRange initialVelocity, VfxVec2 gravity,
            VfxRange scale, VfxCurveDefinition scaleCurve, VfxCurveDefinition alphaCurve,
            VfxGradientDefinition colorRamp, VfxRange angularVelocity, long randomSeed,
            float turbulenceStrength) {
        this(amount, lifetime, lifetimeRandomness, direction, spreadDegrees, initialVelocity, gravity,
                scale, scaleCurve, alphaCurve, colorRamp, angularVelocity, randomSeed, null, null, null,
                turbulenceStrength);
    }

    private VfxEmitterComponent(int amount, float lifetime, float lifetimeRandomness,
            VfxVec2 direction, float spreadDegrees, VfxRange initialVelocity, VfxVec2 gravity,
            VfxRange scale, VfxCurveDefinition scaleCurve, VfxCurveDefinition alphaCurve,
            VfxGradientDefinition colorRamp, VfxRange angularVelocity, long randomSeed,
            VfxFlipbookDefinition flipbook, String blendMode, String textureResourceId,
            float turbulenceStrength) {
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
        this.turbulenceStrength = clampFinite(turbulenceStrength, 0f, MAX_TURBULENCE_STRENGTH);
    }

    public static VfxEmitterComponent from(ParticleEmitterDefinition value) {
        return from(value, 0f);
    }

    /**
     * Builder path carrying an optional restricted turbulence strength. Existing bundles keep the
     * default 0f through {@link #from(ParticleEmitterDefinition)}, so the turbulance term stays off.
     */
    public static VfxEmitterComponent from(ParticleEmitterDefinition value, float turbulenceStrength) {
        int amount = value.amount == null ? 0 : Math.max(0, Math.min(MAX_PARTICLES, value.amount));
        float lifetime = positive(value.lifetime, 1f);
        float randomness = clamp(value.lifetimeRandomness == null ? 0f : value.lifetimeRandomness, 0f, 1f);
        return new VfxEmitterComponent(amount, lifetime, randomness,
                value.direction == null ? new VfxVec2(1f, 0f) : value.direction,
                Math.max(0f, value.spreadDegrees == null ? 0f : value.spreadDegrees),
                range(value.initialVelocity, 0f), value.gravity == null ? new VfxVec2(0f, 0f) : value.gravity,
                range(value.scale, 1f), value.scaleCurve, value.alphaCurve, value.colorRamp,
                range(value.angularVelocity, 0f), value.randomSeed, value.flipbook,
                value.blendMode, value.textureResourceId, turbulence(turbulenceStrength));
    }

    private static float turbulence(Float value) {
        if (value == null) return 0f;
        return clampFinite(value, 0f, MAX_TURBULENCE_STRENGTH);
    }

    /** Clamps to [min, max], mapping NaN to min and any infinity to the nearest finite bound. */
    private static float clampFinite(float value, float min, float max) {
        if (Float.isNaN(value)) return min;
        if (value < min) return min;
        if (value > max) return max;
        return value;
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
