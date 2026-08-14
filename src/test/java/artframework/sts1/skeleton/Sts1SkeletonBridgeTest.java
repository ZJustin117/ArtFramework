package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.skeleton.FakeSkeletonProvider;
import artframework.skeleton.SkeletonHandle;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Sts1SkeletonBridgeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void playStopLifecycle() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        Sts1SkeletonBridge.setProviderId(FakeSkeletonProvider.ID);
        SkeletonHandle h = Sts1SkeletonBridge.play("ironclad", "a.png", "s.json");
        assertNotNull(h);
        assertEquals(1, Sts1SkeletonBridge.liveCount());
        assertEquals(1, fake.liveCount());
        Sts1SkeletonBridge.stop("ironclad");
        assertEquals(0, Sts1SkeletonBridge.liveCount());
        assertEquals(0, fake.liveCount());
    }

    @Test
    public void shouldDrawRequiresFullMounted() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        assertFalse(Sts1SkeletonBridge.shouldDraw());
        FullPresentMode.setSkeletonLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.SKELETON).mount();
        assertTrue(Sts1SkeletonBridge.shouldDraw());
        PresentSafety.panic("sk");
        assertFalse(Sts1SkeletonBridge.shouldDraw());
    }

    @Test
    public void stopAllOnReset() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        Sts1SkeletonBridge.setProviderId(FakeSkeletonProvider.ID);
        Sts1SkeletonBridge.play("a", "", "");
        Sts1SkeletonBridge.play("b", "", "");
        Sts1SkeletonBridge.stopAll();
        assertEquals(0, Sts1SkeletonBridge.liveCount());
    }

    @Test
    public void forwardsCommandProviderActions() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        Sts1SkeletonBridge.setProviderId(FakeSkeletonProvider.ID);
        Sts1SkeletonBridge.play("hero", "", "");

        assertTrue(Sts1SkeletonBridge.setAnimation("hero", "idle_loop", true));
        assertTrue(Sts1SkeletonBridge.addAnimation("hero", "attack", false, 0.2f));
        assertTrue(Sts1SkeletonBridge.setMix("hero", "idle_loop", "attack", 0.1f));

        assertEquals("idle_loop", Sts1SkeletonBridge.currentAnimation("hero"));
        assertEquals(Float.valueOf(0.1f), fake.mix("hero", "idle_loop", "attack"));
        assertTrue(fake.events("hero").contains("add:attack:false:0.2"));
    }

    @Test
    public void hostRecreationReleasesPresentationBindingsAndNativeClaims() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        Sts1SkeletonBridge.setProviderId(FakeSkeletonProvider.ID);
        Sts1SkeletonBridge.play("hero", "", "");
        Sts1SkeletonBridge.syncPresentation(1L, java.util.Arrays.asList(
                new artframework.skeleton.SkeletonPresentationView(
                        "hero",
                        new artframework.skeleton.SkeletonAssetComponent(FakeSkeletonProvider.ID, "a", "s", "", 1f),
                        new artframework.skeleton.SkeletonPoseComponent(1f, 2f, 0f, 1f, 1f, false, false, 0),
                        new artframework.skeleton.SkeletonAnimationComponent(0, "idle", true),
                        new artframework.skeleton.SkeletonVisualComponent(true))));
        SkeletonHandle before = Sts1SkeletonBridge.presentationSystem().binding("hero").handle;

        PresentSafety.onHostRecreated();

        assertEquals(0, Sts1SkeletonBridge.liveCount());
        assertEquals(1, Sts1SkeletonBridge.presentationSystem().size());
        assertFalse(before.isAlive());
        assertTrue(Sts1SkeletonBridge.presentationSystem().binding("hero").handle.isAlive());
        assertEquals(1, fake.liveCount());
        assertEquals(1, PresentSafety.recreationCount());
    }
}
