package artframework.sts1.assets;

/**
 * Host-side adapter from a live libGDX {@code TextureAtlas.AtlasRegion} to the host-neutral
 * {@link artframework.assets.AtlasRegion} descriptor.
 *
 * <p>This is the seam the {@code ImageMaster.vfxAtlas}/{@code vfxAtlas.findRegion(...)} families
 * need: STS1 hands out libGDX regions (for example {@code ImageMaster.EXHAUST_L}), and the
 * presentation graph consumes only the neutral value type. This class owns the small, exact
 * translation and nothing else — no caching, no provider wiring, no renderer changes.
 *
 * <p><strong>Rotation.</strong> libGDX bakes the rotation into the region at atlas load time: a
 * rotated entry {@code size: 64,48 rotate:true} at {@code xy: 10,20} is constructed as
 * {@code new AtlasRegion(texture, 10, 20, 48, 64)} with {@code rotate = true}. Consequently
 * {@link com.badlogic.gdx.graphics.g2d.TextureRegion#getRegionWidth() getRegionWidth()} /
 * {@code getRegionHeight()} already report the <em>packed page footprint</em> (the sampled rect),
 * for both rotated and unrotated regions, and the UVs
 * ({@code getU()/getV()/getU2()/getV2()}) describe that same packed rect. The visual size is the
 * separate pair {@code getRotatedPackedWidth()/getRotatedPackedHeight()}.
 *
 * <p>The neutral type uses the opposite convention: {@code width}/{@code height} are the packed
 * page bounds and {@link artframework.assets.AtlasRegion#displayWidth()} /
 * {@link artframework.assets.AtlasRegion#displayHeight()} swap them when {@code degrees != 0}.
 * This adapter therefore copies the gdx packed footprint through unchanged
 * ({@code width = getRegionWidth()}, {@code height = getRegionHeight()}, {@code degrees = rotate ?
 * 90 : 0}). The neutral swap then recovers the gdx visual size
 * ({@code displayWidth()/displayHeight() == getRotatedPackedWidth()/getRotatedPackedHeight()})
 * while {@link artframework.assets.AtlasRegion#uvRect()} matches the gdx UV box exactly. Feeding
 * the gdx <em>displayed</em> size in swapped order here would double-swap and corrupt the packed
 * rect.
 *
 * <p><strong>Trim metadata is display-oriented on both sides.</strong> libGDX
 * {@code originalWidth}/{@code originalHeight}/{@code offsetX}/{@code offsetY} describe the
 * untrimmed, display-oriented placement, exactly like the neutral fields, so they pass through
 * unchanged. Non-positive {@code originalWidth}/{@code originalHeight} are forwarded as {@code 0}
 * and the neutral constructor defaults them from the display dimensions. libGDX offsets are
 * {@code float}; the neutral fields are {@code int}, so integral host offsets pass exactly and a
 * fractional offset is truncated toward zero (residual: no host atlas writes fractional offsets).
 *
 * <p><strong>Page identity residual.</strong> libGDX {@code AtlasRegion} exposes no reliable
 * public page name or path, so the neutral {@code page} field cannot be filled from the region.
 * This adapter uses the empty string as a stable, deterministic placeholder; a host that needs a
 * real page key must supply it separately (for example from its own atlas/page table).
 *
 * <p><strong>No GL.</strong> Only {@code getTexture()}, {@code getRegionX()}/{@code getRegionY()},
 * {@code getRegionWidth()}/{@code getRegionHeight()} and public fields are read. No method that
 * binds or uploads a texture (for example {@code getTextureObjectHandle()}) is ever called, so the
 * adapter runs against a headless no-GL {@code Texture} double.
 *
 * <p><strong>Fail-open.</strong> {@code null} input or a region whose texture is {@code null}
 * yields {@code null}. A zero/unknown page size still produces a descriptor (whose
 * {@link artframework.assets.AtlasRegion#valid()} is false) rather than throwing.
 */
public final class Sts1GdxAtlasRegions {

    /**
     * Stable placeholder for {@link artframework.assets.AtlasRegion#page}: libGDX {@code
     * AtlasRegion} does not expose page identity (see the class-level residual note).
     */
    private static final String PAGE_LABEL = "";

    private Sts1GdxAtlasRegions() {}

    /**
     * Converts a libGDX atlas region to a neutral descriptor, or returns {@code null} when the
     * input is unusable (a {@code null} region or a region with a {@code null} texture).
     *
     * <p>The returned descriptor samples the same packed page pixels and reports the same displayed
     * size as {@code region}; see the class Javadoc for the rotation mapping. A non-positive page
     * size is preserved (the neutral {@code valid()} flag reports it) and never throws.
     */
    public static artframework.assets.AtlasRegion fromGdx(
            com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion region) {
        if (region == null) {
            return null;
        }
        com.badlogic.gdx.graphics.Texture texture = region.getTexture();
        if (texture == null) {
            return null;
        }
        int pageWidth = texture.getWidth();
        int pageHeight = texture.getHeight();
        boolean rotated = region.rotate;
        // libGDX getRegionWidth()/getRegionHeight() already carry the packed page footprint (the
        // rotation is baked in at atlas load), so copy them through unchanged. The neutral
        // displayWidth()/displayHeight() swap then recovers the gdx visual size.
        int packedWidth = region.getRegionWidth();
        int packedHeight = region.getRegionHeight();
        return new artframework.assets.AtlasRegion(
                PAGE_LABEL,
                region.name,
                region.getRegionX(),
                region.getRegionY(),
                packedWidth,
                packedHeight,
                pageWidth,
                pageHeight,
                region.originalWidth,
                region.originalHeight,
                (int) region.offsetX,
                (int) region.offsetY,
                rotated ? 90 : 0);
    }
}
