package artframework.sts1.render;

import artframework.component.Rect;
import artframework.context.RoomShellView;
import artframework.render.NativeRenderOwnership;
import artframework.render.RenderPhase;
import artframework.render.RenderPixelPayload;
import artframework.render.RenderPlan;
import artframework.render.RenderTargetKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, host-neutral <em>metadata</em> frame for the ART room-shell overlay chrome.
 *
 * <p>Since the shared {@link artframework.render.ArtRenderFrame} aggregation was introduced, the
 * room-shell pixel entries live in the aggregate frame (published by
 * {@link RoomShellRenderProjectionSystem} as the {@code room-shell} producer contribution). This
 * value now carries only the non-payload metadata that {@code RenderPixelPayload} cannot express
 * without changing pixels: the host-side label {@code title} and its centered anchor, plus the
 * non-delegated overlay {@link NativeRenderOwnership}. The consumer reads pixel entries from the
 * aggregate frame and this metadata from here, so the label pixels stay byte-identical while the
 * entry ordering/identity lives in the single aggregate contract.</p>
 *
 * <p>Ownership is recorded as {@link NativeRenderOwnership#NATIVE_WITH_ART_OVERLAY}: native room
 * pixels continue and ART only overdraws chrome. It is never {@code DELEGATED_TO_ART}.</p>
 */
public final class RoomShellRenderFrame {
    private final String title;
    private final float titleCenterX;
    private final float titleCenterY;
    private final NativeRenderOwnership ownership;

    private RoomShellRenderFrame(String title, float titleCenterX, float titleCenterY,
            NativeRenderOwnership ownership) {
        this.title = title == null ? "" : title;
        this.titleCenterX = titleCenterX;
        this.titleCenterY = titleCenterY;
        this.ownership = ownership != null ? ownership : NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY;
    }

    /** Safe empty frame: no label; the consumer draws no metadata. */
    public static RoomShellRenderFrame empty() {
        return new RoomShellRenderFrame("", 0f, 0f, NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY);
    }

    /**
     * Pure, deterministic mapping from the current room-shell observation to this metadata frame.
     *
     * <p>The filter is exactly the legacy draw-time filter, evaluated at projection time instead:
     * a {@code null}, unavailable, or invisible view yields an empty frame, and an {@code event}
     * shell is suppressed while the delegated event surface is available. The title label and its
     * centered anchor are copied so the consumer never re-reads the projection.</p>
     */
    public static RoomShellRenderFrame of(RoomShellView view, boolean eventSurfaceAvailable) {
        if (view == null || !view.available || !view.visible) return empty();
        if ("event".equals(view.kind) && eventSurfaceAvailable) return empty();

        Rect bounds = view.bounds;
        return new RoomShellRenderFrame(view.title,
                bounds.x + bounds.width * 0.5f, bounds.y + bounds.height * 0.54f,
                NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY);
    }

    /**
     * Pure mapping from the current room-shell observation to the payload-bearing frame entries for
     * the shared aggregate. The mapping is host-neutral and references no host object; source
     * resolution and submission stay in the consumer. {@code width}/{@code height} equal the
     * projected bounds so the consumer reproduces the legacy stretched
     * {@code SpriteBatch.draw(texture, x, y, w, h)} exactly.
     *
     * <p>An empty list is returned when the observation is filtered out or has non-positive bounds.
     * The filter mirrors {@link #of}: the metadata frame and these entries are always derived from
     * the same observation.</p>
     */
    public static List<RenderPlan.Entry> payloadEntries(RoomShellView view,
            boolean eventSurfaceAvailable) {
        if (view == null || !view.available || !view.visible) {
            return Collections.emptyList();
        }
        if ("event".equals(view.kind) && eventSurfaceAvailable) {
            return Collections.emptyList();
        }
        Rect bounds = view.bounds;
        if (!(bounds.width > 0f) || !(bounds.height > 0f)
                || !finite(bounds.x) || !finite(bounds.y)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(
                payloadEntry(view, Sts1RoomShellDrawPath.resourceFor(view)));
    }

    /**
     * Maps one room-shell chrome rectangle to a host-neutral pixel payload entry. The mapping is
     * pure and references no host object; source resolution and submission stay in the consumer.
     */
    public static RenderPlan.Entry payloadEntry(RoomShellView view, String resourceId) {
        if (view == null) throw new IllegalArgumentException("view required");
        Rect bounds = view.bounds;
        String stableKey = "room-shell:" + view.kind + ":" + view.id;
        RenderPixelPayload payload = new RenderPixelPayload(
                resourceId, view.kind,
                bounds.x, bounds.y, bounds.width, bounds.height,
                0f, 0f, 1f, 1f,
                false, false, 0f, 1f, 1f,
                1f, 1f, 1f, 1f, "MIX",
                0, 1, 1);
        return RenderPlan.Entry.payloadEntry(stableKey, RenderTargetKind.OVERLAY,
                new Rect(bounds.x, bounds.y, bounds.width, bounds.height),
                RenderPhase.ART_EFFECTS, bounds.y, stableKey, true, payload);
    }

    /** Host-side label text; empty when the projection had no title. Never a pixel payload. */
    public String title() {
        return title;
    }

    public float titleCenterX() {
        return titleCenterX;
    }

    public float titleCenterY() {
        return titleCenterY;
    }

    /** Non-delegated overlay disposition; never {@code DELEGATED_TO_ART}. */
    public NativeRenderOwnership ownership() {
        return ownership;
    }

    /** Metadata frame is empty when there is no label to draw. */
    public boolean isEmpty() {
        return title.isEmpty();
    }

    private static boolean finite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
