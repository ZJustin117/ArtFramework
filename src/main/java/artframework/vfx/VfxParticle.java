package artframework.vfx;

/** One immutable CPU particle value. */
public final class VfxParticle {
    public final int spawnIndex;
    public final float age, lifetime;
    public final VfxVec2 position, velocity;
    public final float rotationDegrees, angularVelocity, baseScale, scale, alpha;
    public final VfxColor color;

    public VfxParticle(int spawnIndex, float age, float lifetime, VfxVec2 position,
            VfxVec2 velocity, float rotationDegrees, float angularVelocity,
            float baseScale, float scale, float alpha, VfxColor color) {
        this.spawnIndex = spawnIndex;
        this.age = age;
        this.lifetime = lifetime;
        this.position = position;
        this.velocity = velocity;
        this.rotationDegrees = rotationDegrees;
        this.angularVelocity = angularVelocity;
        this.baseScale = baseScale;
        this.scale = scale;
        this.alpha = alpha;
        this.color = color;
    }
}
