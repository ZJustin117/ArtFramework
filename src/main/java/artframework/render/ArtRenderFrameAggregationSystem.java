package artframework.render;

import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless aggregation pass that merges every render family's payload contribution into the single
 * immutable {@link ArtRenderFrame}.
 *
 * <p>It reads the {@link ArtRenderContributionComponent} value(s) published by producers, merges
 * their payload entries into one ordered frame via {@link ArtRenderFrame#of}, and publishes that
 * frame onto the shared {@link ArtRenderFrameComponent}. Each entry keeps its structured producer
 * owner, so consumers select their own family by owner id instead of a {@code stableKey} prefix.
 * There is no second mutable draw state: the aggregate is rebuilt from contributions each run and is
 * immutable once published. When no producer has an entry the aggregate component is removed so a
 * cleared/recovered scene cannot render stale chrome. It is deterministic and safe on empty
 * input.</p>
 *
 * <p>Aggregation failures (for example a duplicate stable key across producers) are <em>contained
 * here</em>: {@link ArtRenderFrame#of} keeps rejecting duplicates, but {@link #run} catches every
 * failure at its boundary, publishes the safe empty frame (so the previous aggregate is never
 * mistaken for this frame's content) and records a queryable rejection count/reason on the durable
 * {@link ArtRenderFrameDiagnosticsComponent}. The failure therefore never escapes into the schedule
 * and cannot skip later {@code RENDER_CLOCK} / {@code HOST_BACKEND} phases.</p>
 *
 * <p>The diagnostics live on a frame-independent entity so the cumulative rejection count survives
 * empty ticks, during which the frame component itself is removed. Only the last reason is cleared
 * by a later successful aggregation; the count keeps accumulating.</p>
 *
 * <p>It registers into the existing {@code RENDER_PROJECTION} schedule phase only; no new schedule
 * or phase is introduced.</p>
 */
public final class ArtRenderFrameAggregationSystem implements EcsSystem {
    public static final String SYSTEM_ID = "art.render-frame-aggregation";

    /**
     * Test seam: when {@code true}, {@link #clearRejectionReasonBestEffort} throws so a focused test
     * can prove that a diagnostics failure after a successful publish leaves the published frame
     * intact and the rejection count unchanged. Production leaves this {@code false}.
     */
    private static volatile boolean failDiagnosticsClearForTests;

    static void setFailDiagnosticsClearForTests(boolean fail) {
        failDiagnosticsClearForTests = fail;
    }

    static void resetForTests() {
        failDiagnosticsClearForTests = false;
    }

    /**
     * Installs this system into the existing schedule-owned render projection phase. Idempotent
     * (checked against the live registry each call) so a pack reset cannot leave a stale
     * "installed" flag that silently skips re-registration.
     */
    public static synchronized void install() {
        for (EcsSystem system : PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION)) {
            if (system instanceof ArtRenderFrameAggregationSystem) return;
        }
        PackSystems.enable(PackSystemPhase.RENDER_PROJECTION, SYSTEM_ID,
                new ArtRenderFrameAggregationSystem());
    }

    public static synchronized void uninstall() {
        PackSystems.disable(PackSystemPhase.RENDER_PROJECTION, SYSTEM_ID);
    }

    @Override public void run(PresentationWorld world, EcsTick tick) {
        if (world == null || tick == null) throw new IllegalArgumentException("world and tick required");
        ArtRenderFrame frame;
        try {
            frame = aggregate(world);
        } catch (RuntimeException rejected) {
            // Contain the failure at the aggregation boundary instead of letting it abort the
            // schedule phase. Publish the safe empty frame so a prior aggregate is never mistaken
            // for this frame's content, and record the rejection durably for probes.
            failSafely(world, "aggregate", rejected);
            return;
        }
        if (frame.isEmpty()) {
            try {
                // Empty tick: remove the frame component but leave the durable diagnostics alone so
                // the rejection count/reason survive until a real frame is published again.
                removeFrame(world);
            } catch (RuntimeException failed) {
                // A failure while removing (for example a closed or foreign world) must not escape:
                // fall back to the safe empty frame and record the diagnostic so later schedule
                // phases (RENDER_CLOCK / HOST_BACKEND) still run.
                failSafely(world, "publish", failed);
            }
            return;
        }
        try {
            publish(world, frame);
        } catch (RuntimeException failed) {
            // The frame was never committed: contain the failure at the boundary so later schedule
            // phases still run.
            failSafely(world, "publish", failed);
            return;
        }
        // The frame is now authoritative and committed. Diagnostics are a best-effort side
        // channel: clearing the last reason must never be able to discard the published frame or
        // advance the rejection count, so it is isolated from the publish boundary.
        clearRejectionReasonBestEffort(world);
    }

    /**
     * Best-effort diagnostics clear for a successfully published non-empty frame: clears the last
     * reason while keeping the cumulative count. Skip the write when there is nothing to update so a
     * never-rejected world gains no extra entity. Any read/write failure is swallowed because the
     * frame is already committed; this must never publish an empty frame or increment the count.
     */
    private static void clearRejectionReasonBestEffort(PresentationWorld world) {
        try {
            if (failDiagnosticsClearForTests) {
                throw new IllegalStateException("diagnostics read/write failed (test injection)");
            }
            long rejected = ArtRenderFrameDiagnosticsComponent.rejectedCount(world);
            if (rejected != 0L
                    || !ArtRenderFrameDiagnosticsComponent.rejectionReason(world).isEmpty()) {
                ArtRenderFrameDiagnosticsComponent.record(world, rejected, "");
            }
        } catch (RuntimeException ignored) {
            // Nothing further can be done: the committed frame and the cumulative count stand.
        }
    }

    /**
     * Best-effort recovery for any aggregation failure: try to publish the safe empty frame (so a
     * stale aggregate is never mistaken for this frame's content), advance the durable rejection
     * counter + reason, and never let a secondary failure escape the system boundary.
     */
    private static void failSafely(PresentationWorld world, String stage, RuntimeException failure) {
        long rejected = 1L;
        try {
            rejected = ArtRenderFrameDiagnosticsComponent.rejectedCount(world) + 1L;
        } catch (RuntimeException ignored) {
            // The world is unusable; the diagnostic write below is the last best effort.
        }
        try {
            publish(world, ArtRenderFrame.empty());
        } catch (RuntimeException ignored) {
            // The world itself is unusable; the diagnostic below is the last best effort.
        }
        try {
            ArtRenderFrameDiagnosticsComponent.record(world, rejected, reasonFor(stage, failure));
        } catch (RuntimeException ignored) {
            // Nothing further can be recorded; the schedule phase must still return normally.
        }
    }

    /**
     * Purely merges the current producer contributions into one immutable frame, tagging each entry
     * with its producer owner. Empty input (no producer, or producers with no entries) yields
     * {@link ArtRenderFrame#empty()}. A duplicate stable key within or across producers is rejected
     * by {@link ArtRenderFrame#of}.
     */
    static ArtRenderFrame aggregate(PresentationWorld world) {
        if (world == null) return ArtRenderFrame.empty();
        List<RenderPlan.Entry> merged = new ArrayList<RenderPlan.Entry>();
        Map<String, String> owners = new HashMap<String, String>();
        for (EntityId entity : world.query(ArtRenderContributionComponent.class)) {
            ArtRenderContributionComponent contribution =
                    world.get(entity, ArtRenderContributionComponent.class);
            if (contribution == null) continue;
            for (RenderPlan.Entry entry : contribution.entries) {
                if (entry != null && entry.stableKey != null) {
                    owners.put(entry.stableKey, contribution.producerId);
                }
            }
            merged.addAll(contribution.entries);
        }
        return ArtRenderFrame.of(merged, owners);
    }

    /** Publishes the aggregate onto a single shared frame entity, reusing any existing one. */
    static EntityId publish(PresentationWorld world, ArtRenderFrame frame) {
        if (world == null) throw new IllegalArgumentException("world required");
        EntityId target = frameEntity(world);
        if (target == null) target = world.createEntity();
        world.put(target, ArtRenderFrameComponent.class, new ArtRenderFrameComponent(frame));
        return target;
    }

    /** Removes the published aggregate so a cleared/recovered scene cannot render stale entries. */
    static void removeFrame(PresentationWorld world) {
        if (world == null) return;
        while (true) {
            EntityId target = frameEntity(world);
            if (target == null) return;
            world.destroyEntity(target);
        }
    }

    static ArtRenderFrame frameFor(PresentationWorld world) {
        if (world == null) return null;
        EntityId target = frameEntity(world);
        if (target == null) return null;
        ArtRenderFrameComponent component = world.get(target, ArtRenderFrameComponent.class);
        return component == null ? null : component.value;
    }

    private static String reasonFor(String stage, RuntimeException rejected) {
        String message = rejected.getMessage();
        String detail = message == null || message.isEmpty()
                ? rejected.getClass().getSimpleName() : message;
        return stage + ": " + detail;
    }

    private static EntityId frameEntity(PresentationWorld world) {
        List<EntityId> entities = world.query(ArtRenderFrameComponent.class);
        return entities.isEmpty() ? null : entities.get(0);
    }
}
