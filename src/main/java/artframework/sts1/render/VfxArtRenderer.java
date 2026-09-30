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

        /**
         * True when {@link #render} could actually produce a draw for this exact instance, i.e. the
         * same field/resource checks {@code render} performs up to the point of drawing all pass.
         * Used to distinguish a benign no-pixel decline (the native effect would draw nothing
         * either) from a genuine renderer failure. A real adapter's {@code canDraw} also reflects
         * the per-kind native draw guard: a guard-blocked instance (the native render's wait phase
         * would draw nothing) reports {@code false}, so its decline is classified as benign.
         */
        boolean canDraw(Object effect);

        /**
         * True only when the adapter can determine that THIS exact instance would natively draw
         * NOTHING, i.e. it is a benign no-pixel decline rather than a genuine renderer failure: (a)
         * a kind the native render guards on a present image
         * ({@link VfxDrawGeometry#nativeSkipsDrawWithoutImage}) whose instance has no drawable
         * image, or (b) a kind whose native draw is blocked by the wait-phase guard
         * ({@link VfxDrawGeometry#nativeSkipsDrawByGuard}) and whose guard field is present and
         * {@code >= 0f}. Every other instance — including a guard-SATISFIED instance whose image
         * snapshot fails — is {@code false}, so a genuine failure is never masked as benign.
         * Unlike {@link #canDraw} this is deliberately narrow: it does NOT report {@code true}
         * merely because the instance is undrawable.
         *
         * <p>Defaults to {@code false} so a draw-only fake adapter (which never declines) need not
         * implement it.
         */
        default boolean declinedWithoutPixels(Object effect) {
            return false;
        }
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

    /**
     * True only when the installed adapter could actually draw this exact instance (the same
     * field/resource checks {@link #render} performs before drawing). Returns {@code false} when no
     * adapter is installed or the probe throws, mirroring the {@link #render} delegator style.
     */
    public static boolean canDraw(Object effect) {
        Adapter current = adapter;
        if (current == null) return false;
        try {
            return current.canDraw(effect);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True only when the installed adapter can determine that THIS exact instance would natively
     * draw NOTHING (a benign no-pixel decline): a null-image kind whose native draw is guarded on a
     * present image, or a wait-phase-guarded kind whose guard field is present and {@code >= 0f}.
     * Returns {@code false} when no adapter is installed, the adapter does not implement the probe,
     * the probe throws, or the instance is not one of those two cases — so a genuine renderer
     * failure is never masked as benign. Mirrors the {@link #canDraw} delegator style.
     */
    public static boolean declinedWithoutPixels(Object effect) {
        Adapter current = adapter;
        if (current == null) return false;
        try {
            return current.declinedWithoutPixels(effect);
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
