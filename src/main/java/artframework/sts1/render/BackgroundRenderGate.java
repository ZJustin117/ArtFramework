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
     * attaches Prefixes to. It is deliberately NOT a claim of device verification: only the act-1
     * Bottom scene is exercised by {@code d1_verify_background_only.yaml} today; City/Beyond/Ending
     * are patched but have no deterministic lab act-jump and are NOT device-verified.
     */
    public static final List<String> PATCHED_SCENES = Collections.unmodifiableList(
            Arrays.asList("TheBottomScene", "TheCityScene", "TheBeyondScene", "TheEndingScene"));

    public enum Variant {
        OFF,
        SOLID,
        CHECKER,
        GRID,
        /** Real ACT-1 {@code TheBottomScene} combat background layers (native atlas regions). */
        BOTTOM;

        public static Variant parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase();
            if ("off".equals(normalized)) return OFF;
            if ("solid".equals(normalized)) return SOLID;
            if ("checker".equals(normalized)) return CHECKER;
            if ("grid".equals(normalized)) return GRID;
            if ("bottom".equals(normalized)) return BOTTOM;
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
            BottomRegionSource source = new InstanceBottomRegionSource(readSceneAtlas(sceneInstance));
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
        try {
            Sts1AtlasMaterializer.RegionTexture resolved = source.region(layer.regionName());
            return resolved != null && resolved.region != null && resolved.texture != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Draws the planned native ACT-1 layers through the region source with the exact native
     * pixel-rect submission (see {@link #drawBottomLayer}). Tint mirrors native: WHITE before
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
                    if (resolved == null || resolved.region == null || resolved.texture == null) {
                        continue;
                    }
                    applyBottomTint(sb, layer, f);
                    drawBottomLayer(sb, resolved);
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
     * NOT a UV blit.
     */
    private static void drawBottomLayer(SpriteBatch sb, Sts1AtlasMaterializer.RegionTexture resolved) {
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
    private static final class InstanceBottomRegionSource implements BottomRegionSource {
        private final TextureAtlas atlas;

        InstanceBottomRegionSource(TextureAtlas atlas) {
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
