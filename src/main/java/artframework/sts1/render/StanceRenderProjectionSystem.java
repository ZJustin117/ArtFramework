package artframework.sts1.render;

import artframework.assets.ResourceIds;
import artframework.component.Rect;
import artframework.context.OrbStanceView;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.render.ArtRenderContributionComponent;
import artframework.render.RenderPixelPayload;
import artframework.render.RenderPhase;
import artframework.render.RenderPlan;
import artframework.render.RenderTargetKind;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.backend.Sts1OrbStanceProjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stateless ECS render-projection that publishes the current native stance observation as one
 * host-neutral payload entry into the shared render contribution registry.
 *
 * <p>This slice is <b>publish-only</b>: it never suppresses native drawing, never resolves a
 * texture, and never adds a {@code batch.draw}/GL/Texture call. It only re-expresses the already
 * host-neutral {@link OrbStanceView.Entry} fields (geometry, UV extent, tint, rotation, scale) as a
 * {@link RenderPixelPayload} so later slices can consume the shared
 * {@link artframework.render.ArtRenderFrame} without re-reading the projection at draw time.</p>
 *
 * <p>One producer id per stance ({@code "stance:" + id}); the id is normalized so the
 * {@code "stance:"} prefix is always present even when {@link OrbStanceView.Entry#id} already
 * carries it. Stale cleanup clears any {@code stance:}-owned contribution that is no longer a
 * visible, imaged stance this frame, so a cleared projection leaves no stale pixels on the next
 * tick.</p>
 */
public final class StanceRenderProjectionSystem implements EcsSystem {
    public static final String SYSTEM_ID = "art.stance.render-projection";
    /** Prefix of every stance producer id published into the shared render contribution registry. */
    public static final String PRODUCER_PREFIX = "stance:";
    /** Shared ART_EFFECTS band z for the stance overlay; kept clear of VFX/room-shell keys. */
    public static final float STANCE_Z = 0f;

    @Override public void run(PresentationWorld world, EcsTick tick) {
        if (world == null || tick == null) throw new IllegalArgumentException("world and tick required");

        List<OrbStanceView.Entry> stances = new ArrayList<OrbStanceView.Entry>();
        for (OrbStanceView.Entry entry : Sts1OrbStanceProjection.current().entries) {
            if (entry == null) continue;
            if (!"stance".equals(entry.kind)) continue;
            if (!entry.visible || !entry.hasImage) continue;
            stances.add(entry);
        }

        Set<String> liveProducerIds = new HashSet<String>();
        for (OrbStanceView.Entry entry : stances) liveProducerIds.add(producerId(entry.id));

        // Stale cleanup: only this family's producers are touched. A stance that stopped drawing
        // (cleared/recovered projection) must not keep contributing entries into the shared frame.
        for (EntityId entity : world.query(ArtRenderContributionComponent.class)) {
            ArtRenderContributionComponent contribution =
                    world.get(entity, ArtRenderContributionComponent.class);
            if (contribution == null || contribution.producerId == null) continue;
            if (contribution.producerId.startsWith(PRODUCER_PREFIX)
                    && !liveProducerIds.contains(contribution.producerId)) {
                ArtRenderContributionComponent.clear(world, contribution.producerId);
            }
        }

        for (OrbStanceView.Entry entry : stances) {
            ArtRenderContributionComponent.publish(world, producerId(entry.id),
                    Collections.singletonList(payloadEntry(entry)));
        }
    }

    /** Stable producer id for one stance: {@code "stance:" + normalized id}. */
    public static String producerId(String id) {
        return PRODUCER_PREFIX + stanceId(id);
    }

    /** Normalizes an entry id by stripping a redundant {@code "stance:"} prefix when present. */
    static String stanceId(String id) {
        String value = id != null ? id : "";
        return value.startsWith(PRODUCER_PREFIX) ? value.substring(PRODUCER_PREFIX.length()) : value;
    }

    /**
     * Purely maps one observed stance entry to a host-neutral payload entry. The resource falls back
     * to {@link ResourceIds#STANCE_UNKNOWN} when the catalog does not know the stance resource. All
     * numeric fields are coerced finite so an unsafe observation can never abort the schedule.
     */
    static RenderPlan.Entry payloadEntry(OrbStanceView.Entry entry) {
        String stanceId = stanceId(entry.id);
        String candidate = ResourceIds.stance(stanceId);
        String resourceId = Sts1VanillaCatalog.isKnown(candidate)
                ? candidate : ResourceIds.STANCE_UNKNOWN;
        String stableKey = PRODUCER_PREFIX + stanceId;
        float x = finiteOr(entry.centerX, 0f);
        float y = finiteOr(entry.centerY, 0f);
        float width = dimension(entry.width);
        float height = dimension(entry.height);
        float scale = finiteOr(entry.scale, 1f);
        RenderPixelPayload payload = new RenderPixelPayload(
                resourceId, entry.label,
                x, y, width, height,
                0f, 0f, 1f, 1f,
                false, false, finiteOr(entry.angle, 0f), scale, scale,
                finiteOr(entry.colorR, 1f), finiteOr(entry.colorG, 1f),
                finiteOr(entry.colorB, 1f), finiteOr(entry.colorA, 1f), "MIX",
                0, 1, 1);
        Rect bounds = new Rect(x, y, width, height);
        return RenderPlan.Entry.payloadEntry(stableKey, RenderTargetKind.OVERLAY, bounds,
                RenderPhase.ART_EFFECTS, STANCE_Z, stableKey, true, payload);
    }

    /**
     * Installs this producer into the existing {@code RENDER_PROJECTION} phase and then re-asserts
     * the shared aggregator at the end of that phase so the aggregator always runs after this
     * producer. Idempotent against the live registry each call; the aggregator reinstall is safe
     * and also re-orders it after any producer registered later.
     */
    public static synchronized void install() {
        boolean present = false;
        for (EcsSystem system : PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION)) {
            if (system instanceof StanceRenderProjectionSystem) {
                present = true;
                break;
            }
        }
        if (!present) {
            PackSystems.enable(PackSystemPhase.RENDER_PROJECTION, SYSTEM_ID,
                    new StanceRenderProjectionSystem());
        }
        artframework.render.ArtRenderFrameAggregationSystem.install();
    }

    public static synchronized void uninstall() {
        PackSystems.disable(PackSystemPhase.RENDER_PROJECTION, SYSTEM_ID);
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isNaN(value) || Float.isInfinite(value) ? fallback : value;
    }

    private static float dimension(float value) {
        return value > 0f && !Float.isInfinite(value) ? value : 0f;
    }
}
