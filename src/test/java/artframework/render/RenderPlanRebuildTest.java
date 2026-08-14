package artframework.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.presentation.EffectAttachment;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RenderPlanRebuildTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }

    @Test public void ecsPlanRebuildRestoresSurfaceFullFrameAndItemCache() {
        RenderStateEcs.surface("sts1.test", 1f, 2f, 30f, 40f, true);
        RenderStateEcs.surfaceEffects("sts1.test", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        RenderStateEcs.fullFrame(1920f, 1080f, true, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient", null)));
        artframework.presentation.PresentationVisuals.syncC2Item(
                "sts1.test", "item", new Rect(5f, 6f, 7f, 8f), 2f,
                "card", "", "", true);

        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan(Collections.singleton("sts1.test"));
        host.clearTargets();
        host.rebuildFromEcsPlan(Collections.singleton("sts1.test"));

        assertNotNull(host.getTarget(RenderHost.FULL_FRAME_ID));
        assertNotNull(host.getTarget(RenderHost.c2SurfaceTargetId("sts1.test")));
        assertNotNull(host.getTarget(RenderHost.c2ItemTargetId("sts1.test", "item")));
        assertEquals(3, host.targetCount());
        assertEquals(3, host.bindingCount());
    }

    @Test public void queueProjectsRequestedActiveSurface() {
        RenderStateEcs.surface("sts1.visible", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surface("sts1.hidden", 0f, 0f, 10f, 10f, true);

        RenderProjectionQueue.projectActiveSurfaces(Collections.singleton("sts1.visible"));

        assertNotNull(RenderHosts.get().getTarget(RenderHost.c2SurfaceTargetId("sts1.visible")));
    }

    @Test public void publicRecreationApiRestoresOnlyFromEcsState() {
        RenderStateEcs.surface("sts1.recreate", 2f, 3f, 20f, 30f, true);
        RenderHost host = new RenderHost();
        host.recreateFromEcs();
        assertNotNull(host.getTarget(RenderHost.c2SurfaceTargetId("sts1.recreate")));

        host.clearHostCacheForRecreation();
        assertEquals(0, host.targetCount());
        host.recreateFromEcs();
        assertEquals(1, host.targetCount());
        assertEquals(new Rect(2f, 3f, 20f, 30f),
                host.getTarget(RenderHost.c2SurfaceTargetId("sts1.recreate")).bounds());
    }
}
