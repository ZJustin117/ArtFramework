package artframework.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.presentation.EffectAttachment;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertEquals(null,
                RenderHosts.get().getTarget(RenderHost.c2SurfaceTargetId("sts1.hidden")));
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

    @Test public void hostRecreationReleasesCacheButRetainsEcsAuthority() {
        RenderStateEcs.surface("sts1.host-recreate", 2f, 3f, 20f, 30f, true);
        RenderProjectionQueue.projectNow();
        assertNotNull(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.host-recreate")));

        RenderHosts.get().recreateHostCache();

        assertEquals(1, RenderStateEcs.context().entities().size());
        assertEquals(0, RenderHosts.get().targetCount());
        RenderProjectionQueue.projectNow();
        assertNotNull(RenderHosts.get().getTarget(
                RenderHost.c2SurfaceTargetId("sts1.host-recreate")));
    }

    @Test public void fullFrameEnabledQueryReadsEcsWithoutHostMirror() {
        RenderHost host = new RenderHost();
        assertFalse(host.isFullFrameEnabled());

        RenderStateEcs.fullFrame(320f, 180f, true,
                Collections.<EffectAttachment>emptyList());

        assertTrue(host.isFullFrameEnabled());
        host.recreateFromEcs();
        assertNotNull(host.fullFrameTarget());

        RenderStateEcs.fullFrame(320f, 180f, false,
                Collections.<EffectAttachment>emptyList());
        assertFalse(host.isFullFrameEnabled());
    }

    @Test public void entityPresentTargetRebuildsFromRetainedSlotComponents() {
        ArtFramework.entities().attach("player", "player", "ironclad");
        ArtFramework.entities().layout("player", 100f, 200f, 0.5f);
        RenderHost host = new RenderHost();
        String targetId = "c2:entity:player";
        host.recreateFromEcs();
        Rect before = host.getTarget(targetId).bounds();

        host.clearHostCacheForRecreation();

        assertEquals(1, ArtFramework.entities().size());
        host.recreateFromEcs();
        assertNotNull(host.getTarget(targetId));
        assertEquals(before, host.getTarget(targetId).bounds());
    }

    @Test public void entityPresentDetachImmediatelyRemovesProjectedTarget() {
        ArtFramework.entities().present("player", "player", "ironclad", null,
                100f, 200f, 0.5f);
        assertNotNull(RenderHosts.get().getTarget("c2:entity:player"));

        ArtFramework.entities().detach("player");

        assertEquals(null, RenderHosts.get().getTarget("c2:entity:player"));
    }

    @Test public void entityPresentClearRemovesAllProjectedTargetsInOneBatch() {
        ArtFramework.entities().present("player", "player", "ironclad", null,
                100f, 200f, 0.5f);
        ArtFramework.entities().present("monster", "monster", "cultist", null,
                300f, 200f, 0.5f);
        int before = RenderProjectionQueue.rebuildCountForTests();

        ArtFramework.entities().clear();

        assertEquals(null, RenderHosts.get().getTarget("c2:entity:player"));
        assertEquals(null, RenderHosts.get().getTarget("c2:entity:monster"));
        assertEquals(before + 1, RenderProjectionQueue.rebuildCountForTests());
    }

    @Test public void entityPresentSlotsAreEffectFreeAndCannotGainHostOnlyEffects() {
        ArtFramework.entities().present("player", "player", "ironclad", null,
                100f, 200f, 0.5f);
        RenderHost host = RenderHosts.get();
        assertTrue(host.effectsOf("c2:entity:player").isEmpty());

        try {
            host.bindEffect("c2:entity:player", TintEffect.ID,
                    Collections.<String, Object>emptyMap());
            throw new AssertionError("EntityPresent slots must reject host-only effects");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ECS-owned"));
        }

        host.clearHostCacheForRecreation();
        host.recreateFromEcs();
        assertTrue(host.effectsOf("c2:entity:player").isEmpty());
    }
}
