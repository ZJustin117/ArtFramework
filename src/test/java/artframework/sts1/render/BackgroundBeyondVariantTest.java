package artframework.sts1.render;

import artframework.assets.AtlasRegion;
import artframework.sts1.assets.Sts1AtlasMaterializer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * E08 Red: the {@code beyond} background variant draws the ACT-3 {@code TheBeyondScene} combat
 * background layers as real native atlas regions from the same pre-native Prefix ART already patches
 * (mirrors E06 {@code bottom} / E07 {@code city}).
 *
 * <p>The native {@code TheBeyondScene.renderCombatRoomBg} structure is authoritative (decompile): an
 * overlay pass ({@code floor}, {@code ceiling}, {@code bg1}, the alt {@code bg2}, the
 * {@code mg2}/{@code mg1}/{@code mg3}/{@code mg4} stack, the {@code columnConfig} columns
 * {@code c1..c4}, then {@code s1..s5}); an ADDITIVE repeat of that whole set except
 * {@code floor}/{@code ceiling}; the {@code i1..i5} pass at the restored forced-1.0 overlay alpha;
 * then the rotating {@code f1..f5} layers tinted from {@code tmpColor} blended with the overlay
 * colour. Each planned {@link BackgroundRenderGate.SceneLayerSpec} is one native
 * {@code renderAtlasRegionIf} call — one 16-arg libGDX pixel-rect submission.
 *
 * <p>Non-tautology: the plan order is asserted independently of the draw, the tint/blend assertions
 * are derived from the native {@code setColor}/{@code setBlendFunction} sequence (with an overlay
 * colour whose alpha is distinct so the forced-1.0 override and its restore are separately
 * observable), the animated offsets come from an independently written form of the native
 * trigonometric formulas, and the real draw is proven NOT to be the synthetic single-quad path
 * (which cannot claim a scene-scoped variant).
 */
public class BackgroundBeyondVariantTest {

    private static final float DELTA = 2e-3f;

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

    // ---------------------------------------------------------------- 1. parsers

    @Test
    public void variantParserAcceptsBeyond() {
        assertEquals(BackgroundRenderGate.Variant.BEYOND,
                BackgroundRenderGate.Variant.parse("beyond"));
        assertEquals(BackgroundRenderGate.Variant.BEYOND,
                BackgroundRenderGate.Variant.parse(" Beyond "));
        // Existing names are unchanged.
        assertEquals(BackgroundRenderGate.Variant.BOTTOM,
                BackgroundRenderGate.Variant.parse("bottom"));
        assertEquals(BackgroundRenderGate.Variant.CITY, BackgroundRenderGate.Variant.parse("city"));
        assertEquals(BackgroundRenderGate.Variant.GRID, BackgroundRenderGate.Variant.parse("grid"));
        assertEquals(BackgroundRenderGate.Variant.OFF, BackgroundRenderGate.Variant.parse("off"));
    }

    @Test
    public void columnConfigParserAcceptsNativeEnumNames() {
        assertEquals(BackgroundRenderGate.ColumnConfig.OPEN,
                BackgroundRenderGate.ColumnConfig.parse("open"));
        assertEquals(BackgroundRenderGate.ColumnConfig.SMALL_ONLY,
                BackgroundRenderGate.ColumnConfig.parse("SMALL_ONLY"));
        assertEquals(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT,
                BackgroundRenderGate.ColumnConfig.parse(" small_plus_left "));
        assertEquals(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_RIGHT,
                BackgroundRenderGate.ColumnConfig.parse("Small_Plus_Right"));
        assertNull(BackgroundRenderGate.ColumnConfig.parse("middle"));
        assertNull(BackgroundRenderGate.ColumnConfig.parse(null));
    }

    // ---------------------------------------------------------------- 2. atlas names

    @Test
    public void beyondLayerRegionNamesCoverEveryPlannedRegion() {
        List<String> declared = new ArrayList<String>();
        for (BackgroundRenderGate.BeyondLayer layer : BackgroundRenderGate.BeyondLayer.values()) {
            assertNotNull("regionName for " + layer, layer.regionName());
            declared.add(layer.regionName());
        }
        for (String expected : REGIONS) {
            assertTrue("no BeyondLayer constant declares " + expected + " (declared=" + declared + ")",
                    declared.contains(expected));
        }
        // Every region the plan can emit is a declared atlas layer; no ad-hoc string names.
        for (String planned : names(BackgroundRenderGate.beyondLayerPlan(allOn(
                BackgroundRenderGate.ColumnConfig.SMALL_PLUS_RIGHT), 0L))) {
            assertTrue("plan emitted an undeclared region: " + planned, declared.contains(planned));
        }
    }

    // ---------------------------------------------------------------- 3. native plan order

    @Test
    public void layerPlanFollowsTheNativeTwoPassOrder() {
        List<BackgroundRenderGate.SceneLayerSpec> plan = BackgroundRenderGate.beyondLayerPlan(
                allOn(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT), 10800L);

        assertEquals("one SceneLayerSpec per native renderAtlasRegionIf call", 40, plan.size());
        assertEquals(Arrays.asList(
                // pass 1 (overlay, NORMAL)
                "mod/floor", "mod/ceiling", "mod/bg1", "mod/bg2",
                "mod/mod2", "mod/mod1", "mod/mod3", "mod/mod4",
                "mod/c1", "mod/c2", "mod/c4",
                "mod/s1", "mod/s2", "mod/s3", "mod/s4", "mod/s5",
                // pass 2 (additive repeat of everything except floor/ceiling)
                "mod/bg1", "mod/bg2",
                "mod/mod2", "mod/mod1", "mod/mod3", "mod/mod4",
                "mod/c1", "mod/c2", "mod/c4",
                "mod/s1", "mod/s2", "mod/s3", "mod/s4", "mod/s5",
                // pass 3 (overlay forced back to a==1.0, NORMAL)
                "mod/i1", "mod/i2", "mod/i3", "mod/i4", "mod/i5",
                // pass 4 (tmpColor blend, rotating, NORMAL)
                "mod/f1", "mod/f2", "mod/f3", "mod/f4", "mod/f5"),
                names(plan));

        for (int i = 0; i < plan.size(); i++) {
            BackgroundRenderGate.SceneBlend expected =
                    i >= 16 && i < 30 ? BackgroundRenderGate.SceneBlend.ADDITIVE
                            : BackgroundRenderGate.SceneBlend.NORMAL;
            assertEquals("blend for " + plan.get(i).regionName, expected, plan.get(i).blend);
        }

        // Only the f layers are animated; the rotating overload is never used for the rest.
        for (BackgroundRenderGate.SceneLayerSpec layer : plan) {
            boolean flame = layer.regionName.matches("mod/f[1-5]");
            assertEquals("animated flag for " + layer.regionName, flame, layer.animated);
            if (!flame) {
                assertEquals("angle for " + layer.regionName, 0f, layer.angle, 0f);
                assertEquals("offsetX for " + layer.regionName, 0f, layer.offsetX, 0f);
                assertEquals("offsetY for " + layer.regionName, 0f, layer.offsetY, 0f);
            }
        }
    }

    @Test
    public void additivePassRepeatsEveryLayerExceptFloorAndCeiling() {
        List<BackgroundRenderGate.SceneLayerSpec> plan = BackgroundRenderGate.beyondLayerPlan(
                allOn(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT), 0L);

        // Pass 1 is floor/ceiling plus 14 layers; pass 2 is those same 14 layers, additively.
        for (int i = 0; i < 14; i++) {
            BackgroundRenderGate.SceneLayerSpec first = plan.get(2 + i);
            BackgroundRenderGate.SceneLayerSpec second = plan.get(16 + i);
            assertEquals("pass 2 repeats pass 1's layer", first.regionName, second.regionName);
            assertEquals("pass 1 is NORMAL for " + first.regionName,
                    BackgroundRenderGate.SceneBlend.NORMAL, first.blend);
            assertEquals("pass 2 is ADDITIVE for " + second.regionName,
                    BackgroundRenderGate.SceneBlend.ADDITIVE, second.blend);
        }
        assertEquals("floor is drawn once (normal pass only)",
                1, count(names(plan), "mod/floor"));
        assertEquals("ceiling is drawn once (normal pass only)",
                1, count(names(plan), "mod/ceiling"));
        assertEquals("bg1 is drawn in both passes",
                2, count(names(plan), "mod/bg1"));
    }

    // ---------------------------------------------------------------- 4. columnConfig switch

    @Test
    public void columnConfigSelectsTheNativeSubsetInBothPasses() {
        boolean[] base = allFalse();

        assertEquals(Arrays.asList("mod/floor", "mod/ceiling", "mod/bg1", "mod/bg1"),
                names(BackgroundRenderGate.beyondLayerPlan(
                        flagsFrom(base, BackgroundRenderGate.ColumnConfig.OPEN), 0L)));

        assertEquals(Arrays.asList(
                "mod/floor", "mod/ceiling", "mod/bg1", "mod/c1", "mod/c4",
                "mod/bg1", "mod/c1", "mod/c4"),
                names(BackgroundRenderGate.beyondLayerPlan(
                        flagsFrom(base, BackgroundRenderGate.ColumnConfig.SMALL_ONLY), 0L)));

        assertEquals(Arrays.asList(
                "mod/floor", "mod/ceiling", "mod/bg1", "mod/c1", "mod/c2", "mod/c4",
                "mod/bg1", "mod/c1", "mod/c2", "mod/c4"),
                names(BackgroundRenderGate.beyondLayerPlan(
                        flagsFrom(base, BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT), 0L)));

        assertEquals(Arrays.asList(
                "mod/floor", "mod/ceiling", "mod/bg1", "mod/c1", "mod/c3", "mod/c4",
                "mod/bg1", "mod/c1", "mod/c3", "mod/c4"),
                names(BackgroundRenderGate.beyondLayerPlan(
                        flagsFrom(base, BackgroundRenderGate.ColumnConfig.SMALL_PLUS_RIGHT), 0L)));
    }

    // ---------------------------------------------------------------- 5. tint / blend

    @Test
    public void overlayAlphaOverrideAndRestoreDriveThePassTints() {
        // Distinct alpha: pass 1 forces a==1.0, the additive pass reads the original 0.5, and the
        // `i` pass forces a==1.0 again. The three are therefore independently observable.
        Color overlay = new Color(0.4f, 0.2f, 0.6f, 0.5f);
        Color tmp = new Color(0.8f, 0.4f, 0.2f, 0.7f);
        List<BackgroundRenderGate.SceneLayerSpec> plan = BackgroundRenderGate.beyondLayerPlan(
                allOn(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT, overlay, tmp), 10800L);

        assertEquals(40, plan.size());
        int additive = 0;
        int lanterns = 0;
        int flames = 0;
        for (BackgroundRenderGate.SceneLayerSpec layer : plan) {
            if (layer.regionName.startsWith("mod/i")) {
                lanterns++;
                assertColor("the i pass forces overlayColor.a back to 1.0",
                        0.4f, 0.2f, 0.6f, 1f, layer.tint);
            } else if (layer.regionName.matches("mod/f[1-5]")) {
                flames++;
                assertColor("the f pass carries tmpColor blended with overlayColor",
                        (1f + 0.4f) / 2f, (1f + 0.2f) / 2f, (1f + 0.6f) / 2f, 0.7f, layer.tint);
            } else if (layer.blend == BackgroundRenderGate.SceneBlend.ADDITIVE) {
                additive++;
                assertColor("the additive pass reads overlayColor with its ORIGINAL alpha",
                        0.4f, 0.2f, 0.6f, 0.5f, layer.tint);
            } else {
                assertColor("the overlay pass forces overlayColor.a to 1.0",
                        0.4f, 0.2f, 0.6f, 1f, layer.tint);
            }
        }
        assertEquals("14 additive layers in the repeat pass", 14, additive);
        assertEquals("5 lantern layers", 5, lanterns);
        assertEquals("5 foreground flame layers", 5, flames);
    }

    // ---------------------------------------------------------------- 6. fail-open

    @Test
    public void beyondFlagsFailOpenToFloorCeilingAndBackground() {
        List<BackgroundRenderGate.SceneLayerSpec> failOpen = BackgroundRenderGate.beyondLayerPlan(
                BackgroundRenderGate.BeyondFlags.failOpen(), 500L);
        assertEquals(Arrays.asList("mod/floor", "mod/ceiling", "mod/bg1"), names(failOpen));
        for (BackgroundRenderGate.SceneLayerSpec layer : failOpen) {
            assertEquals("fail-open layers are NORMAL blended",
                    BackgroundRenderGate.SceneBlend.NORMAL, layer.blend);
            assertEquals("fail-open tint is the (WHITE) overlay", Color.WHITE, layer.tint);
            assertFalse("fail-open layers are not animated", layer.animated);
        }

        // Null flags are treated exactly as failOpen().
        List<BackgroundRenderGate.SceneLayerSpec> nullFlags =
                BackgroundRenderGate.beyondLayerPlan(null, 500L);
        assertEquals(Arrays.asList("mod/floor", "mod/ceiling", "mod/bg1"), names(nullFlags));
        for (BackgroundRenderGate.SceneLayerSpec layer : nullFlags) {
            assertEquals(BackgroundRenderGate.SceneBlend.NORMAL, layer.blend);
            assertEquals(Color.WHITE, layer.tint);
            assertFalse(layer.animated);
        }
    }

    // ---------------------------------------------------------------- 7. flag gating

    @Test
    public void altBackgroundAndModuleFlagsGateTheirLayersInBothPasses() {
        List<String> withAlt = names(BackgroundRenderGate.beyondLayerPlan(
                flagsFrom(allTrue(), BackgroundRenderGate.ColumnConfig.OPEN), 0L));
        assertEquals("bg2 is drawn once per pass", 2, count(withAlt, "mod/bg2"));

        List<String> withoutAlt = names(BackgroundRenderGate.beyondLayerPlan(
                flagsFrom(set(allTrue(), 0, false), BackgroundRenderGate.ColumnConfig.OPEN), 0L));
        assertEquals("renderAltBg=false removes bg2 from both passes",
                0, count(withoutAlt, "mod/bg2"));

        for (int m = 0; m < 4; m++) {
            List<String> gated = names(BackgroundRenderGate.beyondLayerPlan(
                    flagsFrom(set(allTrue(), 1 + m, false), BackgroundRenderGate.ColumnConfig.OPEN),
                    0L));
            for (int n = 0; n < 4; n++) {
                boolean enabled = m != n;
                assertEquals("mod/mod" + (n + 1) + " count when renderM" + (m + 1) + "=" + enabled,
                        enabled ? 2 : 0, count(gated, "mod/mod" + (n + 1)));
            }
        }
    }

    @Test
    public void columnSpriteAndLanternFlagsGateTheirOwnLayers() {
        List<String> all = names(BackgroundRenderGate.beyondLayerPlan(
                allOn(BackgroundRenderGate.ColumnConfig.OPEN), 0L));
        for (int n = 0; n < 5; n++) {
            // The s stack is drawn in BOTH the overlay and the additive pass.
            assertEquals("mod/s" + (n + 1) + " is drawn in both passes when enabled",
                    2, count(all, "mod/s" + (n + 1)));
            assertEquals("mod/i" + (n + 1) + " is drawn in the lantern pass only",
                    1, count(all, "mod/i" + (n + 1)));
            assertEquals("mod/f" + (n + 1) + " is drawn in the flame pass only",
                    1, count(all, "mod/f" + (n + 1)));
        }

        for (int n = 0; n < 5; n++) {
            List<String> gated = names(BackgroundRenderGate.beyondLayerPlan(
                    flagsFrom(set(allTrue(), 10 + n, false), BackgroundRenderGate.ColumnConfig.OPEN),
                    0L));
            assertEquals("renderS" + (n + 1) + "=false removes it from both passes",
                    0, count(gated, "mod/s" + (n + 1)));

            gated = names(BackgroundRenderGate.beyondLayerPlan(
                    flagsFrom(set(allTrue(), 5 + n, false), BackgroundRenderGate.ColumnConfig.OPEN),
                    0L));
            assertEquals("renderI" + (n + 1) + "=false removes its lantern",
                    0, count(gated, "mod/i" + (n + 1)));

            gated = names(BackgroundRenderGate.beyondLayerPlan(
                    flagsFrom(set(allTrue(), 15 + n, false), BackgroundRenderGate.ColumnConfig.OPEN),
                    0L));
            assertEquals("renderF" + (n + 1) + "=false removes its flame",
                    0, count(gated, "mod/f" + (n + 1)));
        }
    }

    // ---------------------------------------------------------------- 8. animated offsets

    @Test
    public void animatedForegroundLayersCarryTheNativeTransforms() {
        long now = 10800L;
        BackgroundRenderGate.BeyondFlags flags = allOn(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT);
        List<BackgroundRenderGate.SceneLayerSpec> plan = BackgroundRenderGate.beyondLayerPlan(flags, now);

        // Independently written from the native formulas. cosDeg is the design-authority helper
        // (com.badlogic.gdx.math.MathUtils.cosDeg) and is not part of BackgroundRenderGate; every
        // argument, multiplier and sign below is re-derived from the decompiled call list.
        //
        // f1: cosDeg((now+180)/180%360)*40*xScale, cosDeg((now+500)/72%360)*20*scale,
        //     angle cosDeg((now+180)/180%360)
        float f1Angle = MathUtils.cosDeg((now + 180L) / 180L % 360L);
        float f1X = f1Angle * 40f * Settings.xScale;
        float f1Y = MathUtils.cosDeg((now + 500L) / 72L % 360L) * 20f * Settings.scale;
        assertTrue("f1 is animated", spec(plan, "mod/f1").animated);
        assertEquals("f1 angle", f1Angle, spec(plan, "mod/f1").angle, DELTA);
        assertEquals("f1 offsetX", f1X, spec(plan, "mod/f1").offsetX, DELTA);
        assertEquals("f1 offsetY", f1Y, spec(plan, "mod/f1").offsetY, DELTA);

        // f2: cosDeg((now+91723)/72%360)*20, 0, angle (now/120%360)
        BackgroundRenderGate.SceneLayerSpec f2 = spec(plan, "mod/f2");
        assertTrue("f2 is animated", f2.animated);
        assertEquals("f2 offsetX", MathUtils.cosDeg((now + 91723L) / 72L % 360L) * 20f, f2.offsetX, DELTA);
        assertEquals("f2 offsetY", 0f, f2.offsetY, 0f);
        assertEquals("f2 angle is a raw degree value", (float) (now / 120L % 360L), f2.angle, 0f);

        // f3: -80*scale, cosDeg(now+73)*10-90*scale, angle (now/1000%360)*2
        BackgroundRenderGate.SceneLayerSpec f3 = spec(plan, "mod/f3");
        assertTrue("f3 is animated", f3.animated);
        assertEquals("f3 offsetX", -80f * Settings.scale, f3.offsetX, DELTA);
        assertEquals("f3 offsetY",
                MathUtils.cosDeg(now + 73L) * 10f - 90f * Settings.scale, f3.offsetY, DELTA);
        assertEquals("f3 angle", (float) (now / 1000L % 360L) * 2f, f3.angle, DELTA);

        // f4: 0, cosDeg((now+4442)/20%360)*30*scale, angle cosDeg((now+4442)/10%360)*20
        BackgroundRenderGate.SceneLayerSpec f4 = spec(plan, "mod/f4");
        assertTrue("f4 is animated", f4.animated);
        assertEquals("f4 offsetX", 0f, f4.offsetX, 0f);
        assertEquals("f4 offsetY",
                MathUtils.cosDeg((now + 4442L) / 20L % 360L) * 30f * Settings.scale, f4.offsetY, DELTA);
        assertEquals("f4 angle",
                MathUtils.cosDeg((now + 4442L) / 10L % 360L) * 20f, f4.angle, DELTA);

        // f5: 0, cosDeg(now/48%360)*20, angle 0
        BackgroundRenderGate.SceneLayerSpec f5 = spec(plan, "mod/f5");
        assertTrue("f5 is animated", f5.animated);
        assertEquals("f5 offsetX", 0f, f5.offsetX, 0f);
        assertEquals("f5 offsetY", MathUtils.cosDeg(now / 48L % 360L) * 20f, f5.offsetY, DELTA);
        assertEquals("f5 angle", 0f, f5.angle, 0f);

        // The f1 transform is a function of the frame clock: a different `now` moves it.
        float movedX = spec(BackgroundRenderGate.beyondLayerPlan(flags, 12000L), "mod/f1").offsetX;
        float movedAngle = spec(BackgroundRenderGate.beyondLayerPlan(flags, 12000L), "mod/f1").angle;
        assertTrue("f1 offsetX animates with the frame clock", Math.abs(movedX - f1X) > 0.01f);
        assertTrue("f1 angle animates with the frame clock",
                Math.abs(movedAngle - f1Angle) > 0.001f);
    }

    // ---------------------------------------------------------------- 9. real draw

    @Test
    public void realDrawSubmitsOneNativePixelRectPerPlannedLayer() {
        Color overlay = new Color(0.4f, 0.2f, 0.6f, 0.5f);
        Color tmp = new Color(0.8f, 0.4f, 0.2f, 0.7f);
        BackgroundRenderGate.BeyondFlags flags =
                allOn(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT, overlay, tmp);
        CapturingBatch batch = newBatch();
        MapSource source = fullFixtureSource();
        BackgroundRenderGate.resetForTests();
        long now = 10800L;

        boolean painted = BackgroundRenderGate.renderBeyondBackground(batch, flags, source, now);

        assertTrue("a fully resolved plan must paint", painted);
        List<BackgroundRenderGate.SceneLayerSpec> plan =
                BackgroundRenderGate.beyondLayerPlan(flags, now);
        assertEquals(40, plan.size());
        assertEquals("one submitted draw per planned layer", plan.size(), batch.draws.size());
        assertEquals("counter grows by the number of submitted layers",
                (long) plan.size(), BackgroundRenderGate.artDrawCount());

        float scale = Settings.scale;
        for (int i = 0; i < plan.size(); i++) {
            BackgroundRenderGate.SceneLayerSpec layer = plan.get(i);
            String regionName = layer.regionName;
            Fixture fixture = FIXTURES.get(regionName);
            assertNotNull("missing fixture for " + regionName, fixture);
            Draw draw = batch.draws.get(i);

            assertEquals("the planned tint is active for " + regionName,
                    layer.tint.r, draw.color.r, 1e-6f);
            assertEquals("the planned tint is active for " + regionName,
                    layer.tint.g, draw.color.g, 1e-6f);
            assertEquals("the planned tint is active for " + regionName,
                    layer.tint.b, draw.color.b, 1e-6f);
            assertEquals("the planned tint is active for " + regionName,
                    layer.tint.a, draw.color.a, 1e-6f);

            if (layer.blend == BackgroundRenderGate.SceneBlend.ADDITIVE) {
                assertEquals("additive src blend for " + regionName, 770, draw.blendSrc);
                assertEquals("additive dst blend for " + regionName, 1, draw.blendDst);
            } else {
                assertEquals("normal src blend for " + regionName, 770, draw.blendSrc);
                assertEquals("normal dst blend for " + regionName, 771, draw.blendDst);
            }

            assertEquals("width is the packed width for " + regionName,
                    (float) fixture.srcWidth, draw.width, 0.0001f);
            assertEquals("height is the packed height for " + regionName,
                    (float) fixture.srcHeight, draw.height, 0.0001f);
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
            assertEquals("scaleX is Settings.scale for " + regionName, scale, draw.scaleX, 0.0001f);
            assertEquals("scaleY is Settings.scale for " + regionName, scale, draw.scaleY, 0.0001f);

            // x = region.offsetX * scale + planned x (the plan's x is 0 for every plain layer).
            assertEquals("x for " + regionName,
                    fixture.offsetX * scale + layer.offsetX, draw.x, DELTA);
            assertEquals("y for " + regionName,
                    fixture.offsetY * scale + layer.offsetY, draw.y, DELTA);

            if (layer.animated) {
                assertEquals("animated layers pivot on the packed centre for " + regionName,
                        fixture.srcWidth / 2f, draw.originX, 0.0001f);
                assertEquals("animated layers pivot on the packed centre for " + regionName,
                        fixture.srcHeight / 2f, draw.originY, 0.0001f);
                assertEquals("rotation is the planned angle for " + regionName,
                        layer.angle, draw.rotation, DELTA);
            } else {
                assertEquals("plain layers use origin (0,0) for " + regionName,
                        0f, draw.originX, 0f);
                assertEquals("plain layers use origin (0,0) for " + regionName,
                        0f, draw.originY, 0f);
                assertEquals("plain layers are unrotated for " + regionName,
                        0f, draw.rotation, 0f);
            }
        }

        // The rotating overload always pivots on the packed centre, even for f5 whose native angle
        // is exactly 0, so the origin — not the rotation — identifies it.
        assertEquals("exactly the f layers use the rotating overload",
                5, batch.centeredOriginDraws);
        assertFalse("the additive pass switched the blend function", batch.blendCalls.isEmpty());
        assertEquals("the batch is restored to NORMAL after the additions",
                770, batch.blendSrc);
        assertEquals("the batch is restored to NORMAL after the additions",
                771, batch.blendDst);
    }

    // ---------------------------------------------------------------- 10. all-or-nothing

    @Test
    public void missingBaseLayerFailsOpenInsteadOfPartialSuppression() {
        BackgroundRenderGate.BeyondFlags flags =
                allOn(BackgroundRenderGate.ColumnConfig.SMALL_PLUS_LEFT);

        for (String base : new String[] {"mod/floor", "mod/ceiling", "mod/bg1"}) {
            CapturingBatch batch = newBatch();
            MapSource partial = fullFixtureSource();
            partial.regions.remove(base);
            BackgroundRenderGate.resetForTests();
            assertFalse("a missing base layer (" + base + ") must not paint",
                    BackgroundRenderGate.renderBeyondBackground(batch, flags, partial, 10800L));
            assertEquals("no counter when a base layer is missing (" + base + ")",
                    0L, BackgroundRenderGate.artDrawCount());
            assertEquals("no draws when a base layer is missing (" + base + ")",
                    0, batch.draws.size());
        }
    }

    // ---------------------------------------------------------------- 11. not synthetic / scene guard

    @Test
    public void beyondIsNotTheSyntheticQuadPath() {
        installWhiteSquareTexture();
        CapturingBatch batch = newBatch();
        BackgroundRenderGate.resetForTests();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.BEYOND);

        assertFalse("the synthetic path must not claim Beyond",
                BackgroundRenderGate.renderSelectedVariant(batch));
        assertEquals("the synthetic path records nothing for Beyond",
                0L, BackgroundRenderGate.artDrawCount());
        assertEquals("the synthetic quad path was never entered", 0L, batch.syntheticQuads);
    }

    @Test
    public void beyondDrawIsScopedToTheBeyondScene() {
        CapturingBatch batch = newBatch();
        BackgroundRenderGate.resetForTests();

        assertFalse("a non-Beyond scene instance must fail open",
                BackgroundRenderGate.renderBeyondBackground(batch, new Object()));
        assertFalse("a null scene instance must fail open",
                BackgroundRenderGate.renderBeyondBackground(batch, (Object) null));
        assertFalse("a null batch must fail open",
                BackgroundRenderGate.renderBeyondBackground(null, new Object()));
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertEquals(0, batch.draws.size());
    }

    // ---------------------------------------------------------------- 12. animated native scale

    @Test
    public void animatedForegroundLayersUseTheNativeSingleScaleGeometry() {
        // The native rotating overload AbstractScene.renderAtlasRegionIf(sb, x, y, angle, region,
        // cond) unconditionally uses Settings.scale: x = region.offsetX * scale + x,
        // y = region.offsetY * scale + y, scaleX = scaleY = scale. It does NOT follow the plain
        // draw's four-by-three / letterbox three-branch rule, so on non-default displays the
        // animated f layers must still be placed and scaled by the single Settings.scale.
        long now = 10800L;
        Color overlay = new Color(0.4f, 0.2f, 0.6f, 0.5f);
        Color tmp = new Color(0.8f, 0.4f, 0.2f, 0.7f);
        BackgroundRenderGate.BeyondFlags flags =
                allOn(BackgroundRenderGate.ColumnConfig.OPEN, overlay, tmp);

        // Letterbox is the active branch. scale is distinct from xScale and yScale, so the
        // three-branch rule and the native single-scale rule cannot both satisfy the assertions.
        Settings.isFourByThree = false;
        Settings.isLetterbox = true;
        Settings.scale = 1.333f;
        Settings.xScale = 1.792f;
        Settings.yScale = 1.5f;
        assertAnimatedFlamesUseSingleScale(flags, now);

        // Four-by-three is the active branch. yScale is distinct from scale, so the y/scaleY
        // assertions below fail under the old three-branch rule.
        Settings.isFourByThree = true;
        Settings.isLetterbox = false;
        Settings.scale = 1f;
        Settings.xScale = 1f;
        Settings.yScale = 1.6f;
        assertAnimatedFlamesUseSingleScale(flags, now);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Draws {@code flags} and asserts that every animated {@code f} layer is submitted through the
     * native rotating overload using only {@code Settings.scale} for the region offset and both
     * scales. The draw index equals the plan index because the real draw emits one submission per
     * planned layer in order.
     */
    private static void assertAnimatedFlamesUseSingleScale(
            BackgroundRenderGate.BeyondFlags flags, long now) {
        CapturingBatch batch = newBatch();
        BackgroundRenderGate.resetForTests();
        assertTrue("a fully resolved plan must paint",
                BackgroundRenderGate.renderBeyondBackground(batch, flags, fullFixtureSource(), now));
        List<BackgroundRenderGate.SceneLayerSpec> plan =
                BackgroundRenderGate.beyondLayerPlan(flags, now);

        int animated = 0;
        for (int i = 0; i < plan.size(); i++) {
            BackgroundRenderGate.SceneLayerSpec layer = plan.get(i);
            if (!layer.animated) {
                continue;
            }
            animated++;
            Fixture fixture = FIXTURES.get(layer.regionName);
            Draw draw = batch.draws.get(i);
            assertEquals("the rotating overload uses Settings.scale for x on " + layer.regionName,
                    fixture.offsetX * Settings.scale + layer.offsetX, draw.x, DELTA);
            assertEquals("the rotating overload uses Settings.scale for y on " + layer.regionName,
                    fixture.offsetY * Settings.scale + layer.offsetY, draw.y, DELTA);
            assertEquals("the rotating overload uses Settings.scale for scaleX on "
                            + layer.regionName,
                    Settings.scale, draw.scaleX, 0.0001f);
            assertEquals("the rotating overload uses Settings.scale for scaleY on "
                            + layer.regionName,
                    Settings.scale, draw.scaleY, 0.0001f);
        }
        assertEquals("all five f layers are animated and drawn", 5, animated);
    }

    /** All twenty dynamic booleans true, column config OPEN. */
    private static BackgroundRenderGate.BeyondFlags allOn(
            BackgroundRenderGate.ColumnConfig column) {
        return flagsFrom(allTrue(), column, Color.WHITE, Color.WHITE);
    }

    private static BackgroundRenderGate.BeyondFlags allOn(
            BackgroundRenderGate.ColumnConfig column, Color overlay, Color tmp) {
        return flagsFrom(allTrue(), column, overlay, tmp);
    }

    /**
     * {@code BeyondFlags.of} in the accepted field order: renderAltBg, renderM1..4, renderI1..5,
     * renderS1..5, renderF1..5, columnConfig, overlayColor, tmpColor.
     */
    private static BackgroundRenderGate.BeyondFlags flagsFrom(boolean[] v,
            BackgroundRenderGate.ColumnConfig column) {
        return flagsFrom(v, column, Color.WHITE, Color.WHITE);
    }

    private static BackgroundRenderGate.BeyondFlags flagsFrom(boolean[] v,
            BackgroundRenderGate.ColumnConfig column, Color overlay, Color tmp) {
        return BackgroundRenderGate.BeyondFlags.of(
                v[0], v[1], v[2], v[3], v[4],
                v[5], v[6], v[7], v[8], v[9],
                v[10], v[11], v[12], v[13], v[14],
                v[15], v[16], v[17], v[18], v[19],
                column, overlay, tmp);
    }

    private static boolean[] allTrue() {
        boolean[] v = new boolean[20];
        Arrays.fill(v, true);
        return v;
    }

    private static boolean[] allFalse() {
        return new boolean[20];
    }

    private static boolean[] set(boolean[] values, int index, boolean value) {
        values[index] = value;
        return values;
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

    private static int count(List<String> names, String regionName) {
        int total = 0;
        for (String name : names) {
            if (regionName.equals(name)) {
                total++;
            }
        }
        return total;
    }

    private static void assertColor(String message, float r, float g, float b, float a, Color actual) {
        assertNotNull(message, actual);
        assertEquals(message + " (r)", r, actual.r, 1e-6f);
        assertEquals(message + " (g)", g, actual.g, 1e-6f);
        assertEquals(message + " (b)", b, actual.b, 1e-6f);
        assertEquals(message + " (a)", a, actual.a, 1e-6f);
    }

    /** The {@code beyondScene/scene.atlas} regions this suite fixtures (fg is deferred to E08b). */
    private static final String[] REGIONS = {
            "mod/floor", "mod/ceiling", "mod/bg1", "mod/bg2",
            "mod/mod1", "mod/mod2", "mod/mod3", "mod/mod4",
            "mod/c1", "mod/c2", "mod/c3", "mod/c4",
            "mod/s1", "mod/s2", "mod/s3", "mod/s4", "mod/s5",
            "mod/i1", "mod/i2", "mod/i3", "mod/i4", "mod/i5",
            "mod/f1", "mod/f2", "mod/f3", "mod/f4", "mod/f5"};

    private static final Map<String, Fixture> FIXTURES =
            new LinkedHashMap<String, Fixture>();

    static {
        for (int i = 0; i < REGIONS.length; i++) {
            // Distinct page rects and trim offsets so a draw can be attributed to one region.
            FIXTURES.put(REGIONS[i], new Fixture(
                    8 + i * 17, 12 + i * 13, 60 + i, 40 + i, 1 + i, 2 + i));
        }
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
     * No-GL batch that records the 16-arg pixel-rect overload (the native draw for both the plain
     * and the rotating {@code renderAtlasRegionIf} forms) separately from the synthetic 4-arg quad
     * path, tracks the active tint, and records every {@code setBlendFunction} call without GL.
     */
    static class CapturingBatch extends SpriteBatch {
        // Unsafe.allocateInstance bypasses the constructor AND field initializers, so newBatch()
        // installs these reflectively; they must not be final.
        List<Draw> draws;
        long centeredOriginDraws;
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
            if (originX != 0f || originY != 0f) {
                centeredOriginDraws++;
            }
            draws.add(new Draw(texture, x, y, originX, originY, width, height,
                    scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY,
                    current, blendSrc, blendDst));
        }

        @Override
        public void draw(Texture texture, float x, float y, float width, float height) {
            syntheticQuads++;
        }
    }

    private static CapturingBatch newBatch() {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            CapturingBatch batch = (CapturingBatch) unsafe.allocateInstance(CapturingBatch.class);
            batch.draws = new ArrayList<Draw>();
            batch.blendCalls = new ArrayList<int[]>();
            batch.current = Color.WHITE;
            batch.blendSrc = 770;
            batch.blendDst = 771;
            return batch;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL CapturingBatch", failure);
        }
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
