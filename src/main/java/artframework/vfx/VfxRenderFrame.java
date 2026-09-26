package artframework.vfx;

import artframework.component.Rect;
import artframework.render.RenderPixelPayload;
import artframework.render.RenderPhase;
import artframework.render.RenderPlan;
import artframework.render.RenderTargetKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure payload mapping plus the legacy immutable container type for ART VFX draws.
 *
 * <p><b>Not the pixel authority.</b> The backend pixel authority is the shared
 * {@code ArtRenderFrame}: {@code ParticleRenderProjectionSystem} publishes one contribution per VFX
 * root, and the ART VFX backend consumes that shared frame split by producer. This type is no
 * longer consumed by the backend; it is retained for its pure static
 * {@link #payloadEntry(VfxParticleDraw)} mapping (still used when publishing contributions) and for
 * the immutable {@link #draws}/{@link #rootEnds}/{@link #planEntries} value semantics exercised by
 * tests. {@link #empty()} and the constructors likewise remain for that legacy value contract.</p>
 *
 * <p>Alongside the ordered draw list, the frame exposes the same content as
 * {@link RenderPlan.Entry} values carrying a {@link RenderPixelPayload}. These entries mix, in a
 * single {@code (phase, z, stableKey)} ordering, with identity/geometry-only entries (for example
 * native-retained entries that carry no payload). The payload is never used for ordering or
 * identity, so adding it does not change draw order or pixels.</p>
 */
public final class VfxRenderFrame {
    public final List<VfxParticleDraw> draws;
    public final List<Integer> rootEnds;
    /** Payload-bearing frame entries for the unified frame; deterministically ordered. */
    public final List<RenderPlan.Entry> planEntries;

    public VfxRenderFrame(List<VfxParticleDraw> draws) {
        this(draws, Collections.singletonList(draws.size()));
    }

    public VfxRenderFrame(List<VfxParticleDraw> draws, List<Integer> rootEnds) {
        this.draws = Collections.unmodifiableList(new ArrayList<VfxParticleDraw>(draws));
        this.rootEnds = Collections.unmodifiableList(new ArrayList<Integer>(rootEnds));
        int previous = 0;
        for (Integer end : this.rootEnds) {
            if (end == null || end < previous || end > this.draws.size()) {
                throw new IllegalArgumentException("invalid root draw boundary");
            }
            previous = end;
        }
        if (previous != this.draws.size()) throw new IllegalArgumentException("unassigned draws");
        List<RenderPlan.Entry> entries = new ArrayList<RenderPlan.Entry>(this.draws.size());
        for (VfxParticleDraw draw : this.draws) entries.add(payloadEntry(draw));
        Collections.sort(entries, new java.util.Comparator<RenderPlan.Entry>() {
            @Override public int compare(RenderPlan.Entry a, RenderPlan.Entry b) {
                return artframework.render.RenderOrder.COMPARATOR.compare(
                        new artframework.render.RenderOrder(a.phase, a.z, a.stableKey),
                        new artframework.render.RenderOrder(b.phase, b.z, b.stableKey));
            }
        });
        this.planEntries = Collections.unmodifiableList(entries);
    }

    public static VfxRenderFrame empty() {
        return new VfxRenderFrame(Collections.<VfxParticleDraw>emptyList());
    }

    /**
     * Maps one immutable VFX draw to a host-neutral pixel payload entry. The mapping is pure and
     * deterministic: it reads only the draw record and produces no host object.
     *
     * <p>{@code width}/{@code height} are {@code 0} ("natural texture size"): the projection has no
     * host texture to measure, so only {@code scaleX}/{@code scaleY} transform the natural size. The
     * source rectangle is normalized UV over the flipbook sheet. Resource and label normalization
     * and every finite/non-negative check belong to {@link RenderPixelPayload}.</p>
     */
    public static RenderPlan.Entry payloadEntry(VfxParticleDraw draw) {
        if (draw == null) throw new IllegalArgumentException("draw required");
        int columns = Math.max(1, draw.flipbookColumns);
        int rows = Math.max(1, draw.flipbookRows);
        int frame = Math.max(0, draw.flipbookFrame);
        int column = frame % columns;
        int row = frame / columns;
        RenderPixelPayload payload = new RenderPixelPayload(
                draw.textureReference, draw.nodeId,
                draw.x, draw.y, 0f, 0f,
                (float) column / columns, (float) row / rows,
                1f / columns, 1f / rows,
                draw.flipX, draw.flipY, draw.rotationDegrees, draw.scaleX, draw.scaleY,
                draw.r, draw.g, draw.b, draw.alpha, draw.blendMode,
                frame, columns, rows);
        return RenderPlan.Entry.payloadEntry("vfx:" + draw.stableKey,
                RenderTargetKind.OVERLAY, new Rect(draw.x, draw.y, 0f, 0f),
                RenderPhase.ART_EFFECTS, draw.zIndex, draw.stableKey, true, payload);
    }
}
