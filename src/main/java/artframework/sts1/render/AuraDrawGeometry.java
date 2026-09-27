package artframework.sts1.render;

/**
 * Pure draw geometry for one claimed {@code vfx-stance-aura} effect, mirroring the native render
 * formula exactly.
 *
 * <p>This class is host-neutral data: it performs no GL work, holds no host handles, and applies no
 * color/blend/UV state. For every claimable aura the native render method performs
 * {@code setColor(color)} then {@code setBlendFunction(770, 1)} (additive) before
 * {@code SpriteBatch.draw(...)} and restores {@code setBlendFunction(770, 771)} afterwards. The
 * host draw owns that color/blend/UV (and the region's UV rect); this mapping only resolves the
 * positional/scale/rotation arguments the batch receives, with the native center origin
 * {@code (packedWidth / 2, packedHeight / 2)} and the identity width/height
 * {@code (packedWidth, packedHeight)}.
 *
 * <p>The per-kind formulas mirrored here are:
 *
 * <pre>
 *   StanceAuraEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   WrathParticleEffect.render:
 *     sb.draw(img, x, y + vY, pw/2f, ph/2f, pw, ph,
 *             scale*0.8f, (0.1f + ((dur_div2*2f - duration)*2f*scale)) * Settings.scale, rotation)
 *   DivinityParticleEffect.render:
 *     sb.draw(img, x, y + vY, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   CalmParticleEffect.render:
 *     sb.draw(ImageMaster.FROST_ACTIVATE_VFX_1, x, y, 32f, 32f, 25f, 128f,
 *             scale, scale + (dur_div2*0.4f - duration) * Settings.scale, rotation,
 *             0, 0, 64, 64, false, false)
 * </pre>
 *
 * where {@code pw}/{@code ph} are the region's {@code packedWidth}/{@code packedHeight}. Calm is the
 * one kind that draws a bare {@code Texture}, so its native origin/size/source rect are fixed
 * constants and the packed region size is ignored.
 */
public final class AuraDrawGeometry {

    /** The claimable aura draw formulas. */
    public enum Kind {
        STANCE_AURA,
        WRATH_PARTICLE,
        DIVINITY_PARTICLE,
        CALM_PARTICLE
    }

    // Native CalmParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the FROST_ACTIVATE_VFX_1 texture.
    /** Native Calm draw origin x ({@code 32f}). */
    public static final float CALM_ORIGIN_X = 32f;
    /** Native Calm draw origin y ({@code 32f}). */
    public static final float CALM_ORIGIN_Y = 32f;
    /** Native Calm draw width ({@code 25f}). */
    public static final float CALM_WIDTH = 25f;
    /** Native Calm draw height ({@code 128f}). */
    public static final float CALM_HEIGHT = 128f;
    /** Native Calm draw source rect x ({@code 0}). */
    public static final int CALM_SRC_X = 0;
    /** Native Calm draw source rect y ({@code 0}). */
    public static final int CALM_SRC_Y = 0;
    /** Native Calm draw source rect width ({@code 64}). */
    public static final int CALM_SRC_W = 64;
    /** Native Calm draw source rect height ({@code 64}). */
    public static final int CALM_SRC_H = 64;

    /** Resolved draw arguments; all finite, origin is the native center origin. */
    public static final class Params {
        public final float x;
        public final float y;
        public final float originX;
        public final float originY;
        public final float width;
        public final float height;
        public final float scaleX;
        public final float scaleY;
        public final float rotation;

        public Params(float x, float y, float originX, float originY, float width, float height,
                float scaleX, float scaleY, float rotation) {
            this.x = x;
            this.y = y;
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.rotation = rotation;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Params)) return false;
            Params p = (Params) other;
            return Float.compare(x, p.x) == 0
                    && Float.compare(y, p.y) == 0
                    && Float.compare(originX, p.originX) == 0
                    && Float.compare(originY, p.originY) == 0
                    && Float.compare(width, p.width) == 0
                    && Float.compare(height, p.height) == 0
                    && Float.compare(scaleX, p.scaleX) == 0
                    && Float.compare(scaleY, p.scaleY) == 0
                    && Float.compare(rotation, p.rotation) == 0;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(originX);
            result = 31 * result + Float.floatToIntBits(originY);
            result = 31 * result + Float.floatToIntBits(width);
            result = 31 * result + Float.floatToIntBits(height);
            result = 31 * result + Float.floatToIntBits(scaleX);
            result = 31 * result + Float.floatToIntBits(scaleY);
            result = 31 * result + Float.floatToIntBits(rotation);
            return result;
        }

        @Override
        public String toString() {
            return "Params{x=" + x + ", y=" + y + ", originX=" + originX + ", originY=" + originY
                    + ", width=" + width + ", height=" + height + ", scaleX=" + scaleX
                    + ", scaleY=" + scaleY + ", rotation=" + rotation + '}';
        }
    }

    private AuraDrawGeometry() {}

    /**
     * FQN -&gt; {@link Kind}, or {@code null} when the class is not a claimable aura (fail-open).
     * Matches only the exact FQNs owned by {@link AuraClaimPolicy}; null, blank, near-misses
     * ({@code ...StanceAuraEffect2}), and nested ({@code ...StanceAuraEffect$Sub}) fail open.
     */
    public static Kind kindFor(String nativeClassName) {
        if (nativeClassName == null) return null;
        String value = nativeClassName.trim();
        if (value.isEmpty()) return null;
        if (AuraClaimPolicy.STANCE_AURA_EFFECT.equals(value)) return Kind.STANCE_AURA;
        if (AuraClaimPolicy.WRATH_PARTICLE_EFFECT.equals(value)) return Kind.WRATH_PARTICLE;
        if (AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT.equals(value)) return Kind.DIVINITY_PARTICLE;
        if (AuraClaimPolicy.CALM_PARTICLE_EFFECT.equals(value)) return Kind.CALM_PARTICLE;
        return null;
    }

    /**
     * Pure geometry for one aura draw. The caller supplies the effect field floats and the packed
     * region size; color/blend/UV are applied by the host draw (additive 770/1, restored to
     * 770/771).
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static Params params(Kind kind, float x, float y, float vY, float scale, float rotation,
            float durDiv2, float duration, float settingsScale,
            float packedWidth, float packedHeight) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        float originX = packedWidth / 2f;
        float originY = packedHeight / 2f;
        switch (kind) {
            case STANCE_AURA:
                return new Params(x, y, originX, originY, packedWidth, packedHeight,
                        scale, scale, rotation);
            case DIVINITY_PARTICLE:
                return new Params(x, y + vY, originX, originY, packedWidth, packedHeight,
                        scale, scale, rotation);
            case WRATH_PARTICLE: {
                float scaleX = scale * 0.8f;
                float scaleY = (0.1f + ((durDiv2 * 2f - duration) * 2f * scale)) * settingsScale;
                return new Params(x, y + vY, originX, originY, packedWidth, packedHeight,
                        scaleX, scaleY, rotation);
            }
            case CALM_PARTICLE: {
                // Native Calm ignores the (absent) region: fixed origin/size and scaleY formula;
                // packedWidth/packedHeight and vY are unused.
                float scaleY = scale + (durDiv2 * 0.4f - duration) * settingsScale;
                return new Params(x, y, CALM_ORIGIN_X, CALM_ORIGIN_Y, CALM_WIDTH, CALM_HEIGHT,
                        scale, scaleY, rotation);
            }
            default:
                throw new IllegalArgumentException("unhandled kind: " + kind);
        }
    }
}
