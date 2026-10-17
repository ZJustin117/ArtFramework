package artframework.sts1.render;

import artframework.sts1.assets.Sts1AtlasMaterializer;
import artframework.sts1.assets.Sts1GdxAtlasRegions;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure pre-native background ownership decision plus variant draws for the
 * {@code art verify mode background} surface. The gate concerns only the single background family
 * and fails open on panic; it never suppresses any other native pixels.
 */
public final class BackgroundRenderGate {

    public static final String BACKGROUND_FAMILY = "sts1.room.background";

    /**
     * The concrete scene classes the background family patch targets, as simple class names. This is
     * the STATIC coverage contract — the four concrete overrides of
     * {@code AbstractScene.renderCombatRoomBg(SpriteBatch)} that {@code BackgroundRenderPatches}
     * attaches Prefixes to. It says which scenes are PATCHED; it is deliberately NOT a claim of
     * device verification.
     * <p>
     * The lab act-jump ({@code art lab enter-act <2|3|4>}, L01) exists, so the ACT-2/ACT-3/ACT-4
     * blocks in {@code d1_verify_background.yaml} are ARMED: each jumps to its act and then asserts
     * the per-act real-region draw delta and the filtered close-diff. Only the ACT-1/Bottom block has
     * device evidence (E06); ACT-2/ACT-3/ACT-4 device verification remains PENDING — no device run
     * has been performed for those blocks, so their status is an open gap, not a claim.
     */
    public static final List<String> PATCHED_SCENES = Collections.unmodifiableList(
            Arrays.asList("TheBottomScene", "TheCityScene", "TheBeyondScene", "TheEndingScene"));

    public enum Variant {
        OFF,
        SOLID,
        CHECKER,
        GRID,
        /** Real ACT-1 {@code TheBottomScene} combat background layers (native atlas regions). */
        BOTTOM,
        /** Real ACT-2 {@code TheCityScene} combat background layers (native atlas regions). */
        CITY,
        /** Real ACT-3 {@code TheBeyondScene} combat background layers (native atlas regions). */
        BEYOND,
        /** Real ACT-4 {@code TheEndingScene} combat background region (native atlas region). */
        ENDING;

        public static Variant parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase();
            if ("off".equals(normalized)) return OFF;
            if ("solid".equals(normalized)) return SOLID;
            if ("checker".equals(normalized)) return CHECKER;
            if ("grid".equals(normalized)) return GRID;
            if ("bottom".equals(normalized)) return BOTTOM;
            if ("city".equals(normalized)) return CITY;
            if ("beyond".equals(normalized)) return BEYOND;
            if ("ending".equals(normalized)) return ENDING;
            return null;
        }
    }

    /**
     * The native ACT-1 {@code TheBottomScene.renderCombatRoomBg} layer order, one entry per
     * {@code renderAtlasRegionIf} call. {@link #regionName()} is the {@code bottomScene/scene.atlas}
     * region name the native scene resolves (decompile: {@code bg}, {@code mod/mg},
     * {@code mod/midWall}, {@code mod/mod2}, {@code mod/mod1}, {@code mod/ceiling},
     * {@code mod/ceilingMod1..6}).
     */
    public enum BottomLayer {
        BG("bg"),
        MG("mod/mg"),
        SOLID_WALL("mod/midWall"),
        HOLLOW_WALL("mod/mod2"),
        LEFT_WALL("mod/mod1"),
        CEILING("mod/ceiling"),
        CEILING_MOD1("mod/ceilingMod1"),
        CEILING_MOD2("mod/ceilingMod2"),
        CEILING_MOD3("mod/ceilingMod3"),
        CEILING_MOD4("mod/ceilingMod4"),
        CEILING_MOD5("mod/ceilingMod5"),
        CEILING_MOD6("mod/ceilingMod6");

        private final String regionName;

        BottomLayer(String regionName) {
            this.regionName = regionName;
        }

        /** The native {@code bottomScene/scene.atlas} region name for this layer. */
        public String regionName() {
            return regionName;
        }
    }

    /**
     * The native ACT-2 {@code TheCityScene.renderCombatRoomBg} {@code pillarConfig} switch. The
     * constant names match the native enum exactly; {@code parse} is tolerant of case.
     */
    public enum PillarConfig {
        OPEN,
        SIDES_ONLY,
        FULL,
        LEFT_1,
        LEFT_2;

        /** Parses a native enum constant name (case/space tolerant); unknown ⇒ {@code null}. */
        public static PillarConfig parse(String value) {
            String normalized = value == null ? "" : value.trim().toUpperCase();
            if ("OPEN".equals(normalized)) return OPEN;
            if ("SIDES_ONLY".equals(normalized)) return SIDES_ONLY;
            if ("FULL".equals(normalized)) return FULL;
            if ("LEFT_1".equals(normalized)) return LEFT_1;
            if ("LEFT_2".equals(normalized)) return LEFT_2;
            return null;
        }
    }

    /** The libGDX blend states the native ACT-2 sequence toggles between. */
    public enum SceneBlend {
        NORMAL,
        ADDITIVE
    }

    /**
     * The native ACT-3 {@code TheBeyondScene.renderCombatRoomBg} {@code columnConfig} switch. The
     * constant names match the native enum exactly; {@code parse} is tolerant of case.
     */
    public enum ColumnConfig {
        OPEN,
        SMALL_ONLY,
        SMALL_PLUS_LEFT,
        SMALL_PLUS_RIGHT;

        /** Parses a native enum constant name (case/space tolerant); unknown ⇒ {@code null}. */
        public static ColumnConfig parse(String value) {
            String normalized = value == null ? "" : value.trim().toUpperCase();
            if ("OPEN".equals(normalized)) return OPEN;
            if ("SMALL_ONLY".equals(normalized)) return SMALL_ONLY;
            if ("SMALL_PLUS_LEFT".equals(normalized)) return SMALL_PLUS_LEFT;
            if ("SMALL_PLUS_RIGHT".equals(normalized)) return SMALL_PLUS_RIGHT;
            return null;
        }
    }

    /**
     * The native ACT-3 {@code TheBeyondScene.renderCombatRoomBg} layer regions, one entry per native
     * atlas field. {@link #regionName()} is the {@code beyondScene/scene.atlas} region name the
     * native scene resolves.
     */
    public enum BeyondLayer {
        FLOOR("mod/floor"),
        CEILING("mod/ceiling"),
        BG1("mod/bg1"),
        BG2("mod/bg2"),
        MG1("mod/mod1"),
        MG2("mod/mod2"),
        MG3("mod/mod3"),
        MG4("mod/mod4"),
        C1("mod/c1"),
        C2("mod/c2"),
        C3("mod/c3"),
        C4("mod/c4"),
        S1("mod/s1"),
        S2("mod/s2"),
        S3("mod/s3"),
        S4("mod/s4"),
        S5("mod/s5"),
        I1("mod/i1"),
        I2("mod/i2"),
        I3("mod/i3"),
        I4("mod/i4"),
        I5("mod/i5"),
        F1("mod/f1"),
        F2("mod/f2"),
        F3("mod/f3"),
        F4("mod/f4"),
        F5("mod/f5");

        private final String regionName;

        BeyondLayer(String regionName) {
            this.regionName = regionName;
        }

        /** The native {@code beyondScene/scene.atlas} region name for this layer. */
        public String regionName() {
            return regionName;
        }
    }

    /**
     * One native {@code renderAtlasRegionIf} submission: the atlas region name, the active tint at
     * that call, and whether the additive {@code blend(770,1)} state is active. Tints are copied so
     * caller mutation cannot alias the plan.
     *
     * <p>{@code animated} selects the rotating {@code renderAtlasRegionIf} overload (packed-centre
     * origin, {@code layer.angle} rotation) and the plan-local {@code offsetX}/{@code offsetY} are
     * added to the region's placement (in scene units, scaled like the region offset). The plain
     * 3-arg constructor is the non-animated form used by the ACT-1/ACT-2 plans and leaves offsets at
     * {@code 0} with {@code angle == 0}.
     */
    public static final class SceneLayerSpec {
        public final String regionName;
        public final Color tint;
        public final SceneBlend blend;
        public final boolean animated;
        public final float offsetX;
        public final float offsetY;
        public final float angle;

        public SceneLayerSpec(String regionName, Color tint, SceneBlend blend) {
            this(regionName, tint, blend, 0f, 0f, 0f, false);
        }

        public SceneLayerSpec(String regionName, Color tint, SceneBlend blend, float offsetX,
                float offsetY, float angle) {
            this(regionName, tint, blend, offsetX, offsetY, angle, true);
        }

        private SceneLayerSpec(String regionName, Color tint, SceneBlend blend, float offsetX,
                float offsetY, float angle, boolean animated) {
            this.regionName = regionName;
            this.tint = tint == null ? null : new Color(tint);
            this.blend = blend;
            this.animated = animated;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.angle = angle;
        }
    }

    /**
     * The native ACT-2 {@code TheCityScene.renderCombatRoomBg} layer order, one entry per
     * {@code renderAtlasRegionIf} call. {@link #regionName()} is the {@code cityScene/scene.atlas}
     * region name the native scene resolves.
     */
    public enum CityLayer {
        BG("mod/bg1"),
        BG_GLOW("mod/bgGlowv2"),
        BG_GLOW2("mod/bgGlowBlur"),
        BG2("mod/bg2"),
        BG2_GLOW("mod/bg2Glow"),
        FLOOR("mod/floor"),
        CEILING("mod/ceiling"),
        WALL("mod/wall"),
        CHAINS("mod/chains"),
        CHAINS_GLOW("mod/chainsGlow"),
        MG("mod/mg1"),
        MG_GLOW("mod/mg1Glow"),
        MG2("mod/mg2"),
        P1("mod/p1"),
        P2("mod/p2"),
        P3("mod/p3"),
        P4("mod/p4"),
        P5("mod/p5"),
        THRONE("mod/throne"),
        THRONE_GLOW("mod/throneGlow");

        private final String regionName;

        CityLayer(String regionName) {
            this.regionName = regionName;
        }

        /** The native {@code cityScene/scene.atlas} region name for this layer. */
        public String regionName() {
            return regionName;
        }
    }

    /**
     * The dynamic ACT-2 {@code TheCityScene} background state the native scene reads each frame. The
     * nine booleans, the {@code pillarConfig} switch, and the three native {@code Color} fields
     * mirror the private scene members. {@link #failOpen()} carries no glow/pillar/throne state and
     * WHITE tints, and is flagged so {@link #cityLayerPlan} emits exactly the always-present
     * {@code bg}/{@code floor}/{@code ceiling} base plan.
     */
    public static final class CityFlags {
        public final boolean renderAltBg;
        public final boolean renderMg;
        public final boolean renderMgGlow;
        public final boolean renderMgAlt;
        public final boolean renderWall;
        public final boolean renderChains;
        public final boolean renderThrone;
        public final boolean renderFg2;
        public final boolean darkDay;
        public final PillarConfig pillarConfig;
        public final Color overlayColor;
        public final Color whiteColor;
        public final Color yellowTint;
        private final boolean failOpen;

        private CityFlags(boolean renderAltBg, boolean renderMg, boolean renderMgGlow,
                boolean renderMgAlt, boolean renderWall, boolean renderChains, boolean renderThrone,
                boolean renderFg2, boolean darkDay, PillarConfig pillarConfig, Color overlayColor,
                Color whiteColor, Color yellowTint, boolean failOpen) {
            this.renderAltBg = renderAltBg;
            this.renderMg = renderMg;
            this.renderMgGlow = renderMgGlow;
            this.renderMgAlt = renderMgAlt;
            this.renderWall = renderWall;
            this.renderChains = renderChains;
            this.renderThrone = renderThrone;
            this.renderFg2 = renderFg2;
            this.darkDay = darkDay;
            this.pillarConfig = pillarConfig;
            this.overlayColor = overlayColor == null ? null : new Color(overlayColor);
            this.whiteColor = whiteColor == null ? null : new Color(whiteColor);
            this.yellowTint = yellowTint == null ? null : new Color(yellowTint);
            this.failOpen = failOpen;
        }

        public static CityFlags of(boolean renderAltBg, boolean renderMg, boolean renderMgGlow,
                boolean renderMgAlt, boolean renderWall, boolean renderChains, boolean renderThrone,
                boolean renderFg2, boolean darkDay, PillarConfig pillarConfig, Color overlayColor,
                Color whiteColor, Color yellowTint) {
            return new CityFlags(renderAltBg, renderMg, renderMgGlow, renderMgAlt, renderWall,
                    renderChains, renderThrone, renderFg2, darkDay,
                    pillarConfig == null ? PillarConfig.OPEN : pillarConfig, overlayColor, whiteColor,
                    yellowTint, false);
        }

        /**
         * All-false flags: the base layers only ({@code bg}/{@code floor}/{@code ceiling}), pillar
         * {@code OPEN}, and WHITE tints. Flagged so the plan is exactly the three base layers.
         */
        public static CityFlags failOpen() {
            return new CityFlags(false, false, false, false, false, false, false, false, false,
                    PillarConfig.OPEN, Color.WHITE, Color.WHITE, Color.WHITE, true);
        }
    }

    /**
     * The dynamic ACT-3 {@code TheBeyondScene} background state the native scene reads each frame.
     * The twenty booleans (alt background, the {@code mg1..4} modules, the {@code i1..5} lanterns,
     * the {@code s1..5} stalactites, the {@code f1..5} flames), the {@code columnConfig} switch, and
     * the two native {@code Color} fields mirror the private scene members. {@link #failOpen()}
     * carries no optional layer state and WHITE tints, and is flagged so {@link #beyondLayerPlan}
     * emits exactly the always-present {@code floor}/{@code ceiling}/{@code bg1} base plan.
     */
    public static final class BeyondFlags {
        public final boolean renderAltBg;
        public final boolean renderM1;
        public final boolean renderM2;
        public final boolean renderM3;
        public final boolean renderM4;
        public final boolean renderI1;
        public final boolean renderI2;
        public final boolean renderI3;
        public final boolean renderI4;
        public final boolean renderI5;
        public final boolean renderS1;
        public final boolean renderS2;
        public final boolean renderS3;
        public final boolean renderS4;
        public final boolean renderS5;
        public final boolean renderF1;
        public final boolean renderF2;
        public final boolean renderF3;
        public final boolean renderF4;
        public final boolean renderF5;
        public final ColumnConfig columnConfig;
        public final Color overlayColor;
        public final Color tmpColor;
        private final boolean failOpen;

        private BeyondFlags(boolean renderAltBg,
                boolean renderM1, boolean renderM2, boolean renderM3, boolean renderM4,
                boolean renderI1, boolean renderI2, boolean renderI3, boolean renderI4,
                boolean renderI5,
                boolean renderS1, boolean renderS2, boolean renderS3, boolean renderS4,
                boolean renderS5,
                boolean renderF1, boolean renderF2, boolean renderF3, boolean renderF4,
                boolean renderF5,
                ColumnConfig columnConfig, Color overlayColor, Color tmpColor, boolean failOpen) {
            this.renderAltBg = renderAltBg;
            this.renderM1 = renderM1;
            this.renderM2 = renderM2;
            this.renderM3 = renderM3;
            this.renderM4 = renderM4;
            this.renderI1 = renderI1;
            this.renderI2 = renderI2;
            this.renderI3 = renderI3;
            this.renderI4 = renderI4;
            this.renderI5 = renderI5;
            this.renderS1 = renderS1;
            this.renderS2 = renderS2;
            this.renderS3 = renderS3;
            this.renderS4 = renderS4;
            this.renderS5 = renderS5;
            this.renderF1 = renderF1;
            this.renderF2 = renderF2;
            this.renderF3 = renderF3;
            this.renderF4 = renderF4;
            this.renderF5 = renderF5;
            this.columnConfig = columnConfig;
            this.overlayColor = overlayColor == null ? null : new Color(overlayColor);
            this.tmpColor = tmpColor == null ? null : new Color(tmpColor);
            this.failOpen = failOpen;
        }

        /**
         * Builds the flags from the twenty native booleans, the {@code columnConfig} switch, and the
         * two native colors. A null column config is treated as {@link ColumnConfig#OPEN}.
         */
        public static BeyondFlags of(boolean renderAltBg,
                boolean renderM1, boolean renderM2, boolean renderM3, boolean renderM4,
                boolean renderI1, boolean renderI2, boolean renderI3, boolean renderI4,
                boolean renderI5,
                boolean renderS1, boolean renderS2, boolean renderS3, boolean renderS4,
                boolean renderS5,
                boolean renderF1, boolean renderF2, boolean renderF3, boolean renderF4,
                boolean renderF5,
                ColumnConfig columnConfig, Color overlayColor, Color tmpColor) {
            return new BeyondFlags(renderAltBg,
                    renderM1, renderM2, renderM3, renderM4,
                    renderI1, renderI2, renderI3, renderI4, renderI5,
                    renderS1, renderS2, renderS3, renderS4, renderS5,
                    renderF1, renderF2, renderF3, renderF4, renderF5,
                    columnConfig == null ? ColumnConfig.OPEN : columnConfig,
                    overlayColor, tmpColor, false);
        }

        /**
         * All-false flags: the base layers only ({@code floor}/{@code ceiling}/{@code bg1}),
         * {@code columnConfig} {@code OPEN}, and WHITE tints. Flagged so the plan is exactly the
         * three base layers.
         */
        public static BeyondFlags failOpen() {
            return new BeyondFlags(false,
                    false, false, false, false,
                    false, false, false, false, false,
                    false, false, false, false, false,
                    false, false, false, false, false,
                    ColumnConfig.OPEN, Color.WHITE, Color.WHITE, true);
        }
    }

    /**
     * The dynamic ACT-1 wall/ceiling selection the native scene reads each frame. {@code ceilingMods}
     * is indexed 1..6 (index 0 unused) so {@code ceilingMod(3)} is the native
     * {@code renderCeilingMod3}.
     */
    public static final class BottomFlags {
        public final boolean solidMid;
        public final boolean hollowMid;
        public final boolean leftWall;
        private final boolean[] ceilingMods;

        private BottomFlags(boolean solidMid, boolean hollowMid, boolean leftWall,
                boolean[] ceilingMods) {
            this.solidMid = solidMid;
            this.hollowMid = hollowMid;
            this.leftWall = leftWall;
            this.ceilingMods = ceilingMods;
        }

        /**
         * Builds the flags from the native three wall booleans plus 0..6 ceiling-mod booleans
         * (positions 1..6). Missing positions default to {@code false}.
         */
        public static BottomFlags of(boolean solidMid, boolean hollowMid, boolean leftWall,
                boolean... ceilingMod) {
            boolean[] mods = new boolean[7];
            if (ceilingMod != null) {
                for (int i = 0; i < 6 && i < ceilingMod.length; i++) {
                    mods[i + 1] = ceilingMod[i];
                }
            }
            return new BottomFlags(solidMid, hollowMid, leftWall, mods);
        }

        /** All-false flags: the base layers only ({@code bg}, {@code mg}, {@code ceiling}). */
        public static BottomFlags failOpen() {
            return of(false, false, false);
        }

        /** Ceiling-mod flag for native index {@code 1..6}; out-of-range reads {@code false}. */
        public boolean ceilingMod(int nativeIndex) {
            return nativeIndex >= 1 && nativeIndex <= 6 && ceilingMods[nativeIndex];
        }
    }

    /** Region resolver seam: a native region name to a live page texture plus neutral descriptor. */
    public interface BottomRegionSource {
        Sts1AtlasMaterializer.RegionTexture region(String regionName);
    }

    private static final float CELL = 64f;
    private static final float DEFAULT_WIDTH = 1920f;
    private static final float DEFAULT_HEIGHT = 1080f;

    private static Variant configured = Variant.OFF;
    private static long artDrawCount;
    private static long nativeFallbackCount;

    private BackgroundRenderGate() {}

    public static void setVariant(Variant variant) {
        configured = variant == null ? Variant.OFF : variant;
    }

    public static Variant variant() {
        return configured;
    }

    /**
     * Draw the selected background variant in the pre-native scene Prefix. Draw and suppression are
     * decoupled but ordered: drawing happens whenever a non-{@code off} variant is selected and no
     * panic is active (that is the variant's job); whether the native background is suppressed is a
     * separate decision in {@link #suppressNativeBackground()}, and the Prefix only suppresses when
     * this draw actually painted.
     *
     * <p>Returns {@code true} only when a variant was painted this frame. Returns {@code false} (and
     * draws nothing) when the batch is null, the variant is OFF, a panic is active, or the
     * white-square texture is unavailable. Any {@code Throwable} fails open: the color is restored
     * in {@code finally}, no quad is recorded, {@code false} is returned, and the native background
     * still runs. There is no post-native draw path.
     */
    public static boolean renderSelectedVariant(SpriteBatch sb) {
        if (sb == null) return false;
        if (configured == Variant.OFF) return false;
        // The real ACT-1 background is layer-based and scene-scoped; the scene-agnostic synthetic
        // path never claims it. Use renderSelectedVariant(SpriteBatch, Object) for BOTTOM.
        if (configured == Variant.BOTTOM) return false;
        // The real ACT-2 background is likewise layer-based and scene-scoped.
        if (configured == Variant.CITY) return false;
        // The real ACT-3 background is likewise layer-based and scene-scoped.
        if (configured == Variant.BEYOND) return false;
        // The real ACT-4 background is likewise atlas-region based and scene-scoped.
        if (configured == Variant.ENDING) return false;
        if (artframework.sts1.PresentSafety.isPanic()) return false;
        Texture texture = com.megacrit.cardcrawl.helpers.ImageMaster.WHITE_SQUARE_IMG;
        if (texture == null) return false;
        Color prev;
        try {
            prev = sb.getColor();
        } catch (Throwable ignored) {
            return false;
        }
        int quads = 0;
        boolean painted = false;
        try {
            quads = drawConfiguredVariant(sb, texture);
            painted = quads > 0;
        } catch (Throwable ignored) {
            painted = false;
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
        if (painted) {
            recordArtDraw(quads);
            BackgroundOnlyGate.recordBackgroundDraw();
        }
        return painted;
    }

    /**
     * Scene-scoped pre-native entry used by all four {@code renderCombatRoomBg} Prefixes. When the
     * {@code bottom} variant is selected this draws the real ACT-1 {@code TheBottomScene} background
     * layers from the live scene instance; a scene that is not {@code TheBottomScene}
     * (City/Beyond/Ending) fails open to native. Every other variant keeps the previous
     * scene-agnostic synthetic behavior, and a null instance (the patch tests' synthetic call shape)
     * falls through to it unchanged.
     */
    public static boolean renderSelectedVariant(SpriteBatch sb, Object sceneInstance) {
        if (configured == Variant.BOTTOM) {
            return renderBottomBackground(sb, sceneInstance);
        }
        if (configured == Variant.CITY) {
            return renderCityBackground(sb, sceneInstance);
        }
        if (configured == Variant.BEYOND) {
            return renderBeyondBackground(sb, sceneInstance);
        }
        if (configured == Variant.ENDING) {
            return renderEndingBackground(sb, sceneInstance);
        }
        return renderSelectedVariant(sb);
    }

    /**
     * Production ACT-1 entry: draws the live {@code TheBottomScene} background layers read off the
     * scene instance. The dynamic wall/ceiling flags and the scene's own {@code AbstractScene.atlas}
     * are read reflectively; any read failure fails open to {@link BottomFlags#failOpen()} and the
     * three base layers. A null batch, an active panic, or any non-{@code TheBottomScene} instance
     * draws nothing.
     */
    public static boolean renderBottomBackground(SpriteBatch sb, Object sceneInstance) {
        try {
            if (sb == null) return false;
            if (artframework.sts1.PresentSafety.isPanic()) return false;
            if (!(sceneInstance instanceof com.megacrit.cardcrawl.scenes.TheBottomScene)) return false;
            BottomFlags flags = readBottomFlags(sceneInstance);
            BottomRegionSource source = new InstanceSceneRegionSource(readSceneAtlas(sceneInstance));
            return renderBottomBackground(sb, flags, source);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True only when the native scene background must be suppressed: a non-off variant is selected,
     * no panic is active, the white-square texture is available, and the background family is
     * explicitly filtered. Decision only — no draw. Deliberately independent of {@code
     * Sts1RenderBoundary} capability.
     */
    public static boolean suppressNativeBackground() {
        if (configured == Variant.OFF) return false;
        if (artframework.sts1.PresentSafety.isPanic()) return false;
        if (com.megacrit.cardcrawl.helpers.ImageMaster.WHITE_SQUARE_IMG == null) return false;
        return BackgroundOnlyGate.isActive()
                || NativeRenderBridge.filterScope().isFiltered(BACKGROUND_FAMILY);
    }

    /**
     * Probe-facing suppression decision. Equal to {@link #suppressNativeBackground()}; the probe key
     * {@code artOwnsBackgroundRequested} reports this same decision.
     */
    public static boolean artOwnsBackground() {
        return suppressNativeBackground();
    }

    public static void recordNativeSuppression() {
        BackgroundOnlyGate.recordBackgroundSuppression();
    }

    /**
     * Package-visible decision seam with no draw side effect. Tests and future host wiring can ask
     * whether the gate currently owns the background without needing a tolerating {@code
     * SpriteBatch}. Equivalent to {@link #suppressNativeBackground()}.
     */
    static boolean ownsBackgroundDecision() {
        return suppressNativeBackground();
    }

    public static boolean available() {
        return true;
    }

    /** Draw the selected variant and return the number of quads submitted. */
    private static int drawConfiguredVariant(SpriteBatch sb, Texture texture) {
        if (configured == Variant.SOLID) return drawSolid(sb, texture);
        if (configured == Variant.CHECKER) return drawChecker(sb, texture);
        if (configured == Variant.GRID) return drawGrid(sb, texture);
        return 0;
    }

    private static int drawSolid(SpriteBatch sb, Texture texture) {
        sb.setColor(0.07f, 0.08f, 0.11f, 1f);
        sb.draw(texture, 0f, 0f, screenWidth(), screenHeight());
        return 1;
    }

    private static int checkerCols() {
        return (int) Math.ceil(screenWidth() / CELL);
    }

    private static int checkerRows() {
        return (int) Math.ceil(screenHeight() / CELL);
    }

    private static int drawChecker(SpriteBatch sb, Texture texture) {
        int cols = checkerCols();
        int rows = checkerRows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (((row + col) & 1) == 0) {
                    sb.setColor(0.06f, 0.07f, 0.10f, 1f);
                } else {
                    sb.setColor(0.16f, 0.17f, 0.22f, 1f);
                }
                sb.draw(texture, col * CELL, row * CELL, CELL, CELL);
            }
        }
        return cols * rows;
    }

    private static int drawGrid(SpriteBatch sb, Texture texture) {
        float width = screenWidth();
        float height = screenHeight();
        sb.setColor(0.07f, 0.08f, 0.11f, 1f);
        sb.draw(texture, 0f, 0f, width, height);
        sb.setColor(0.35f, 0.38f, 0.45f, 0.6f);
        for (float x = CELL; x < width; x += CELL) {
            sb.draw(texture, x, 0f, 1f, height);
        }
        for (float y = CELL; y < height; y += CELL) {
            sb.draw(texture, 0f, y, width, 1f);
        }
        return 1 + checkerCols() + checkerRows();
    }

    private static float screenWidth() {
        try {
            float w = com.megacrit.cardcrawl.core.Settings.WIDTH;
            return w > 0f ? w : DEFAULT_WIDTH;
        } catch (Throwable ignored) {
            return DEFAULT_WIDTH;
        }
    }

    private static float screenHeight() {
        try {
            float h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
            return h > 0f ? h : DEFAULT_HEIGHT;
        } catch (Throwable ignored) {
            return DEFAULT_HEIGHT;
        }
    }

    /**
     * The native ACT-1 layer plan in exact {@code renderCombatRoomBg} order: always {@code bg} and
     * {@code mg}, then the flagged walls (solid, hollow, left), then always {@code ceiling}, then the
     * flagged ceiling mods in native index order. Pure — no draw, no state. A null flags value is
     * treated as {@link BottomFlags#failOpen()}.
     */
    public static List<BottomLayer> bottomLayerPlan(BottomFlags flags) {
        BottomFlags f = flags != null ? flags : BottomFlags.failOpen();
        List<BottomLayer> plan = new ArrayList<BottomLayer>(12);
        plan.add(BottomLayer.BG);
        plan.add(BottomLayer.MG);
        if (f.solidMid) plan.add(BottomLayer.SOLID_WALL);
        if (f.hollowMid) plan.add(BottomLayer.HOLLOW_WALL);
        if (f.leftWall) plan.add(BottomLayer.LEFT_WALL);
        plan.add(BottomLayer.CEILING);
        if (f.ceilingMod(1)) plan.add(BottomLayer.CEILING_MOD1);
        if (f.ceilingMod(2)) plan.add(BottomLayer.CEILING_MOD2);
        if (f.ceilingMod(3)) plan.add(BottomLayer.CEILING_MOD3);
        if (f.ceilingMod(4)) plan.add(BottomLayer.CEILING_MOD4);
        if (f.ceilingMod(5)) plan.add(BottomLayer.CEILING_MOD5);
        if (f.ceilingMod(6)) plan.add(BottomLayer.CEILING_MOD6);
        return plan;
    }

    /** True when a base layer's region resolves to a drawable page texture + descriptor. */
    private static boolean resolves(BottomRegionSource source, BottomLayer layer) {
        return resolves(source, layer.regionName());
    }

    /** True when a named region resolves to a drawable page texture + descriptor. */
    private static boolean resolves(BottomRegionSource source, String regionName) {
        try {
            return drawable(source.region(regionName));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True when a resolved region carries both a live page texture and a valid descriptor. The draw
     * loops test this predicate directly on the region they are about to submit, so it must NOT
     * swallow a throwing source: there the enclosing fail-open catch fails the whole draw rather
     * than skipping one layer.
     */
    private static boolean drawable(Sts1AtlasMaterializer.RegionTexture resolved) {
        return resolved != null && resolved.region != null && resolved.texture != null;
    }

    /**
     * Draws the planned native ACT-1 layers through the region source with the exact native
     * pixel-rect submission (see {@link #drawSceneLayer}). Tint mirrors native: WHITE before
     * {@code bg}/{@code mg}/{@code ceiling} and the hollow/left walls, GRAY before the solid wall
     * only while {@code hollowMid && (solidMid || leftWall)}, WHITE before the hollow wall. A layer
     * whose region resolves {@code null} is skipped. {@code recordArtDraw(n)} records the regions
     * actually submitted and {@link BackgroundOnlyGate#recordBackgroundDraw()} runs when anything
     * painted. Returns {@code true} iff at least one layer was painted. Any {@code Throwable} fails
     * open (returns {@code false} and records no counter). The previous batch color is restored.
     */
    public static boolean renderBottomBackground(SpriteBatch sb, BottomFlags flags,
            BottomRegionSource source) {
        if (sb == null || source == null) return false;
        Color prev;
        try {
            prev = sb.getColor();
        } catch (Throwable ignored) {
            return false;
        }
        int submitted = 0;
        boolean painted;
        try {
            BottomFlags f = flags != null ? flags : BottomFlags.failOpen();
            List<BottomLayer> plan = bottomLayerPlan(f);
            // MEDIUM review fix: suppression is ALL-OR-NOTHING. If any always-present base layer
            // (bg/mg/ceiling) does not resolve, drawing only the other layers then suppressing native
            // would leave a visibly incomplete room with no native fallback. Require the three base
            // layers to resolve BEFORE drawing anything; otherwise paint nothing and let native
            // continue (painted=false → suppression does not engage).
            if (resolves(source, BottomLayer.BG)
                    && resolves(source, BottomLayer.MG)
                    && resolves(source, BottomLayer.CEILING)) {
                for (BottomLayer layer : plan) {
                    Sts1AtlasMaterializer.RegionTexture resolved = source.region(layer.regionName());
                    if (!drawable(resolved)) {
                        continue;
                    }
                    applyBottomTint(sb, layer, f);
                    drawSceneLayer(sb, resolved);
                    submitted++;
                }
                painted = submitted > 0;
            } else {
                painted = false;
            }
        } catch (Throwable ignored) {
            painted = false;
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
        if (painted) {
            recordArtDraw(submitted);
            BackgroundOnlyGate.recordBackgroundDraw();
        }
        return painted;
    }

    /**
     * The native ACT-2 {@code TheCityScene.renderCombatRoomBg} layer plan in exact call order, one
     * {@link SceneLayerSpec} per {@code renderAtlasRegionIf} with the active tint and additive/normal
     * blend state. Pure — no draw, no state. A null flags value is treated as
     * {@link CityFlags#failOpen()}; a fail-open flags value yields exactly the always-present
     * {@code bg}/{@code floor}/{@code ceiling} base layers.
     */
    public static List<SceneLayerSpec> cityLayerPlan(CityFlags flags, long nowMillis) {
        CityFlags f = flags != null ? flags : CityFlags.failOpen();
        if (f.failOpen) {
            return cityBasePlan(f);
        }
        Color overlay = colorOrWhite(f.overlayColor);
        Color white = colorOrWhite(f.whiteColor);
        Color yellow = colorOrWhite(f.yellowTint);
        List<SceneLayerSpec> plan = new ArrayList<SceneLayerSpec>(24);
        Color bgTarget = f.darkDay ? Color.WHITE : overlay;
        plan.add(spec(CityLayer.BG, overlay, SceneBlend.NORMAL));
        plan.add(spec(CityLayer.BG_GLOW, overlay, SceneBlend.ADDITIVE));
        if (f.darkDay) {
            plan.add(spec(CityLayer.BG_GLOW2, Color.WHITE, SceneBlend.ADDITIVE));
            plan.add(spec(CityLayer.BG_GLOW2, Color.WHITE, SceneBlend.ADDITIVE));
        }
        if (f.renderAltBg) {
            plan.add(spec(CityLayer.BG2, bgTarget, SceneBlend.NORMAL));
            plan.add(spec(CityLayer.BG2_GLOW, bgTarget, SceneBlend.ADDITIVE));
        }
        plan.add(spec(CityLayer.FLOOR, overlay, SceneBlend.NORMAL));
        plan.add(spec(CityLayer.CEILING, overlay, SceneBlend.NORMAL));
        if (f.renderWall) {
            plan.add(spec(CityLayer.WALL, overlay, SceneBlend.NORMAL));
        }
        if (f.renderChains) {
            plan.add(spec(CityLayer.CHAINS, overlay, SceneBlend.NORMAL));
            Color chainsGlow = new Color(white);
            chainsGlow.a = com.badlogic.gdx.math.MathUtils.cosDeg(nowMillis % 360L) / 10.0f + 0.9f;
            plan.add(spec(CityLayer.CHAINS_GLOW, chainsGlow, SceneBlend.ADDITIVE));
            plan.add(spec(CityLayer.CHAINS_GLOW, chainsGlow, SceneBlend.ADDITIVE));
        }
        if (f.renderMg) {
            plan.add(spec(CityLayer.MG, overlay, SceneBlend.NORMAL));
        }
        if (f.renderMg && f.renderMgGlow) {
            Color mgGlow = new Color(white);
            mgGlow.a = com.badlogic.gdx.math.MathUtils.cosDeg((nowMillis / 10L) % 360L) / 2.0f + 0.5f;
            plan.add(spec(CityLayer.MG_GLOW, mgGlow, SceneBlend.ADDITIVE));
            plan.add(spec(CityLayer.MG_GLOW, mgGlow, SceneBlend.ADDITIVE));
        } else if (f.renderMg) {
            plan.add(spec(CityLayer.MG_GLOW, overlay, SceneBlend.ADDITIVE));
        }
        Color afterMg = f.renderMgGlow ? yellow : overlay;
        if (f.renderMgAlt) {
            plan.add(spec(CityLayer.MG2, afterMg, SceneBlend.NORMAL));
        }
        addPillars(plan, f.pillarConfig, afterMg);
        if (f.renderThrone) {
            plan.add(spec(CityLayer.THRONE, afterMg, SceneBlend.NORMAL));
            plan.add(spec(CityLayer.THRONE_GLOW, afterMg, SceneBlend.ADDITIVE));
        }
        return plan;
    }

    /** The fail-open base plan: exactly {@code bg}, {@code floor}, {@code ceiling}, all NORMAL. */
    private static List<SceneLayerSpec> cityBasePlan(CityFlags f) {
        Color overlay = colorOrWhite(f.overlayColor);
        List<SceneLayerSpec> plan = new ArrayList<SceneLayerSpec>(3);
        plan.add(spec(CityLayer.BG, overlay, SceneBlend.NORMAL));
        plan.add(spec(CityLayer.FLOOR, overlay, SceneBlend.NORMAL));
        plan.add(spec(CityLayer.CEILING, overlay, SceneBlend.NORMAL));
        return plan;
    }

    private static void addPillars(List<SceneLayerSpec> plan, PillarConfig config, Color carried) {
        if (config == null) config = PillarConfig.OPEN;
        switch (config) {
            case SIDES_ONLY:
                plan.add(spec(CityLayer.P1, carried, SceneBlend.NORMAL));
                plan.add(spec(CityLayer.P5, carried, SceneBlend.NORMAL));
                break;
            case FULL:
                plan.add(spec(CityLayer.P1, carried, SceneBlend.NORMAL));
                plan.add(spec(CityLayer.P2, carried, SceneBlend.NORMAL));
                plan.add(spec(CityLayer.P3, carried, SceneBlend.NORMAL));
                plan.add(spec(CityLayer.P4, carried, SceneBlend.NORMAL));
                plan.add(spec(CityLayer.P5, carried, SceneBlend.NORMAL));
                break;
            case LEFT_1:
                plan.add(spec(CityLayer.P1, carried, SceneBlend.NORMAL));
                break;
            case LEFT_2:
                plan.add(spec(CityLayer.P1, carried, SceneBlend.NORMAL));
                plan.add(spec(CityLayer.P2, carried, SceneBlend.NORMAL));
                break;
            case OPEN:
            default:
                break;
        }
    }

    private static SceneLayerSpec spec(CityLayer layer, Color tint, SceneBlend blend) {
        return new SceneLayerSpec(layer.regionName(), tint, blend);
    }

    private static Color colorOrWhite(Color color) {
        return color != null ? color : Color.WHITE;
    }

    /**
     * Production ACT-2 entry: draws the live {@code TheCityScene} background layers read off the
     * scene instance, with {@code time} from {@code System.currentTimeMillis()}. A null batch, an
     * active panic, or any non-{@code TheCityScene} instance draws nothing; any reflective read
     * failure fails open to {@link CityFlags#failOpen()} and the base plan.
     */
    public static boolean renderCityBackground(SpriteBatch sb, Object sceneInstance) {
        try {
            if (sb == null) return false;
            if (artframework.sts1.PresentSafety.isPanic()) return false;
            if (!(sceneInstance instanceof com.megacrit.cardcrawl.scenes.TheCityScene)) return false;
            CityFlags flags = readCityFlags(sceneInstance);
            BottomRegionSource source = new InstanceSceneRegionSource(readSceneAtlas(sceneInstance));
            return renderCityBackground(sb, flags, source, System.currentTimeMillis());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Draws the planned native ACT-2 layers through the region source with the exact native
     * pixel-rect submission (see {@link #drawSceneLayer}). Each layer applies its planned tint and
     * switches the blend function only when the additive/normal state changes; the batch is left in
     * the native NORMAL end state. Suppression is ALL-OR-NOTHING: {@code mod/bg1},
     * {@code mod/floor}, and {@code mod/ceiling} must all resolve or nothing is painted and no
     * counter is recorded. Returns {@code true} iff at least one layer was painted. Any
     * {@code Throwable} fails open (returns {@code false}, records no counter). The previous batch
     * color is restored.
     */
    public static boolean renderCityBackground(SpriteBatch sb, CityFlags flags,
            BottomRegionSource source, long nowMillis) {
        if (sb == null || source == null) return false;
        Color prev;
        try {
            prev = sb.getColor();
        } catch (Throwable ignored) {
            return false;
        }
        int submitted = 0;
        boolean painted;
        try {
            CityFlags f = flags != null ? flags : CityFlags.failOpen();
            List<SceneLayerSpec> plan = cityLayerPlan(f, nowMillis);
            if (resolves(source, CityLayer.BG.regionName())
                    && resolves(source, CityLayer.FLOOR.regionName())
                    && resolves(source, CityLayer.CEILING.regionName())) {
                SceneBlend current = SceneBlend.NORMAL;
                for (SceneLayerSpec layer : plan) {
                    if (layer.blend != current) {
                        sb.setBlendFunction(770, layer.blend == SceneBlend.ADDITIVE ? 1 : 771);
                        current = layer.blend;
                    }
                    Sts1AtlasMaterializer.RegionTexture resolved = source.region(layer.regionName);
                    if (!drawable(resolved)) {
                        continue;
                    }
                    if (layer.tint != null) {
                        sb.setColor(layer.tint);
                    }
                    drawSceneLayer(sb, resolved);
                    submitted++;
                }
                // Native always returns the blend function to NORMAL after the throne glow block.
                if (current != SceneBlend.NORMAL) {
                    sb.setBlendFunction(770, 771);
                }
                painted = submitted > 0;
            } else {
                painted = false;
            }
        } catch (Throwable ignored) {
            painted = false;
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
        if (painted) {
            recordArtDraw(submitted);
            BackgroundOnlyGate.recordBackgroundDraw();
        }
        return painted;
    }

    /**
     * The native ACT-3 {@code TheBeyondScene.renderCombatRoomBg} layer plan in exact call order, one
     * {@link SceneLayerSpec} per {@code renderAtlasRegionIf} with the active tint, additive/normal
     * blend state, and (for the {@code f} flames) the rotating-overload transform. Pure — no draw,
     * no state; {@code nowMillis} is the frame clock the native {@code System.currentTimeMillis()}
     * read produces.
     *
     * <p>The native sequence is a two-pass draw: an overlay pass ({@code floor}, {@code ceiling},
     * {@code bg1}, the alt {@code bg2}, the {@code mg2}/{@code mg1}/{@code mg3}/{@code mg4} stack,
     * the {@code columnConfig} columns, then {@code s1..s5}) with {@code overlayColor.a} forced to
     * {@code 1.0}; an ADDITIVE repeat of that set except {@code floor}/{@code ceiling} with the
     * original {@code overlayColor} alpha; the {@code i1..i5} lantern pass back at the forced
     * {@code 1.0}; and the rotating {@code f1..f5} flames tinted from {@code tmpColor} blended with
     * {@code overlayColor}. A null flags value is treated as {@link BeyondFlags#failOpen()}; a
     * fail-open flags value yields exactly the always-present
     * {@code floor}/{@code ceiling}/{@code bg1} base plan.
     */
    public static List<SceneLayerSpec> beyondLayerPlan(BeyondFlags flags, long nowMillis) {
        BeyondFlags f = flags != null ? flags : BeyondFlags.failOpen();
        if (f.failOpen) {
            return beyondBasePlan(f);
        }
        Color overlay = colorOrWhite(f.overlayColor);
        Color tmp = colorOrWhite(f.tmpColor);
        // The overlay pass forces a == 1.0; the additive pass reads the ORIGINAL alpha.
        Color overlayOpaque = new Color(overlay);
        overlayOpaque.a = 1f;
        Color flame = new Color(tmp);
        flame.r = (1f + overlay.r) / 2f;
        flame.g = (1f + overlay.g) / 2f;
        flame.b = (1f + overlay.b) / 2f;

        List<SceneLayerSpec> plan = new ArrayList<SceneLayerSpec>(40);
        plan.add(spec(BeyondLayer.FLOOR, overlayOpaque, SceneBlend.NORMAL));
        plan.add(spec(BeyondLayer.CEILING, overlayOpaque, SceneBlend.NORMAL));
        addBeyondWallPass(plan, f, overlayOpaque, SceneBlend.NORMAL);
        addBeyondWallPass(plan, f, overlay, SceneBlend.ADDITIVE);
        // The i pass runs at the restored forced-1.0 overlay alpha.
        addBeyondLayer(plan, f.renderI1, BeyondLayer.I1, overlayOpaque, SceneBlend.NORMAL);
        addBeyondLayer(plan, f.renderI2, BeyondLayer.I2, overlayOpaque, SceneBlend.NORMAL);
        addBeyondLayer(plan, f.renderI3, BeyondLayer.I3, overlayOpaque, SceneBlend.NORMAL);
        addBeyondLayer(plan, f.renderI4, BeyondLayer.I4, overlayOpaque, SceneBlend.NORMAL);
        addBeyondLayer(plan, f.renderI5, BeyondLayer.I5, overlayOpaque, SceneBlend.NORMAL);
        // The rotating f flames use the tmpColor/overlayColor blend and the per-frame transforms.
        float scale = com.megacrit.cardcrawl.core.Settings.scale;
        float xScale = com.megacrit.cardcrawl.core.Settings.xScale;
        if (f.renderF1) {
            float angle = com.badlogic.gdx.math.MathUtils.cosDeg((nowMillis + 180L) / 180L % 360L);
            plan.add(new SceneLayerSpec(BeyondLayer.F1.regionName(), flame, SceneBlend.NORMAL,
                    angle * 40f * xScale,
                    com.badlogic.gdx.math.MathUtils.cosDeg((nowMillis + 500L) / 72L % 360L)
                            * 20f * scale,
                    angle));
        }
        if (f.renderF2) {
            plan.add(new SceneLayerSpec(BeyondLayer.F2.regionName(), flame, SceneBlend.NORMAL,
                    com.badlogic.gdx.math.MathUtils.cosDeg((nowMillis + 91723L) / 72L % 360L) * 20f,
                    0f,
                    (float) (nowMillis / 120L % 360L)));
        }
        if (f.renderF3) {
            plan.add(new SceneLayerSpec(BeyondLayer.F3.regionName(), flame, SceneBlend.NORMAL,
                    -80f * scale,
                    com.badlogic.gdx.math.MathUtils.cosDeg((float) (nowMillis + 73L)) * 10f
                            - 90f * scale,
                    (float) (nowMillis / 1000L % 360L) * 2f));
        }
        if (f.renderF4) {
            plan.add(new SceneLayerSpec(BeyondLayer.F4.regionName(), flame, SceneBlend.NORMAL,
                    0f,
                    com.badlogic.gdx.math.MathUtils.cosDeg((nowMillis + 4442L) / 20L % 360L)
                            * 30f * scale,
                    com.badlogic.gdx.math.MathUtils.cosDeg((nowMillis + 4442L) / 10L % 360L)
                            * 20f));
        }
        if (f.renderF5) {
            plan.add(new SceneLayerSpec(BeyondLayer.F5.regionName(), flame, SceneBlend.NORMAL,
                    0f,
                    com.badlogic.gdx.math.MathUtils.cosDeg(nowMillis / 48L % 360L) * 20f,
                    0f));
        }
        return plan;
    }

    /** The fail-open base plan: exactly {@code floor}, {@code ceiling}, {@code bg1}, all NORMAL. */
    private static List<SceneLayerSpec> beyondBasePlan(BeyondFlags f) {
        Color overlay = colorOrWhite(f.overlayColor);
        List<SceneLayerSpec> plan = new ArrayList<SceneLayerSpec>(3);
        plan.add(spec(BeyondLayer.FLOOR, overlay, SceneBlend.NORMAL));
        plan.add(spec(BeyondLayer.CEILING, overlay, SceneBlend.NORMAL));
        plan.add(spec(BeyondLayer.BG1, overlay, SceneBlend.NORMAL));
        return plan;
    }

    /**
     * The shared two-pass wall/module/column/sprite stack: {@code bg1}, the flagged alt {@code bg2},
     * the {@code mg2}/{@code mg1}/{@code mg3}/{@code mg4} order, the {@code columnConfig} columns,
     * then {@code s1..s5}. Emitted once per pass with the pass tint and blend.
     */
    private static void addBeyondWallPass(List<SceneLayerSpec> plan, BeyondFlags f, Color tint,
            SceneBlend blend) {
        plan.add(spec(BeyondLayer.BG1, tint, blend));
        if (f.renderAltBg) plan.add(spec(BeyondLayer.BG2, tint, blend));
        if (f.renderM2) plan.add(spec(BeyondLayer.MG2, tint, blend));
        if (f.renderM1) plan.add(spec(BeyondLayer.MG1, tint, blend));
        if (f.renderM3) plan.add(spec(BeyondLayer.MG3, tint, blend));
        if (f.renderM4) plan.add(spec(BeyondLayer.MG4, tint, blend));
        addBeyondColumns(plan, f.columnConfig, tint, blend);
        addBeyondLayer(plan, f.renderS1, BeyondLayer.S1, tint, blend);
        addBeyondLayer(plan, f.renderS2, BeyondLayer.S2, tint, blend);
        addBeyondLayer(plan, f.renderS3, BeyondLayer.S3, tint, blend);
        addBeyondLayer(plan, f.renderS4, BeyondLayer.S4, tint, blend);
        addBeyondLayer(plan, f.renderS5, BeyondLayer.S5, tint, blend);
    }

    /** The {@code columnConfig} column subset, identical in both native passes. */
    private static void addBeyondColumns(List<SceneLayerSpec> plan, ColumnConfig config, Color tint,
            SceneBlend blend) {
        ColumnConfig c = config == null ? ColumnConfig.OPEN : config;
        switch (c) {
            case SMALL_ONLY:
                plan.add(spec(BeyondLayer.C1, tint, blend));
                plan.add(spec(BeyondLayer.C4, tint, blend));
                break;
            case SMALL_PLUS_LEFT:
                plan.add(spec(BeyondLayer.C1, tint, blend));
                plan.add(spec(BeyondLayer.C2, tint, blend));
                plan.add(spec(BeyondLayer.C4, tint, blend));
                break;
            case SMALL_PLUS_RIGHT:
                plan.add(spec(BeyondLayer.C1, tint, blend));
                plan.add(spec(BeyondLayer.C3, tint, blend));
                plan.add(spec(BeyondLayer.C4, tint, blend));
                break;
            case OPEN:
            default:
                break;
        }
    }

    private static void addBeyondLayer(List<SceneLayerSpec> plan, boolean enabled, BeyondLayer layer,
            Color tint, SceneBlend blend) {
        if (enabled) {
            plan.add(spec(layer, tint, blend));
        }
    }

    private static SceneLayerSpec spec(BeyondLayer layer, Color tint, SceneBlend blend) {
        return new SceneLayerSpec(layer.regionName(), tint, blend);
    }

    /**
     * Production ACT-3 entry: draws the live {@code TheBeyondScene} background layers read off the
     * scene instance, with {@code now} from {@code System.currentTimeMillis()} read once. A null
     * batch, an active panic, or any non-{@code TheBeyondScene} instance draws nothing; any
     * reflective read failure fails open to {@link BeyondFlags#failOpen()} and the base plan.
     */
    public static boolean renderBeyondBackground(SpriteBatch sb, Object sceneInstance) {
        try {
            if (sb == null) return false;
            if (artframework.sts1.PresentSafety.isPanic()) return false;
            if (!(sceneInstance instanceof com.megacrit.cardcrawl.scenes.TheBeyondScene)) return false;
            BeyondFlags flags = readBeyondFlags(sceneInstance);
            BottomRegionSource source = new InstanceSceneRegionSource(readSceneAtlas(sceneInstance));
            return renderBeyondBackground(sb, flags, source, System.currentTimeMillis());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Draws the planned native ACT-3 layers through the region source. Each layer applies its
     * planned tint and switches the blend function only when the additive/normal state changes; the
     * batch is left in the native NORMAL end state. Suppression is ALL-OR-NOTHING: {@code mod/floor},
     * {@code mod/ceiling}, and {@code mod/bg1} must all resolve or nothing is painted and no counter
     * is recorded. The animated {@code f} layers use the rotating overload (see
     * {@link #drawAnimatedSceneLayer}); every other layer uses the shared plain submission. Returns
     * {@code true} iff at least one layer was painted. Any {@code Throwable} fails open (returns
     * {@code false}, records no counter). The previous batch color is restored.
     */
    public static boolean renderBeyondBackground(SpriteBatch sb, BeyondFlags flags,
            BottomRegionSource source, long nowMillis) {
        if (sb == null || source == null) return false;
        Color prev;
        try {
            prev = sb.getColor();
        } catch (Throwable ignored) {
            return false;
        }
        int submitted = 0;
        boolean painted;
        try {
            BeyondFlags f = flags != null ? flags : BeyondFlags.failOpen();
            List<SceneLayerSpec> plan = beyondLayerPlan(f, nowMillis);
            if (resolves(source, BeyondLayer.FLOOR.regionName())
                    && resolves(source, BeyondLayer.CEILING.regionName())
                    && resolves(source, BeyondLayer.BG1.regionName())) {
                SceneBlend current = SceneBlend.NORMAL;
                for (SceneLayerSpec layer : plan) {
                    if (layer.blend != current) {
                        sb.setBlendFunction(770, layer.blend == SceneBlend.ADDITIVE ? 1 : 771);
                        current = layer.blend;
                    }
                    Sts1AtlasMaterializer.RegionTexture resolved = source.region(layer.regionName);
                    if (!drawable(resolved)) {
                        continue;
                    }
                    if (layer.tint != null) {
                        sb.setColor(layer.tint);
                    }
                    if (layer.animated) {
                        drawAnimatedSceneLayer(sb, resolved, layer.offsetX, layer.offsetY,
                                layer.angle);
                    } else {
                        drawSceneLayer(sb, resolved);
                    }
                    submitted++;
                }
                // Native always returns the blend function to NORMAL after the additive pass.
                if (current != SceneBlend.NORMAL) {
                    sb.setBlendFunction(770, 771);
                }
                painted = submitted > 0;
            } else {
                painted = false;
            }
        } catch (Throwable ignored) {
            painted = false;
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
        if (painted) {
            recordArtDraw(submitted);
            BackgroundOnlyGate.recordBackgroundDraw();
        }
        return painted;
    }

    /**
     * The native ACT-4 {@code TheEndingScene.renderCombatRoomBg} plan. The whole native method is
     * {@code sb.setColor(Color.WHITE); this.renderAtlasRegionIf(sb, this.bg, true);} — exactly ONE
     * {@code endingScene/scene.atlas} region ({@code bg}) with a WHITE tint and no blend change. Pure
     * — no draw, no state. The atlas's {@code campfire}/{@code event} regions belong to the
     * non-combat branches and are deliberately NOT planned (deferred).
     */
    public static List<SceneLayerSpec> endingLayerPlan() {
        List<SceneLayerSpec> plan = new ArrayList<SceneLayerSpec>(1);
        plan.add(new SceneLayerSpec("bg", Color.WHITE, SceneBlend.NORMAL));
        return plan;
    }

    /**
     * Production ACT-4 entry: draws the live {@code TheEndingScene} combat background region read off
     * the scene instance. A null batch, an active panic, or any non-{@code TheEndingScene} instance
     * draws nothing. Regions come from the scene's own atlas, per instance, so they cannot go stale
     * across act swaps. Any {@code Throwable} fails open.
     */
    public static boolean renderEndingBackground(SpriteBatch sb, Object sceneInstance) {
        try {
            if (sb == null) return false;
            if (artframework.sts1.PresentSafety.isPanic()) return false;
            if (!(sceneInstance instanceof com.megacrit.cardcrawl.scenes.TheEndingScene)) return false;
            BottomRegionSource source = new InstanceSceneRegionSource(readSceneAtlas(sceneInstance));
            return renderEndingBackground(sb, source);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Draws exactly the {@link #endingLayerPlan()} regions through the region source with the shared
     * plain native pixel-rect submission (see {@link #drawSceneLayer}). Native issues no
     * {@code setBlendFunction} call, so the batch blend is left untouched. Suppression is
     * ALL-OR-NOTHING: {@code bg} must resolve before anything is submitted, otherwise nothing is
     * painted and no counter/evidence is recorded. Returns {@code true} iff the region painted. Any
     * {@code Throwable} fails open (returns {@code false}, records no counter). The previous batch
     * color is restored.
     */
    public static boolean renderEndingBackground(SpriteBatch sb, BottomRegionSource source) {
        if (sb == null || source == null) return false;
        Color prev;
        try {
            prev = sb.getColor();
        } catch (Throwable ignored) {
            return false;
        }
        int submitted = 0;
        boolean painted;
        try {
            List<SceneLayerSpec> plan = endingLayerPlan();
            if (resolves(source, "bg")) {
                for (SceneLayerSpec layer : plan) {
                    Sts1AtlasMaterializer.RegionTexture resolved = source.region(layer.regionName);
                    if (!drawable(resolved)) {
                        continue;
                    }
                    if (layer.tint != null) {
                        sb.setColor(layer.tint);
                    }
                    drawSceneLayer(sb, resolved);
                    submitted++;
                }
                painted = submitted > 0;
            } else {
                painted = false;
            }
        } catch (Throwable ignored) {
            painted = false;
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
        if (painted) {
            recordArtDraw(submitted);
            BackgroundOnlyGate.recordBackgroundDraw();
        }
        return painted;
    }

    private static void applyBottomTint(SpriteBatch sb, BottomLayer layer, BottomFlags flags) {
        if (layer == BottomLayer.SOLID_WALL) {
            if (flags.hollowMid && (flags.solidMid || flags.leftWall)) {
                sb.setColor(Color.GRAY);
            } else {
                sb.setColor(Color.WHITE);
            }
            return;
        }
        sb.setColor(Color.WHITE);
    }

    /**
     * The exact native {@code AbstractScene.renderAtlasRegionIf} submission: the 16-arg pixel-rect
     * overload with origin {@code (0,0)}, the packed page size, the page source rect, and the
     * three-branch {@code Settings} scale selection (four-by-three / letterbox / normal). This is
     * NOT a UV blit. Shared by the ACT-1 Bottom and ACT-2 City layer draws; the caller is
     * responsible for the tint and blend state.
     */
    private static void drawSceneLayer(SpriteBatch sb, Sts1AtlasMaterializer.RegionTexture resolved) {
        artframework.assets.AtlasRegion region = resolved.region;
        float x;
        float y;
        float scaleX;
        float scaleY;
        if (com.megacrit.cardcrawl.core.Settings.isFourByThree) {
            x = region.offsetX * com.megacrit.cardcrawl.core.Settings.scale;
            y = region.offsetY * com.megacrit.cardcrawl.core.Settings.yScale;
            scaleX = com.megacrit.cardcrawl.core.Settings.scale;
            scaleY = com.megacrit.cardcrawl.core.Settings.yScale;
        } else if (com.megacrit.cardcrawl.core.Settings.isLetterbox) {
            x = region.offsetX * com.megacrit.cardcrawl.core.Settings.xScale;
            y = region.offsetY * com.megacrit.cardcrawl.core.Settings.xScale;
            scaleX = com.megacrit.cardcrawl.core.Settings.xScale;
            scaleY = com.megacrit.cardcrawl.core.Settings.xScale;
        } else {
            x = region.offsetX * com.megacrit.cardcrawl.core.Settings.scale;
            y = region.offsetY * com.megacrit.cardcrawl.core.Settings.scale;
            scaleX = com.megacrit.cardcrawl.core.Settings.scale;
            scaleY = com.megacrit.cardcrawl.core.Settings.scale;
        }
        sb.draw(resolved.texture, x, y, 0f, 0f,
                region.width, region.height, scaleX, scaleY, 0f,
                region.x, region.y, region.width, region.height, false, false);
    }

    /**
     * The rotating native {@code AbstractScene.renderAtlasRegionIf(sb, x, y, angle, region, cond)}
     * submission: the same 16-arg pixel-rect overload but with origin at the packed centre
     * {@code (region.width / 2, region.height / 2)}, {@code angle} rotation, and the plan-local
     * {@code extraX}/{@code extraY} added to the region placement. Unlike {@link #drawSceneLayer},
     * this native overload does not consult the four-by-three / letterbox branches: it places and
     * scales the region with a single unconditional {@code Settings.scale}. Shared by the ACT-3
     * flame layers; the caller is responsible for the tint and blend state.
     */
    private static void drawAnimatedSceneLayer(SpriteBatch sb,
            Sts1AtlasMaterializer.RegionTexture resolved,
            float extraX, float extraY, float angle) {
        artframework.assets.AtlasRegion region = resolved.region;
        float x = region.offsetX * com.megacrit.cardcrawl.core.Settings.scale;
        float y = region.offsetY * com.megacrit.cardcrawl.core.Settings.scale;
        float scaleX = com.megacrit.cardcrawl.core.Settings.scale;
        float scaleY = com.megacrit.cardcrawl.core.Settings.scale;
        sb.draw(resolved.texture, x + extraX, y + extraY,
                region.width / 2f, region.height / 2f,
                region.width, region.height, scaleX, scaleY, angle,
                region.x, region.y, region.width, region.height, false, false);
    }

    private static BottomFlags readBottomFlags(Object sceneInstance) {
        try {
            Class<?> scene = Class.forName("com.megacrit.cardcrawl.scenes.TheBottomScene");
            boolean solidMid = readBooleanField(scene, sceneInstance, "renderSolidMid");
            boolean hollowMid = readBooleanField(scene, sceneInstance, "renderHollowMid");
            boolean leftWall = readBooleanField(scene, sceneInstance, "renderLeftWall");
            boolean[] mods = new boolean[6];
            for (int i = 0; i < 6; i++) {
                mods[i] = readBooleanField(scene, sceneInstance, "renderCeilingMod" + (i + 1));
            }
            return BottomFlags.of(solidMid, hollowMid, leftWall,
                    mods[0], mods[1], mods[2], mods[3], mods[4], mods[5]);
        } catch (Throwable ignored) {
            return BottomFlags.failOpen();
        }
    }

    private static boolean readBooleanField(Class<?> owner, Object target, String name)
            throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    /**
     * Reads the live ACT-2 {@code TheCityScene} dynamic state reflectively. A single unreadable
     * member aborts the whole read and fails open to {@link CityFlags#failOpen()} (the base plan);
     * there is no per-member neutral fallback inside this method.
     */
    private static CityFlags readCityFlags(Object sceneInstance) {
        try {
            Class<?> scene = Class.forName("com.megacrit.cardcrawl.scenes.TheCityScene");
            boolean renderAltBg = readBooleanField(scene, sceneInstance, "renderAltBg");
            boolean renderMg = readBooleanField(scene, sceneInstance, "renderMg");
            boolean renderMgGlow = readBooleanField(scene, sceneInstance, "renderMgGlow");
            boolean renderMgAlt = readBooleanField(scene, sceneInstance, "renderMgAlt");
            boolean renderWall = readBooleanField(scene, sceneInstance, "renderWall");
            boolean renderChains = readBooleanField(scene, sceneInstance, "renderChains");
            boolean renderThrone = readBooleanField(scene, sceneInstance, "renderThrone");
            boolean renderFg2 = readBooleanField(scene, sceneInstance, "renderFg2");
            boolean darkDay = readBooleanField(scene, sceneInstance, "darkDay");
            PillarConfig pillars = readPillarConfig(scene, sceneInstance);
            Color overlay = readColorField(scene, sceneInstance, "overlayColor");
            Color white = readColorField(scene, sceneInstance, "whiteColor");
            Color yellow = readColorField(scene, sceneInstance, "yellowTint");
            return CityFlags.of(renderAltBg, renderMg, renderMgGlow, renderMgAlt, renderWall,
                    renderChains, renderThrone, renderFg2, darkDay, pillars, overlay, white, yellow);
        } catch (Throwable ignored) {
            return CityFlags.failOpen();
        }
    }

    /**
     * Reads the live ACT-3 {@code TheBeyondScene} dynamic state reflectively. Members are read
     * individually: each boolean is read per field (a missing boolean throws and aborts the whole
     * read), a colour read neutralises to {@link Color#WHITE}, and a {@code columnConfig} read
     * neutralises to {@link ColumnConfig#OPEN}. Only such a whole-read failure reaches the catch and
     * fails open to {@link BeyondFlags#failOpen()} (the base plan).
     */
    private static BeyondFlags readBeyondFlags(Object sceneInstance) {
        try {
            Class<?> scene = Class.forName("com.megacrit.cardcrawl.scenes.TheBeyondScene");
            boolean renderAltBg = readBooleanField(scene, sceneInstance, "renderAltBg");
            boolean renderM1 = readBooleanField(scene, sceneInstance, "renderM1");
            boolean renderM2 = readBooleanField(scene, sceneInstance, "renderM2");
            boolean renderM3 = readBooleanField(scene, sceneInstance, "renderM3");
            boolean renderM4 = readBooleanField(scene, sceneInstance, "renderM4");
            boolean renderI1 = readBooleanField(scene, sceneInstance, "renderI1");
            boolean renderI2 = readBooleanField(scene, sceneInstance, "renderI2");
            boolean renderI3 = readBooleanField(scene, sceneInstance, "renderI3");
            boolean renderI4 = readBooleanField(scene, sceneInstance, "renderI4");
            boolean renderI5 = readBooleanField(scene, sceneInstance, "renderI5");
            boolean renderS1 = readBooleanField(scene, sceneInstance, "renderS1");
            boolean renderS2 = readBooleanField(scene, sceneInstance, "renderS2");
            boolean renderS3 = readBooleanField(scene, sceneInstance, "renderS3");
            boolean renderS4 = readBooleanField(scene, sceneInstance, "renderS4");
            boolean renderS5 = readBooleanField(scene, sceneInstance, "renderS5");
            boolean renderF1 = readBooleanField(scene, sceneInstance, "renderF1");
            boolean renderF2 = readBooleanField(scene, sceneInstance, "renderF2");
            boolean renderF3 = readBooleanField(scene, sceneInstance, "renderF3");
            boolean renderF4 = readBooleanField(scene, sceneInstance, "renderF4");
            boolean renderF5 = readBooleanField(scene, sceneInstance, "renderF5");
            ColumnConfig columns = readColumnConfig(scene, sceneInstance);
            Color overlay = readColorField(scene, sceneInstance, "overlayColor");
            Color tmp = readColorField(scene, sceneInstance, "tmpColor");
            return BeyondFlags.of(renderAltBg,
                    renderM1, renderM2, renderM3, renderM4,
                    renderI1, renderI2, renderI3, renderI4, renderI5,
                    renderS1, renderS2, renderS3, renderS4, renderS5,
                    renderF1, renderF2, renderF3, renderF4, renderF5,
                    columns, overlay, tmp);
        } catch (Throwable ignored) {
            return BeyondFlags.failOpen();
        }
    }

    private static ColumnConfig readColumnConfig(Class<?> scene, Object sceneInstance) {
        try {
            Field field = scene.getDeclaredField("columnConfig");
            field.setAccessible(true);
            Object value = field.get(sceneInstance);
            if (value == null) return ColumnConfig.OPEN;
            ColumnConfig parsed = ColumnConfig.parse(((Enum<?>) value).name());
            return parsed != null ? parsed : ColumnConfig.OPEN;
        } catch (Throwable ignored) {
            return ColumnConfig.OPEN;
        }
    }

    private static PillarConfig readPillarConfig(Class<?> scene, Object sceneInstance) {
        try {
            Field field = scene.getDeclaredField("pillarConfig");
            field.setAccessible(true);
            Object value = field.get(sceneInstance);
            if (value == null) return PillarConfig.OPEN;
            PillarConfig parsed = PillarConfig.parse(((Enum<?>) value).name());
            return parsed != null ? parsed : PillarConfig.OPEN;
        } catch (Throwable ignored) {
            return PillarConfig.OPEN;
        }
    }

    private static Color readColorField(Class<?> scene, Object sceneInstance, String name) {
        try {
            Field field = scene.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(sceneInstance);
            return value instanceof Color ? (Color) value : Color.WHITE;
        } catch (Throwable ignored) {
            return Color.WHITE;
        }
    }

    private static TextureAtlas readSceneAtlas(Object sceneInstance) {
        try {
            Class<?> scene = Class.forName("com.megacrit.cardcrawl.scenes.AbstractScene");
            Field field = scene.getDeclaredField("atlas");
            field.setAccessible(true);
            Object value = field.get(sceneInstance);
            return value instanceof TextureAtlas ? (TextureAtlas) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Per-instance region reads from the scene's own atlas; never cached across act swaps. */
    private static final class InstanceSceneRegionSource implements BottomRegionSource {
        private final TextureAtlas atlas;

        InstanceSceneRegionSource(TextureAtlas atlas) {
            this.atlas = atlas;
        }

        public Sts1AtlasMaterializer.RegionTexture region(String regionName) {
            try {
                if (atlas == null || regionName == null) return null;
                TextureAtlas.AtlasRegion gdx = atlas.findRegion(regionName);
                if (gdx == null) return null;
                artframework.assets.AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
                if (neutral == null || !neutral.valid()) return null;
                Texture texture = gdx.getTexture();
                if (texture == null) return null;
                return new Sts1AtlasMaterializer.RegionTexture(texture, neutral);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public static void recordArtDraw(int quadCount) {
        if (quadCount > 0) {
            artDrawCount += quadCount;
        }
    }

    public static long artDrawCount() {
        return artDrawCount;
    }

    public static void recordNativeFallback() {
        nativeFallbackCount++;
    }

    public static long nativeFallbackCount() {
        return nativeFallbackCount;
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("variant", configured.name().toLowerCase());
        m.put("available", Boolean.valueOf(available()));
        m.put("artOwnsBackgroundRequested", Boolean.valueOf(artOwnsBackground()));
        m.put("artDrawCount", Long.valueOf(artDrawCount));
        m.put("nativeFallbackCount", Long.valueOf(nativeFallbackCount));
        m.put("patchedScenes", PATCHED_SCENES);
        return m;
    }

    public static void clearForRecovery() {
        configured = Variant.OFF;
        artDrawCount = 0L;
        nativeFallbackCount = 0L;
        BackgroundOnlyGate.clearForRecovery();
    }

    public static void resetForTests() {
        clearForRecovery();
        BackgroundOnlyGate.resetForTests();
    }
}
