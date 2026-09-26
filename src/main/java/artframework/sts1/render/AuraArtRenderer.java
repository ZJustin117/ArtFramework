package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Injected draw callback seam for a claimed {@code vfx-stance-aura} instance.
 *
 * <p>The default is intentionally inert: {@link #isReady} returns {@code false} and {@link #render}
 * returns {@code false}, so F1 never claims (and never changes) a pixel. F2 installs the real ART
 * atlas draw through the test seam. A false result always means "no pixels produced" so the caller
 * fails open to the native effect.
 */
public final class AuraArtRenderer {

    /**
     * Host draw callback for one claimed aura instance. Returns true only on a real ART draw.
     *
     * <p>Retained as the public F2 draw seam: F2's real atlas renderer implements this single-purpose
     * interface (the mutable readiness predicate lives separately on {@link Adapter}) so a host or
     * test can supply draw-only fakes without a readiness probe.
     */
    public interface EffectDraw {
        boolean render(SpriteBatch sb, AbstractGameEffect effect);
    }

    /** Ready predicate plus draw callback installed by F2's real renderer (test seam). */
    public interface Adapter {
        boolean isReady(String nativeClassName);

        boolean render(SpriteBatch sb, AbstractGameEffect effect);
    }

    private static volatile Adapter adapter;

    /** Successful claimed-draw counter; independent of the adapter test seam. */
    private static final AtomicInteger DRAW_COUNT = new AtomicInteger();

    private AuraArtRenderer() {}

    /** Increments the ART aura draw counter (called by the claim patch on a successful draw). */
    public static void recordDraw() {
        DRAW_COUNT.incrementAndGet();
    }

    /** Number of successful ART aura draws recorded since the last reset. */
    public static int drawCount() {
        return DRAW_COUNT.get();
    }

    /** Test seam: zero the draw counter so a case can assert its own increment. */
    public static void resetDrawCountForTests() {
        DRAW_COUNT.set(0);
    }

    /** True only when ART can draw this effect's pixels. Default: never (F2 supplies the real one). */
    public static boolean isReady(String nativeClassName) {
        Adapter current = adapter;
        if (current == null) return false;
        try {
            return current.isReady(nativeClassName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Draws the aura; false means "no pixels produced" (caller fails open to native). */
    public static boolean render(SpriteBatch sb, AbstractGameEffect effect) {
        Adapter current = adapter;
        if (current == null) return false;
        try {
            return current.render(sb, effect);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Test seam: install a ready predicate and draw callback; null restores the inert default. */
    static void setForTests(Adapter next) {
        adapter = next;
    }

    static void resetForTests() {
        adapter = null;
    }

    /** Installs the real aura renderer. Null restores the inert default. */
    public static void install(Adapter next) {
        try {
            setForTests(next);
        } catch (Throwable ignored) {
        }
    }

    /** Restores the inert default renderer. */
    public static void uninstall() {
        try {
            setForTests(null);
        } catch (Throwable ignored) {
        }
    }
}
