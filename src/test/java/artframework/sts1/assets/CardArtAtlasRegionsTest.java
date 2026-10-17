package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * U04. The native card PORTRAIT page is {@code cards/cards.atlas}, whose region names ARE the
 * native card art urls ({@code AbstractCard.assetUrl}, for example {@code red/attack/strike}).
 *
 * <p>This test drives the existing {@link Sts1AtlasMaterializer#regionFromAtlas(String, String)}
 * live seam with a fake {@link Sts1AtlasMaterializer.LiveAtlasSource} exposing a {@code cards}
 * atlas, so the portrait descriptor {@code cards#red/attack/strike} resolves exactly the way the
 * {@code cardui} frame/banner/orb descriptors already do. No GL: the page texture is an
 * allocated {@code Texture} with a headless {@link TextureData}.
 */
public class CardArtAtlasRegionsTest {

    @After
    public void restoreMaterializer() {
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.clearCache();
    }

    /**
     * The native art url {@code red/attack/strike} resolves to a real region on the {@code cards}
     * page: the texture is the page texture, the name is the art url verbatim, and the packed
     * bounds are the native atlas entry ({@code xy: 997, 375 / size: 248, 186}).
     */
    @Test
    public void cardArtRegionResolvesFromCardsAtlas() {
        installCardsAtlas();

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.regionFromAtlas("cards", "red/attack/strike");

        assertNotNull("the native card art region must resolve", resolved);
        assertNotNull(resolved.texture);
        AtlasRegion region = resolved.region;
        assertNotNull(region);
        assertEquals("the region name is the native art url", "red/attack/strike", region.name);
        assertEquals("the packed portrait width is the native atlas size", 248, region.width);
        assertEquals("the packed portrait height is the native atlas size", 186, region.height);
        assertEquals("the native cards page is 2048px", 2048, region.pageWidth);
        assertEquals(2048, region.pageHeight);
        assertEquals("the native untrimmed portrait is 250x190", 250, region.originalWidth);
        assertEquals(190, region.originalHeight);
        assertTrue("the packed region must be valid: " + region.name, region.valid());
    }

    /**
     * A missing region name on the same {@code cards} atlas yields {@code null} (fail-open: no
     * portrait draw rather than a bogus one).
     */
    @Test
    public void missingCardArtRegionResolvesToNull() {
        installCardsAtlas();

        assertNull(Sts1AtlasMaterializer.regionFromAtlas("cards", "red/attack/not_a_real_card"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("cards", ""));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas(null, "red/attack/strike"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("", "red/attack/strike"));
    }

    /**
     * Without an installed live source the portrait key fails open: off-game there is no
     * {@code cards} page, and the lookup must return {@code null} instead of throwing.
     */
    @Test
    public void noLiveSourceYieldsNullForCardArt() {
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("cards", "red/attack/strike"));
    }

    /** Installs a fake live source exposing a {@code cards} page with the native strike region. */
    private static void installCardsAtlas() {
        Texture page = noGlTexture(2048, 2048);
        TextureAtlas cards = new TextureAtlas();
        TextureAtlas.AtlasRegion region =
                cards.addRegion("red/attack/strike", page, 997, 375, 248, 186);
        region.originalWidth = 250;
        region.originalHeight = 190;
        region.offsetX = 1f;
        region.offsetY = 4f;
        region.rotate = false;

        StubLiveAtlasSource source = new StubLiveAtlasSource();
        source.atlases.put("cards", cards);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);
    }

    private static Texture noGlTexture(final int width, final int height) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Texture texture =
                    (Texture) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(Texture.class);
            Field dataField = Texture.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(texture, new TextureData() {
                public TextureDataType getType() { return TextureDataType.Pixmap; }
                public boolean isPrepared() { return true; }
                public void prepare() {}
                public Pixmap consumePixmap() { return null; }
                public boolean disposePixmap() { return false; }
                public void consumeCustomData(int target) {}
                public int getWidth() { return width; }
                public int getHeight() { return height; }
                public Pixmap.Format getFormat() { return Pixmap.Format.RGBA8888; }
                public boolean useMipMaps() { return false; }
                public boolean isManaged() { return false; }
            });
            return texture;
        } catch (Exception failure) {
            throw new AssertionError("could not create no-GL texture test double", failure);
        }
    }

    private static final class StubLiveAtlasSource implements Sts1AtlasMaterializer.LiveAtlasSource {
        final Map<String, TextureAtlas> atlases = new LinkedHashMap<String, TextureAtlas>();

        public TextureAtlas atlas(String atlasKey) {
            return atlases.get(atlasKey);
        }
    }
}
