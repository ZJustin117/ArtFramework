package artframework.sts1.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure pre-native background ownership decision plus variant draws for the
 * {@code art verify mode background} surface. The gate concerns only the single background family
 * and fails open on panic; it never suppresses any other native pixels.
 */
public final class BackgroundRenderGate {

    public static final String BACKGROUND_FAMILY = "sts1.room.background";

    public enum Variant {
        OFF,
        SOLID,
        CHECKER,
        GRID;

        public static Variant parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase();
            if ("off".equals(normalized)) return OFF;
            if ("solid".equals(normalized)) return SOLID;
            if ("checker".equals(normalized)) return CHECKER;
            if ("grid".equals(normalized)) return GRID;
            return null;
        }
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
