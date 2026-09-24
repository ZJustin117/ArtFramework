package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.context.RoomShellView;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.render.ArtRenderFrame;
import artframework.render.ArtRenderFrameComponent;
import artframework.render.RenderPixelPayload;
import artframework.render.RenderPlan;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.backend.Sts1RoomShellProjection;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.helpers.FontHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resource-backed chrome/label overlay for native room-shell pixels.
 *
 * <p>Room-shell pixel entries are read from the shared {@link ArtRenderFrame} aggregate: the
 * stateless {@link RoomShellRenderProjectionSystem} publishes this family's payload contribution and
 * the {@link artframework.render.ArtRenderFrameAggregationSystem} merges it into one ordered frame.
 * The host-side {@code title} label and anchor are read from the minimal
 * {@link RoomShellRenderFrame} metadata component because {@code RenderPixelPayload} cannot express
 * {@code FontHelper.renderFontCentered} without changing pixels.</p>
 *
 * <p>This class is the host boundary that turns the aggregate payload entries into
 * {@link SpriteBatch} submissions. It never re-reads {@link Sts1RoomShellProjection} while
 * drawing. Native room pixels continue ({@code nativeContinuation}, {@code overlay-only}); ART owns
 * no native pixels here.</p>
 */
public final class Sts1RoomShellDrawPath {
    private Sts1RoomShellDrawPath() {}

    public static RoomShellView current() { return Sts1RoomShellProjection.current(); }

    public static String resourceFor(RoomShellView view) {
        if (view == null) return ResourceIds.UI_ROOM_SHELL_UNKNOWN;
        if (Sts1VanillaCatalog.isKnown(view.resourceId)) return view.resourceId;
        String candidate = ResourceIds.roomShell(view.kind);
        return Sts1VanillaCatalog.isKnown(candidate) ? candidate : ResourceIds.UI_ROOM_SHELL_UNKNOWN;
    }

    public static Map<String, Object> probeSlice() {
        RoomShellView view = current();
        Map<String, Object> out = new LinkedHashMap<String, Object>(view.toMap());
        out.put("resourceId", resourceFor(view));
        AssetResolveResult resolved = null;
        try { resolved = ArtFramework.assets().resolve(resourceFor(view)); } catch (Throwable ignored) {}
        out.put("resourceFound", Boolean.valueOf(resolved != null && resolved.found));
        out.put("nativeContinuation", Boolean.TRUE);
        out.put("nativePixelsSuppressed", Boolean.FALSE);
        out.put("surface", "overlay-only");
        // Evidence for the aggregate-frame-backed pixel path: the consumer draws the room-shell
        // payload entries from the shared aggregate frame plus the host-side title metadata, never
        // the live observation.
        List<RenderPlan.Entry> entries = aggregatedEntries();
        RoomShellRenderFrame metadata = publishedMetadata();
        out.put("frameEntries", Integer.valueOf(entries.size()));
        out.put("frameHasTitle", Boolean.valueOf(metadata != null && !metadata.title().isEmpty()));
        out.put("frameNativePixelsSuppressed", Boolean.FALSE);
        return out;
    }

    /**
     * Draws the room-shell entries from the published aggregate frame plus the host-side title. The
     * observation filter (unavailable/invisible/event owned by the delegated event surface) was
     * already applied by the projection system, so an empty aggregate contributes nothing. Failure
     * is fail-open because native room rendering already continued.
     */
    public static void render(SpriteBatch sb) {
        if (sb == null) return;
        RoomShellRenderFrame metadata = publishedMetadata();
        try {
            List<RenderPlan.Entry> entries = aggregatedEntries();
            for (int index = 0; index < entries.size(); index++) {
                RenderPlan.Entry entry = entries.get(index);
                if (entry == null || entry.payload == null) continue;
                drawPayload(sb, entry.payload);
            }
            if (metadata != null && !metadata.title().isEmpty()) {
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, metadata.title(),
                        metadata.titleCenterX(), metadata.titleCenterY(),
                        com.badlogic.gdx.graphics.Color.WHITE);
            }
        } catch (Throwable ignored) {
            // Native room rendering has already continued; overlay failure is fail-open.
        }
    }

    /** Host boundary for one payload entry: resolve the resource and submit the draw. */
    private static void drawPayload(SpriteBatch sb, RenderPixelPayload payload) {
        try {
            String resourceId = payload.resourceId();
            AssetResolveResult result = ArtFramework.assets().resolve(resourceId);
            com.badlogic.gdx.graphics.Texture texture =
                    artframework.sts1.assets.Sts1AssetMaterializer.resolveTexture(result);
            if (texture != null && payload.width() > 0f && payload.height() > 0f) {
                sb.draw(texture, payload.x(), payload.y(), payload.width(), payload.height());
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * The room-shell entries in the current published aggregate frame, or an empty list when no
     * aggregate is published (for example before the first schedule tick or after a clear). A
     * missing frame is never replaced with a fresh observation read: the draw path stays frame-only.
     */
    static List<RenderPlan.Entry> aggregatedEntries() {
        ArtRenderFrame frame = ArtRenderFrameComponent.read(ArtEcs.world());
        return roomShellEntries(frame);
    }

    /**
     * Filters the shared aggregate down to this family's entries by structured producer owner. A
     * foreign producer that happens to use a {@code room-shell:} stable-key prefix is still excluded
     * because ownership is the producer id recorded at aggregation, never a key prefix.
     */
    static List<RenderPlan.Entry> roomShellEntries(ArtRenderFrame frame) {
        if (frame == null) return new ArrayList<RenderPlan.Entry>();
        return new ArrayList<RenderPlan.Entry>(
                frame.entriesFor(RoomShellRenderProjectionSystem.PRODUCER_ID));
    }

    /**
     * The current published room-shell metadata frame, or {@code null} when the projection system
     * has not published one (for example before the first schedule tick or after a clear).
     */
    static RoomShellRenderFrame publishedMetadata() {
        List<EntityId> entities = ArtEcs.world().query(RoomShellRenderFrameComponent.class);
        if (entities.isEmpty()) return null;
        RoomShellRenderFrameComponent component =
                ArtEcs.world().get(entities.get(0), RoomShellRenderFrameComponent.class);
        return component == null ? null : component.value;
    }
}
