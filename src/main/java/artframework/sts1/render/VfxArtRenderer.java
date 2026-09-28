package artframework.sts1.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Injected draw callback seam for one claimed per-instance transient effect (family-neutral; current
 * members are the {@code vfx-stance-aura} FQNs).
 *
 * <p>The default is intentionally inert: {@link #isReady} returns {@code false} and {@link #render}
 * returns {@code false}, so F1 never claims (and never changes) a pixel. F2 installs the real ART
 * atlas draw through the test seam. A false result always means "no pixels produced" so the caller
 * fails open to the native effect.
 */
public final class VfxArtRenderer {

    /**
     * Host draw callback for one claimed transient-effect instance. Returns true only on a real ART draw.
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

    private VfxArtRenderer() {}

    /** Increments the ART claim draw counter (called by the claim patch on a successful draw). */
    public static void recordDraw() {
        DRAW_COUNT.incrementAndGet();
    }

    /** Number of successful ART claim draws recorded since the last reset. */
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

    /** Draws the claimed effect; false means "no pixels produced" (caller fails open to native). */
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

    /** Installs the real claim renderer. Null restores the inert default. */
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

    /**
     * Read-only probe: current gate state, how many supported claim-seam FQNs the renderer is ready
     * for, and the ART draw count. Never throws; a failure reports gate=false/ready=0/draws=0.
     */
    public static java.util.Map<String, Object> probeSlice() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        try {
            boolean gate = VfxDelegationGate.isActive();
            int ready = 0;
            for (String nativeClass : VfxClaimPolicy.supportedClasses()) {
                if (isReady(nativeClass)) ready++;
            }
            m.put("gate", Boolean.valueOf(gate));
            m.put("ready", Integer.valueOf(ready));
            m.put("draws", Integer.valueOf(drawCount()));
        } catch (Throwable ignored) {
            m.put("gate", Boolean.FALSE);
            m.put("ready", Integer.valueOf(0));
            m.put("draws", Integer.valueOf(0));
        }
        return m;
    }
}
