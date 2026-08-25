package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.skeleton.FakeSkeletonProvider;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.SkeletonPresentationFrames;
import artframework.skeleton.SkeletonPresentationView;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Sts1SkeletonBridgeTest {

    @After
    public void tearDown() {
        Sts1SkeletonBridge.stopAll();
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

    @Test
    public void publishedSkeletonFrameReachesBridgePresentationSystem() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        Sts1SkeletonBridge.installPresentationSignals();
        SkeletonPresentationFrames.publish(4L, java.util.Arrays.asList(
                new artframework.skeleton.SkeletonPresentationView(
                        "hero",
                        new artframework.skeleton.SkeletonAssetComponent(FakeSkeletonProvider.ID,
                                "a", "s", "", 1f),
                        new artframework.skeleton.SkeletonPoseComponent(1f, 2f, 0f, 1f, 1f,
                                false, false, 0),
                        new artframework.skeleton.SkeletonAnimationComponent(0, "idle", true),
                        new artframework.skeleton.SkeletonVisualComponent(true))));

        assertEquals(1, Sts1SkeletonBridge.presentationSystem().size());
        assertNotNull(Sts1SkeletonBridge.presentationSystem().binding("hero"));
        assertEquals(4L, Sts1SkeletonBridge.presentationSystem().lastFrameId());
    }

    @Test
    public void unclaimedNativeSkeletonPassesThroughWithNativeContinuation() {
        Skeleton skeleton = new Skeleton(new SkeletonData());
        RenderDisposition disposition = NativeRenderBridge.beginSkeletonRender(skeleton);
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
        assertTrue("unclaimed skeleton must continue to native renderer",
                disposition.nativeContinuation);
    }

    @Test
    public void claimedSkeletonDelegatesWithoutNativeContinuation() {
        String entityKey = "claimed-creature-1";
        FakeSkeletonProvider fake = registerAndMountSkeletonProvider();
        Skeleton skeleton = new Skeleton(new SkeletonData());
        Sts1SkeletonBridge.observeNativeSkeletonForTests(skeleton, entityKey, "atlas", "skeleton");
        SkeletonPresentationView view = viewFor(entityKey, fake.id());
        Sts1SkeletonBridge.syncPresentation(1L, java.util.Arrays.asList(view));

        assertTrue("skeleton must be claimed", Sts1SkeletonBridge.canRenderClaimedNative(skeleton));
        assertEquals(entityKey, Sts1SkeletonBridge.nativeEntityKey(skeleton));

        RenderDisposition disposition = NativeRenderBridge.beginSkeletonRender(skeleton);
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        assertFalse("claimed skeleton must not continue to native renderer",
                disposition.nativeContinuation);
        assertEquals("skeleton:" + entityKey, disposition.presentationEntityId);

        boolean rendered = Sts1SkeletonBridge.renderClaimedNative(skeleton, new Object());
        assertTrue(rendered);
        NativeRenderBridge.recordSkeletonDraw(skeleton, 1);
        assertEquals(1, NativeRenderBridge.ledger().evidenceCount());
        assertTrue(fake.renderedAtNativeSlot(entityKey));
    }

    @Test
    public void hostRecreationRestoresSkeletonClaimWithoutDoubleDraw() {
        String entityKey = "claimed-creature-2";
        FakeSkeletonProvider fake = registerAndMountSkeletonProvider();
        Skeleton skeleton = new Skeleton(new SkeletonData());
        Sts1SkeletonBridge.observeNativeSkeletonForTests(skeleton, entityKey, "atlas", "skeleton");
        SkeletonPresentationView view = viewFor(entityKey, fake.id());
        Sts1SkeletonBridge.syncPresentation(1L, java.util.Arrays.asList(view));
        assertTrue(Sts1SkeletonBridge.canRenderClaimedNative(skeleton));

        Sts1SkeletonBridge.onHostRecreated();

        assertTrue("claim must survive host recreation",
                Sts1SkeletonBridge.canRenderClaimedNative(skeleton));
        assertEquals(entityKey, Sts1SkeletonBridge.nativeEntityKey(skeleton));
        assertEquals(1, Sts1SkeletonBridge.presentationSystem().size());
        assertEquals(1, fake.liveCount());
    }

    private FakeSkeletonProvider registerAndMountSkeletonProvider() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        FullPresentMode.setSkeletonLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.SKELETON).mount();
        return fake;
    }

    private SkeletonPresentationView viewFor(String entityKey, String providerId) {
        return new SkeletonPresentationView(
                entityKey,
                new artframework.skeleton.SkeletonAssetComponent(
                        providerId, "atlas", "skeleton", "", 1f),
                new artframework.skeleton.SkeletonPoseComponent(
                        100f, 200f, 0f, 1f, 1f, false, false, 0),
                new artframework.skeleton.SkeletonAnimationComponent(0, "idle", true),
                new artframework.skeleton.SkeletonVisualComponent(true));
    }
}
