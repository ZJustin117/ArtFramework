package artframework.c1.host;

import artframework.component.Rect;
import artframework.console.ProbePublisher;
import artframework.c2.EntitySnapshot;
import artframework.render.NativeRenderOwnership;
import artframework.render.RenderHosts;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.render.BackgroundOnlyGate;
import artframework.sts1.render.NativeRenderInvocation;
import artframework.sts1.render.Sts1NativePresentationAdapter;
import artframework.sts1.render.Sts1RenderPipeline;
import artframework.api.ArtFramework;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StageHostProbeCadenceTest {

    @After
    public void tearDown() {
        BackgroundOnlyGate.resetForTests();
        FullPresentMode.resetForTests();
        ArtFramework.resetForTests();
    }

    @Test public void postRenderDoesNotAdvanceProbePublisher() {
        final int[] heartbeat = {0};
        ProbePublisher publisher = new ProbePublisher("test", new ProbePublisher.Clock() {
            private long now;
            @Override public long nanoTime() { now += ProbePublisher.HEARTBEAT_INTERVAL_NANOS; return now; }
        }, new ProbePublisher.Sink() {
            @Override public boolean writeFull(String line) { return true; }
            @Override public boolean writeHeartbeat(String line) { heartbeat[0]++; return true; }
        }, new ProbePublisher.FullSnapshot() {
            @Override public String line() { return "ART_PROBE {}"; }
        });
        StageHost host = new StageHost(publisher);

        host.receivePostUpdate();
        host.receivePostRender(null);
        host.receivePostRender(null);
        assertEquals(0, heartbeat[0]);

        host.receivePostUpdate();
        assertEquals(1, heartbeat[0]);
    }

    @Test public void backgroundOnlyRecordsUncoveredOnlyWhenArtOutputPending() throws Exception {
        RenderHosts.resetForTests();
        Sts1RenderPipeline.resetForTests();
        StageHost host = new StageHost(null);
        forceReady(host);
        BackgroundOnlyGate.setActive(true);

        // Clean frame: no stage, no FX bindings/targets, no present draw plan, no live VFX draws.
        // The post-render pass is a true no-op and must NOT record uncovered.
        host.receivePostRender(null);
        assertEquals(Long.valueOf(0L), coverage().get("uncovered"));

        // Genuine ART output pending (full-frame overlay) must be recorded as uncovered.
        RenderHosts.get().setFullFrameEnabled(true);
        host.receivePostRender(null);
        assertEquals(Long.valueOf(1L), coverage().get("uncovered"));
        assertEquals(Long.valueOf(1L), coverage().get("uncoveredDistinct"));
    }

    @Test public void retainedNativeTargetsDoNotCountAsArtOutput() throws Exception {
        RenderHosts.resetForTests();
        Sts1RenderPipeline.resetForTests();
        StageHost host = new StageHost(null);
        forceReady(host);
        BackgroundOnlyGate.setActive(true);

        // Project a retained NATIVE input: the plan carries a NATIVE_RETAINED target
        // ("native:<key>") with no ART effect bindings. This is native pixel ownership ART does
        // not draw, so it must NOT count as pending ART output and must NOT record uncovered.
        Sts1NativePresentationAdapter.present(new NativeRenderInvocation(1L, 1L, "combat",
                "sts1.native.torch", "Native", "render", "family", "src",
                new Rect(1f, 2f, 3f, 4f)), NativeRenderOwnership.OBSERVED);
        RenderHosts.get().recreateFromEcs();

        assertTrue("retained native target present", RenderHosts.get().targetCount() > 0);
        host.receivePostRender(null);
        assertEquals(Long.valueOf(0L), coverage().get("uncovered"));
    }

    @Test public void emptyEntityPresentSlotDoesNotCountAsArtOutput() throws Exception {
        RenderHosts.resetForTests();
        Sts1RenderPipeline.resetForTests();
        StageHost host = new StageHost(null);
        forceReady(host);
        BackgroundOnlyGate.setActive(true);
        // Entity/skeleton surface inactive: the native-only anchor carries no ART resource.
        FullPresentMode.setSkeletonLevel(PresentLevel.OFF);

        ArtFramework.entities().present("sts1.native.skeleton/test",
                "monster", "test", EntitySnapshot.playerChrome("Native", 0, 0, 0),
                100f, 100f, 1f);
        RenderHosts.get().recreateFromEcs();

        assertTrue("entity-present target present",
                RenderHosts.get().getTarget("c2:entity:sts1.native.skeleton/test") != null);
        host.receivePostRender(null);
        assertEquals(Long.valueOf(0L), coverage().get("uncovered"));
    }

    @Test public void entityPresentSlotCountsWhenEntitySurfaceActive() throws Exception {
        RenderHosts.resetForTests();
        Sts1RenderPipeline.resetForTests();
        StageHost host = new StageHost(null);
        forceReady(host);
        BackgroundOnlyGate.setActive(true);
        // Skeleton/entity surface at FULL owns those pixels, so the slot counts as ART output.
        FullPresentMode.setSkeletonLevel(PresentLevel.FULL);

        ArtFramework.entities().present("sts1.native.skeleton/test",
                "monster", "test", EntitySnapshot.playerChrome("Native", 0, 0, 0),
                100f, 100f, 1f);
        RenderHosts.get().recreateFromEcs();

        host.receivePostRender(null);
        assertEquals(Long.valueOf(1L), coverage().get("uncovered"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> coverage() {
        return BackgroundOnlyGate.probeSlice();
    }

    private static void forceReady(StageHost host) throws Exception {
        Field ready = StageHost.class.getDeclaredField("ready");
        ready.setAccessible(true);
        ready.setBoolean(host, true);
    }
}
