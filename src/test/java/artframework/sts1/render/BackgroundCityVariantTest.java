package artframework.sts1.render;

import artframework.assets.AtlasRegion;
import artframework.sts1.assets.Sts1AtlasMaterializer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * E07 Red: the {@code city} background variant draws the ACT-2 {@code TheCityScene} combat
 * background layers as real native atlas regions from the pre-native Prefix.
 *
 * <p>The native {@code TheCityScene.renderCombatRoomBg} order is authoritative (decompile):
 * {@code bg}, {@code bgGlow}, the {@code darkDay} double {@code bgGlowBlur}, the alt {@code bg2}
 * pass, {@code floor}, {@code ceiling}, the flagged {@code wall}/{@code chains} stack, the animated
 * {@code chainsGlow} double, the {@code mg} pass with its {@code mgGlow} double, the alt {@code mg2}
 * pass, the {@code pillarConfig} pillars, then the {@code throne} plus additive {@code throneGlow}.
 * Every region is submitted through the PIXEL-RECT 16-arg libGDX overload
 * ({@code originX/originY == 0}, packed size, page source rect) — never a UV blit. Tints and blend
 * transitions are carried exactly as the native {@code setColor}/{@code setBlendFunction} sequence.
 *
 * <p>Non-tautology: the plan assertions are independent of the draw, the tint/blend assertions are
 * derived from the native setColor sequence, and the real draw is proven NOT to be the synthetic
 * single-quad path (which cannot own a scene-scoped variant).
 */
public class BackgroundCityVariantTest {

    private float previousScale;
    private float previousXScale;
    private float previousYScale;
    private boolean previousFourByThree;
    private boolean previousLetterbox;
    private Object previousWhiteSquare;

    @Before
    public void setUp() throws Exception {
        previousScale = Settings.scale;
        previousXScale = Settings.xScale;
        previousYScale = Settings.yScale;
        previousFourByThree = Settings.isFourByThree;
        previousLetterbox = Settings.isLetterbox;
        Settings.scale = 1f;
        Settings.xScale = 1f;
        Settings.yScale = 1f;
        Settings.isFourByThree = false;
        Settings.isLetterbox = false;
        previousWhiteSquare = whiteSquareField().get(null);
    }

    @After
    public void tearDown() throws Exception {
        Settings.scale = previousScale;
        Settings.xScale = previousXScale;
        Settings.yScale = previousYScale;
        Settings.isFourByThree = previousFourByThree;
        Settings.isLetterbox = previousLetterbox;
        whiteSquareField().set(null, previousWhiteSquare);
        BackgroundRenderGate.resetForTests();
        Sts1VerifyDiagnostics.resetForTests();
    }

    // ---------------------------------------------------------------- 1. variant parser

    @Test
    public void variantParserAcceptsCity() {
        assertEquals(BackgroundRenderGate.Variant.CITY, BackgroundRenderGate.Variant.parse("city"));
        assertEquals(BackgroundRenderGate.Variant.CITY,
                BackgroundRenderGate.Variant.parse(" City "));
        // Existing names are unchanged.
        assertEquals(BackgroundRenderGate.Variant.BOTTOM, BackgroundRenderGate.Variant.parse("bottom"));
        assertEquals(BackgroundRenderGate.Variant.GRID, BackgroundRenderGate.Variant.parse("grid"));
    }

    @Test
    public void pillarParserAcceptsNativeEnumNames() {
        assertEquals(BackgroundRenderGate.PillarConfig.OPEN,
                BackgroundRenderGate.PillarConfig.parse("open"));
        assertEquals(BackgroundRenderGate.PillarConfig.SIDES_ONLY,
                BackgroundRenderGate.PillarConfig.parse("sides_only"));
        assertEquals(BackgroundRenderGate.PillarConfig.FULL,
                BackgroundRenderGate.PillarConfig.parse("FULL"));
        assertEquals(BackgroundRenderGate.PillarConfig.LEFT_1,
                BackgroundRenderGate.PillarConfig.parse("left_1"));
        assertEquals(BackgroundRenderGate.PillarConfig.LEFT_2,
                BackgroundRenderGate.PillarConfig.parse("left_2"));
    }

    // ---------------------------------------------------------------- 2. native plan order

    @Test
    public void layerPlanFollowsNativeOrderIncludingGlowDoubles() {
        BackgroundRenderGate.CityFlags flags = cityFlags(
                false, true, true, true, true, true, true, false, true,
                BackgroundRenderGate.PillarConfig.FULL);

        List<String> names = names(BackgroundRenderGate.cityLayerPlan(flags, 2000L));

        assertEquals(Arrays.asList(
                "mod/bg1",
                "mod/bgGlowv2",
                "mod/bgGlowBlur",
                "mod/bgGlowBlur",
                "mod/floor",
                "mod/ceiling",
                "mod/wall",
                "mod/chains",
                "mod/chainsGlow",
                "mod/chainsGlow",
                "mod/mg1",
                "mod/mg1Glow",
                "mod/mg1Glow",
                "mod/mg2",
                "mod/p1",
                "mod/p2",
                "mod/p3",
                "mod/p4",
                "mod/p5",
                "mod/throne",
                "mod/throneGlow"),
                names);
    }

    @Test
    public void altBackgroundPassIsPresentOnlyForRenderAltBg() {
        BackgroundRenderGate.CityFlags withAlt = cityFlags(
                true, false, false, false, false, false, false, false, false,
                BackgroundRenderGate.PillarConfig.OPEN);
        assertEquals(Arrays.asList("mod/bg1", "mod/bgGlowv2", "mod/bg2", "mod/bg2Glow",
                "mod/floor", "mod/ceiling"),
                names(BackgroundRenderGate.cityLayerPlan(withAlt, 0L)));

        BackgroundRenderGate.CityFlags withoutAlt = cityFlags(
                false, false, false, false, false, false, false, false, false,
                BackgroundRenderGate.PillarConfig.OPEN);
        assertEquals(Arrays.asList("mod/bg1", "mod/bgGlowv2", "mod/floor", "mod/ceiling"),
                names(BackgroundRenderGate.cityLayerPlan(withoutAlt, 0L)));
    }

    @Test
    public void mgGlowIsDoubledOnlyWithRenderMgGlowAndMg2CarriesTheLastColor() {
        BackgroundRenderGate.CityFlags glow = cityFlags(
                false, true, true, true, false, false, false, false, false,
                BackgroundRenderGate.PillarConfig.OPEN);
        assertEquals(Arrays.asList("mod/bg1", "mod/bgGlowv2", "mod/floor", "mod/ceiling",
                "mod/mg1", "mod/mg1Glow", "mod/mg1Glow", "mod/mg2"),
                names(BackgroundRenderGate.cityLayerPlan(glow, 1000L)));

        BackgroundRenderGate.CityFlags noGlow = cityFlags(
                false, true, false, true, false, false, false, false, false,
                BackgroundRenderGate.PillarConfig.OPEN);
        List<BackgroundRenderGate.SceneLayerSpec> plan =
                BackgroundRenderGate.cityLayerPlan(noGlow, 1000L);
        assertEquals(Arrays.asList("mod/bg1", "mod/bgGlowv2", "mod/floor", "mod/ceiling",
                "mod/mg1", "mod/mg1Glow", "mod/mg2"),
                names(plan));
        assertEquals("the single mgGlow is additive",
                BackgroundRenderGate.SceneBlend.ADDITIVE, spec(plan, "mod/mg1Glow").blend);

        // Native gate: the mgGlow pass is drawn only when renderMg, even when renderMgGlow is set
        // (both the double branch and the else branch pass renderMg as their gate).
        BackgroundRenderGate.CityFlags glowWithoutMg = cityFlags(
                false, false, true, true, false, false, false, false, false,
                BackgroundRenderGate.PillarConfig.OPEN);
        assertFalse("renderMg=false must draw no mgGlow even with renderMgGlow=true",
                names(BackgroundRenderGate.cityLayerPlan(glowWithoutMg, 1000L))
                        .contains("mod/mg1Glow"));
    }

    // ---------------------------------------------------------------- 3. pillar switch

    @Test
    public void pillarConfigSelectsTheNativeSubset() {
        assertEquals(Arrays.asList(),
                pillarNames(BackgroundRenderGate.PillarConfig.OPEN));
        assertEquals(Arrays.asList("mod/p1", "mod/p5"),
                pillarNames(BackgroundRenderGate.PillarConfig.SIDES_ONLY));
        assertEquals(Arrays.asList("mod/p1", "mod/p2", "mod/p3", "mod/p4", "mod/p5"),
                pillarNames(BackgroundRenderGate.PillarConfig.FULL));
        assertEquals(Arrays.asList("mod/p1"),
                pillarNames(BackgroundRenderGate.PillarConfig.LEFT_1));
        assertEquals(Arrays.asList("mod/p1", "mod/p2"),
                pillarNames(BackgroundRenderGate.PillarConfig.LEFT_2));
    }

    // ---------------------------------------------------------------- 4. fail-open

    @Test
    public void failOpenPlanIsBackgroundFloorAndCeiling() {
        List<BackgroundRenderGate.SceneLayerSpec> failOpen =
                BackgroundRenderGate.cityLayerPlan(BackgroundRenderGate.CityFlags.failOpen(), 500L);
        assertEquals(Arrays.asList("mod/bg1", "mod/floor", "mod/ceiling"), names(failOpen));
        for (BackgroundRenderGate.SceneLayerSpec layer : failOpen) {
            assertEquals("fail-open overlay tint is WHITE", Color.WHITE, layer.tint);
            assertEquals("fail-open layers are NORMAL blended",
                    BackgroundRenderGate.SceneBlend.NORMAL, layer.blend);
        }

        // Null flags are treated exactly as failOpen().
        List<BackgroundRenderGate.SceneLayerSpec> nullFlags =
                BackgroundRenderGate.cityLayerPlan(null, 500L);
        assertEquals(Arrays.asList("mod/bg1", "mod/floor", "mod/ceiling"), names(nullFlags));
        for (BackgroundRenderGate.SceneLayerSpec layer : nullFlags) {
            assertEquals(Color.WHITE, layer.tint);
            assertEquals(BackgroundRenderGate.SceneBlend.NORMAL, layer.blend);
        }
    }

    // ---------------------------------------------------------------- 5. tint + blend

    @Test
    public void overlayTintAndBlendFollowTheNativeColorSequence() {
        Color overlay = new Color(0.5f, 0.25f, 0.125f, 1f);
        Color yellow = new Color(0.75f, 0.5f, 0.25f, 1f);
        BackgroundRenderGate.CityFlags flags = cityFlags(
                true, true, true, true, true, true, true, false, true,
                BackgroundRenderGate.PillarConfig.FULL, overlay, Color.WHITE, yellow);

        List<BackgroundRenderGate.SceneLayerSpec> plan =
                BackgroundRenderGate.cityLayerPlan(flags, 2000L);

        for (BackgroundRenderGate.SceneLayerSpec layer : plan) {
            boolean glow = isGlow(layer.regionName);
            assertEquals("blend for " + layer.regionName,
                    glow ? BackgroundRenderGate.SceneBlend.ADDITIVE
                            : BackgroundRenderGate.SceneBlend.NORMAL,
                    layer.blend);

            if ("mod/bg1".equals(layer.regionName)
                    || "mod/floor".equals(layer.regionName)
                    || "mod/ceiling".equals(layer.regionName)
                    || "mod/chains".equals(layer.regionName)
                    || "mod/wall".equals(layer.regionName)) {
                assertEquals("overlayColor is carried for " + layer.regionName,
                        overlay, layer.tint);
            }
        }

        // After the mgGlow double native setColor(yellowTint), so mg2 and the pillars carry yellow.
        assertEquals("mg2 carries the last setColor (yellowTint)",
                yellow, spec(plan, "mod/mg2").tint);
        assertEquals("pillars carry the last setColor (yellowTint)",
                yellow, spec(plan, "mod/p1").tint);
        assertEquals("throneGlow carries the last setColor (yellowTint)",
                yellow, spec(plan, "mod/throneGlow").tint);
    }

    @Test
    public void darkDayCarriesWhiteIntoSingleBackgroundPass() {
        Color overlay = new Color(0.25f, 0.5f, 0.75f, 1f);
        BackgroundRenderGate.CityFlags dark = cityFlags(
                false, false, false, false, false, false, false, false, true,
                BackgroundRenderGate.PillarConfig.OPEN, overlay, Color.WHITE, Color.WHITE);
        assertEquals("the darkDay bgGlowBlur double is WHITE",
                Color.WHITE, spec(BackgroundRenderGate.cityLayerPlan(dark, 0L), "mod/bgGlowBlur").tint);

        BackgroundRenderGate.CityFlags light = cityFlags(
                true, false, false, false, false, false, false, false, false,
                BackgroundRenderGate.PillarConfig.OPEN, overlay, Color.WHITE, Color.WHITE);
        assertEquals("without darkDay the carried overlayColor reaches bg2",
                overlay, spec(BackgroundRenderGate.cityLayerPlan(light, 0L), "mod/bg2").tint);
    }

    // ---------------------------------------------------------------- 6. animated alpha

    @Test
    public void chainAndMgGlowAlphasFollowTheAnimatedNativeTime() {
        BackgroundRenderGate.CityFlags flags = cityFlags(
                false, true, true, true, false, true, false, false, false,
                BackgroundRenderGate.PillarConfig.OPEN);

        // Native (TheCityScene.renderCombatRoomBg):
        //   chainsGlow.a = cosDeg(System.currentTimeMillis() % 360L) / 10 + 0.9   (period ~360 ms)
        //   mgGlow.a     = cosDeg(System.currentTimeMillis() / 10L % 360L) / 2 + 0.5
        // The cosDeg argument is MILLISECONDS in [0,360) DEGREES, not cos(seconds) / cos(seconds/10).

        // Deterministic anchors: cosDeg(90) == 0 and cosDeg(0) == 1.
        assertEquals("chainsGlow alpha is exactly the 0.9 base at cosDeg(90)==0",
                0.9, spec(BackgroundRenderGate.cityLayerPlan(flags, 90L), "mod/chainsGlow").tint.a,
                1e-6);
        assertEquals("chainsGlow alpha peaks at 1.0 at cosDeg(0)==1",
                1.0, spec(BackgroundRenderGate.cityLayerPlan(flags, 0L), "mod/chainsGlow").tint.a,
                1e-6);
        assertEquals("mgGlow alpha is exactly the 0.5 base at cosDeg(90)==0",
                0.5, spec(BackgroundRenderGate.cityLayerPlan(flags, 900L), "mod/mg1Glow").tint.a,
                1e-6);

        // Independent expectation, not a copy of the production expression.
        long nowMillis = 2000L;
        List<BackgroundRenderGate.SceneLayerSpec> plan =
                BackgroundRenderGate.cityLayerPlan(flags, nowMillis);
        float expectedChains =
                (float) Math.cos(Math.toRadians(nowMillis % 360L)) / 10f + 0.9f;
        float expectedMg =
                (float) Math.cos(Math.toRadians((nowMillis / 10L) % 360L)) / 2f + 0.5f;

        assertEquals("chainsGlow alpha follows cosDeg(nowMillis % 360)/10+0.9",
                expectedChains, spec(plan, "mod/chainsGlow").tint.a, 0.002);
        assertEquals("mgGlow alpha follows cosDeg(nowMillis/10 % 360)/2+0.5",
                expectedMg, spec(plan, "mod/mg1Glow").tint.a, 0.002);

        float chainsAt2000 = spec(plan, "mod/chainsGlow").tint.a;
        float mgAt2000 = spec(plan, "mod/mg1Glow").tint.a;

        long later = 3000L;
        float laterChains =
                (float) Math.cos(Math.toRadians(later % 360L)) / 10f + 0.9f;
        float laterMg =
                (float) Math.cos(Math.toRadians((later / 10L) % 360L)) / 2f + 0.5f;
        List<BackgroundRenderGate.SceneLayerSpec> laterPlan =
                BackgroundRenderGate.cityLayerPlan(flags, later);
        float chainsAt3000 = spec(laterPlan, "mod/chainsGlow").tint.a;
        float mgAt3000 = spec(laterPlan, "mod/mg1Glow").tint.a;

        assertEquals(laterChains, chainsAt3000, 0.002);
        assertEquals(laterMg, mgAt3000, 0.002);
        assertTrue("chainsGlow alpha animates with time",
                Math.abs(chainsAt2000 - chainsAt3000) > 0.005f);
        assertTrue("mgGlow alpha animates with time",
                Math.abs(mgAt2000 - mgAt3000) > 0.005f);
    }

    // ---------------------------------------------------------------- 7. real draw

    @Test
    public void realDrawMirrorsTheSixteenArgNativeCallInPlanOrder() {
        BackgroundRenderGate.CityFlags flags = cityFlags(
                true, true, true, true, true, true, true, false, true,
                BackgroundRenderGate.PillarConfig.FULL);
        CapturingBatch batch = newBatch(texture(1024, 1024));
        MapSource source = fullFixtureSource();
        BackgroundRenderGate.resetForTests();
        long nowMillis = 2000L;

        boolean painted = BackgroundRenderGate.renderCityBackground(batch, flags, source, nowMillis);

        assertTrue("a fully resolved plan must paint", painted);
        List<BackgroundRenderGate.SceneLayerSpec> plan =
                BackgroundRenderGate.cityLayerPlan(flags, nowMillis);
        assertEquals("one submitted draw per planned layer", plan.size(), batch.cityDraws.size());
        assertEquals("counter grows by the number of submitted layers",
                (long) plan.size(), BackgroundRenderGate.artDrawCount());

        for (int i = 0; i < plan.size(); i++) {
            BackgroundRenderGate.SceneLayerSpec layer = plan.get(i);
            String regionName = layer.regionName;
            Fixture fixture = FIXTURES.get(regionName);
            assertNotNull("missing fixture for " + regionName, fixture);
            Draw draw = batch.cityDraws.get(i);

            assertEquals("x is offsetX*Settings.scale for " + regionName,
                    fixture.offsetX * 1f, draw.x, 0.0001f);
            assertEquals("y is offsetY*Settings.scale for " + regionName,
                    fixture.offsetY * 1f, draw.y, 0.0001f);
            assertEquals("originX is 0 for " + regionName, 0f, draw.originX, 0.0001f);
            assertEquals("originY is 0 for " + regionName, 0f, draw.originY, 0.0001f);
            assertEquals("width is the packed width for " + regionName,
                    (float) fixture.srcWidth, draw.width, 0.0001f);
            assertEquals("height is the packed height for " + regionName,
                    (float) fixture.srcHeight, draw.height, 0.0001f);
            assertEquals("scaleX is Settings.scale for " + regionName, 1f, draw.scaleX, 0.0001f);
            assertEquals("scaleY is Settings.scale for " + regionName, 1f, draw.scaleY, 0.0001f);
            assertEquals("rotation is 0 for " + regionName, 0f, draw.rotation, 0.0001f);
            assertEquals("srcX is the region page x for " + regionName, fixture.srcX, draw.srcX);
            assertEquals("srcY is the region page y for " + regionName, fixture.srcY, draw.srcY);
            assertEquals("srcWidth is the packed width for " + regionName,
                    fixture.srcWidth, draw.srcWidth);
            assertEquals("srcHeight is the packed height for " + regionName,
                    fixture.srcHeight, draw.srcHeight);
            assertFalse("flipX is false for " + regionName, draw.flipX);
            assertFalse("flipY is false for " + regionName, draw.flipY);
            assertSame("texture comes from the region source for " + regionName,
                    source.page, draw.texture);
            assertEquals("the planned tint is active for " + regionName, layer.tint, draw.color);

            if (layer.blend == BackgroundRenderGate.SceneBlend.ADDITIVE) {
                assertEquals("additive src blend for " + regionName, 770, draw.blendSrc);
                assertEquals("additive dst blend for " + regionName, 1, draw.blendDst);
            } else {
                assertEquals("normal src blend for " + regionName, 770, draw.blendSrc);
                assertEquals("normal dst blend for " + regionName, 771, draw.blendDst);
            }
        }

        assertFalse("the draw requested at least one additive blend", batch.blendCalls.isEmpty());
        assertEquals("the batch is restored to NORMAL after the last additive glow",
                770, batch.blendSrc);
        assertEquals("the batch is restored to NORMAL after the last additive glow",
                771, batch.blendDst);
    }

    // ---------------------------------------------------------------- 8. all-or-nothing

    @Test
    public void missingBaseLayerFailsOpenInsteadOfPartialSuppression() {
        CapturingBatch batch = newBatch(texture(1024, 1024));
        BackgroundRenderGate.CityFlags flags = cityFlags(
                true, true, true, true, true, true, true, false, true,
                BackgroundRenderGate.PillarConfig.FULL);

        for (String base : new String[] {"mod/bg1", "mod/floor", "mod/ceiling"}) {
            MapSource partial = fullFixtureSource();
            partial.regions.remove(base);
            BackgroundRenderGate.resetForTests();
            assertFalse("a missing base layer (" + base + ") must not paint",
                    BackgroundRenderGate.renderCityBackground(batch, flags, partial, 2000L));
            assertEquals("no counter when a base layer is missing (" + base + ")",
                    0L, BackgroundRenderGate.artDrawCount());
        }
    }

    // ---------------------------------------------------------------- 9. not synthetic

    @Test
    public void cityIsNotTheSyntheticQuadPath() {
        installWhiteSquareTexture();
        CapturingBatch batch = newBatch(texture(1024, 1024));
        BackgroundRenderGate.resetForTests();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.CITY);

        assertFalse("the synthetic path must not claim City",
                BackgroundRenderGate.renderSelectedVariant(batch));
        assertEquals("the synthetic path records nothing for City",
                0L, BackgroundRenderGate.artDrawCount());
        assertEquals("the synthetic quad path was never entered", 0L, batch.syntheticQuads);
    }

    // ---------------------------------------------------------------- 10. atlas names

    @Test
    public void cityLayerRegionNamesMatchTheAtlas() {
        assertEquals("mod/bg1", BackgroundRenderGate.CityLayer.BG.regionName());
        assertEquals("mod/bgGlowv2", BackgroundRenderGate.CityLayer.BG_GLOW.regionName());
        assertEquals("mod/bgGlowBlur", BackgroundRenderGate.CityLayer.BG_GLOW2.regionName());
        assertEquals("mod/bg2", BackgroundRenderGate.CityLayer.BG2.regionName());
        assertEquals("mod/bg2Glow", BackgroundRenderGate.CityLayer.BG2_GLOW.regionName());
        assertEquals("mod/floor", BackgroundRenderGate.CityLayer.FLOOR.regionName());
        assertEquals("mod/ceiling", BackgroundRenderGate.CityLayer.CEILING.regionName());
        assertEquals("mod/wall", BackgroundRenderGate.CityLayer.WALL.regionName());
        assertEquals("mod/chains", BackgroundRenderGate.CityLayer.CHAINS.regionName());
        assertEquals("mod/chainsGlow", BackgroundRenderGate.CityLayer.CHAINS_GLOW.regionName());
        assertEquals("mod/mg1", BackgroundRenderGate.CityLayer.MG.regionName());
        assertEquals("mod/mg1Glow", BackgroundRenderGate.CityLayer.MG_GLOW.regionName());
        assertEquals("mod/mg2", BackgroundRenderGate.CityLayer.MG2.regionName());
        assertEquals("mod/p1", BackgroundRenderGate.CityLayer.P1.regionName());
        assertEquals("mod/p2", BackgroundRenderGate.CityLayer.P2.regionName());
        assertEquals("mod/p3", BackgroundRenderGate.CityLayer.P3.regionName());
        assertEquals("mod/p4", BackgroundRenderGate.CityLayer.P4.regionName());
        assertEquals("mod/p5", BackgroundRenderGate.CityLayer.P5.regionName());
        assertEquals("mod/throne", BackgroundRenderGate.CityLayer.THRONE.regionName());
        assertEquals("mod/throneGlow", BackgroundRenderGate.CityLayer.THRONE_GLOW.regionName());

        // Every declared layer has a concrete fixture region in this suite.
        for (BackgroundRenderGate.CityLayer layer : BackgroundRenderGate.CityLayer.values()) {
            assertTrue("missing fixture for " + layer.regionName(),
                    FIXTURES.containsKey(layer.regionName()));
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * {@code CityFlags.of} in the accepted field order: the nine dynamic booleans, the pillar
     * config, then overlayColor / whiteColor / yellowTint.
     */
    private static BackgroundRenderGate.CityFlags cityFlags(
            boolean renderAltBg, boolean renderMg, boolean renderMgGlow, boolean renderMgAlt,
            boolean renderWall, boolean renderChains, boolean renderThrone, boolean renderFg2,
            boolean darkDay, BackgroundRenderGate.PillarConfig pillars) {
        return cityFlags(renderAltBg, renderMg, renderMgGlow, renderMgAlt, renderWall, renderChains,
                renderThrone, renderFg2, darkDay, pillars, Color.WHITE, Color.WHITE, Color.WHITE);
    }

    private static BackgroundRenderGate.CityFlags cityFlags(
            boolean renderAltBg, boolean renderMg, boolean renderMgGlow, boolean renderMgAlt,
            boolean renderWall, boolean renderChains, boolean renderThrone, boolean renderFg2,
            boolean darkDay, BackgroundRenderGate.PillarConfig pillars,
            Color overlayColor, Color whiteColor, Color yellowTint) {
        return BackgroundRenderGate.CityFlags.of(renderAltBg, renderMg, renderMgGlow, renderMgAlt,
                renderWall, renderChains, renderThrone, renderFg2, darkDay, pillars,
                overlayColor, whiteColor, yellowTint);
    }

    private static List<String> names(List<BackgroundRenderGate.SceneLayerSpec> plan) {
        List<String> names = new ArrayList<String>(plan.size());
        for (BackgroundRenderGate.SceneLayerSpec layer : plan) {
            names.add(layer.regionName);
        }
        return names;
    }

    private static BackgroundRenderGate.SceneLayerSpec spec(
            List<BackgroundRenderGate.SceneLayerSpec> plan, String regionName) {
        for (BackgroundRenderGate.SceneLayerSpec layer : plan) {
            if (regionName.equals(layer.regionName)) {
                return layer;
            }
        }
        throw new AssertionError("no planned layer for " + regionName);
    }

    private static List<String> pillarNames(BackgroundRenderGate.PillarConfig pillars) {
        BackgroundRenderGate.CityFlags flags = cityFlags(
                false, false, false, false, false, false, false, false, false, pillars);
        List<String> pillarsOnly = new ArrayList<String>();
        for (String name : names(BackgroundRenderGate.cityLayerPlan(flags, 0L))) {
            if (name.startsWith("mod/p")) {
                pillarsOnly.add(name);
            }
        }
        return pillarsOnly;
    }

    private static boolean isGlow(String regionName) {
        return "mod/bgGlowv2".equals(regionName)
                || "mod/bgGlowBlur".equals(regionName)
                || "mod/bg2Glow".equals(regionName)
                || "mod/chainsGlow".equals(regionName)
                || "mod/mg1Glow".equals(regionName)
                || "mod/throneGlow".equals(regionName);
    }

    private static MapSource fullFixtureSource() {
        Texture page = texture(1024, 1024);
        MapSource source = new MapSource(page);
        for (Map.Entry<String, Fixture> entry : FIXTURES.entrySet()) {
            Fixture f = entry.getValue();
            AtlasRegion region = new AtlasRegion(
                    "",
                    entry.getKey(),
                    f.srcX,
                    f.srcY,
                    f.srcWidth,
                    f.srcHeight,
                    1024,
                    1024,
                    f.srcWidth,
                    f.srcHeight,
                    f.offsetX,
                    f.offsetY,
                    0);
            source.regions.put(entry.getKey(),
                    new Sts1AtlasMaterializer.RegionTexture(page, region));
        }
        return source;
    }

    private static final Map<String, Fixture> FIXTURES =
            new LinkedHashMap<String, Fixture>();

    static {
        FIXTURES.put("mod/bg1", new Fixture(10, 20, 100, 50, 1, 2));
        FIXTURES.put("mod/bgGlowv2", new Fixture(110, 28, 96, 48, 2, 3));
        FIXTURES.put("mod/bgGlowBlur", new Fixture(210, 36, 92, 46, 3, 4));
        FIXTURES.put("mod/bg2", new Fixture(310, 44, 88, 44, 4, 5));
        FIXTURES.put("mod/bg2Glow", new Fixture(410, 52, 84, 42, 5, 6));
        FIXTURES.put("mod/floor", new Fixture(510, 60, 80, 40, 6, 7));
        FIXTURES.put("mod/ceiling", new Fixture(610, 68, 76, 38, 7, 8));
        FIXTURES.put("mod/wall", new Fixture(710, 76, 72, 36, 8, 9));
        FIXTURES.put("mod/chains", new Fixture(810, 84, 68, 34, 9, 10));
        FIXTURES.put("mod/chainsGlow", new Fixture(830, 86, 66, 32, 10, 11));
        FIXTURES.put("mod/mg1", new Fixture(850, 88, 64, 30, 11, 12));
        FIXTURES.put("mod/mg1Glow", new Fixture(870, 90, 62, 28, 12, 13));
        FIXTURES.put("mod/mg2", new Fixture(890, 92, 60, 26, 13, 14));
        FIXTURES.put("mod/p1", new Fixture(600, 120, 40, 22, 14, 15));
        FIXTURES.put("mod/p2", new Fixture(620, 124, 41, 23, 15, 16));
        FIXTURES.put("mod/p3", new Fixture(640, 128, 42, 24, 16, 17));
        FIXTURES.put("mod/p4", new Fixture(660, 132, 43, 25, 17, 18));
        FIXTURES.put("mod/p5", new Fixture(680, 136, 44, 26, 18, 19));
        FIXTURES.put("mod/throne", new Fixture(300, 200, 50, 60, 19, 20));
        FIXTURES.put("mod/throneGlow", new Fixture(320, 204, 52, 62, 20, 21));
    }

    private static final class Fixture {
        final int srcX;
        final int srcY;
        final int srcWidth;
        final int srcHeight;
        final int offsetX;
        final int offsetY;

        Fixture(int srcX, int srcY, int srcWidth, int srcHeight, int offsetX, int offsetY) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.srcWidth = srcWidth;
            this.srcHeight = srcHeight;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    private static final class MapSource implements BackgroundRenderGate.BottomRegionSource {
        final Texture page;
        final Map<String, Sts1AtlasMaterializer.RegionTexture> regions =
                new LinkedHashMap<String, Sts1AtlasMaterializer.RegionTexture>();

        MapSource(Texture page) {
            this.page = page;
        }

        public Sts1AtlasMaterializer.RegionTexture region(String regionName) {
            return regions.get(regionName);
        }
    }

    /** One recorded 16-arg draw plus the tint and blend state active at submission time. */
    private static final class Draw {
        final Texture texture;
        final float x;
        final float y;
        final float originX;
        final float originY;
        final float width;
        final float height;
        final float scaleX;
        final float scaleY;
        final float rotation;
        final int srcX;
        final int srcY;
        final int srcWidth;
        final int srcHeight;
        final boolean flipX;
        final boolean flipY;
        final Color color;
        final int blendSrc;
        final int blendDst;

        Draw(Texture texture,
                float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation,
                int srcX, int srcY, int srcWidth, int srcHeight,
                boolean flipX, boolean flipY, Color color, int blendSrc, int blendDst) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.rotation = rotation;
            this.srcX = srcX;
            this.srcY = srcY;
            this.srcWidth = srcWidth;
            this.srcHeight = srcHeight;
            this.flipX = flipX;
            this.flipY = flipY;
            this.color = color == null ? null : new Color(color);
            this.blendSrc = blendSrc;
            this.blendDst = blendDst;
        }
    }

    /**
     * No-GL batch that records the 16-arg pixel-rect overload (the native City draw) separately from
     * the synthetic 4-arg quad path, tracks the active tint, and records every
     * {@code setBlendFunction} call without touching GL.
     */
    static class CapturingBatch extends SpriteBatch {
        // Unsafe.allocateInstance bypasses the constructor AND field initializers, so newBatch()
        // installs these reflectively; they must not be final.
        List<Draw> cityDraws;
        long syntheticQuads;
        Color current;
        int blendSrc;
        int blendDst;
        List<int[]> blendCalls;

        CapturingBatch() {
            super(1);
        }

        @Override
        public void setColor(Color color) {
            if (color != null) {
                current = color;
            }
        }

        @Override
        public void setColor(float r, float g, float b, float a) {
            current = new Color(r, g, b, a);
        }

        @Override
        public Color getColor() {
            return current;
        }

        @Override
        public void setBlendFunction(int srcFunc, int dstFunc) {
            blendSrc = srcFunc;
            blendDst = dstFunc;
            blendCalls.add(new int[] {srcFunc, dstFunc});
        }

        @Override
        public void draw(Texture texture,
                float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation,
                int srcX, int srcY, int srcWidth, int srcHeight,
                boolean flipX, boolean flipY) {
            cityDraws.add(new Draw(texture, x, y, originX, originY, width, height,
                    scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY,
                    current, blendSrc, blendDst));
        }

        @Override
        public void draw(Texture texture, float x, float y, float width, float height) {
            syntheticQuads++;
        }
    }

    private static CapturingBatch newBatch(Texture texture) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            CapturingBatch batch = (CapturingBatch) unsafe.allocateInstance(CapturingBatch.class);
            batch.cityDraws = new ArrayList<Draw>();
            batch.blendCalls = new ArrayList<int[]>();
            batch.current = Color.WHITE;
            batch.blendSrc = 770;
            batch.blendDst = 771;
            return batch;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL CapturingBatch", failure);
        }
    }

    private static Texture texture(final int width, final int height) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Texture texture = (Texture) ((sun.misc.Unsafe) unsafeField.get(null))
                    .allocateInstance(Texture.class);
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

    private static Field whiteSquareField() throws NoSuchFieldException, ClassNotFoundException {
        return Class.forName("com.megacrit.cardcrawl.helpers.ImageMaster")
                .getField("WHITE_SQUARE_IMG");
    }

    private static void installWhiteSquareTexture() {
        Texture texture = texture(1, 1);
        try {
            whiteSquareField().set(null, texture);
        } catch (Exception failure) {
            throw new AssertionError("could not install no-GL white-square texture double", failure);
        }
    }
}
