package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.c2.EntityAnchorView;
import artframework.c2.EntityKind;
import artframework.c2.EntitySlot;
import artframework.context.SurfaceIds;
import artframework.skeleton.FakeSkeletonProvider;
import artframework.skeleton.SkeletonHandle;
import artframework.skeleton.SkeletonProvider;
import artframework.skeleton.SkeletonPresentationFrames;
import artframework.skeleton.SkeletonPresentationView;
import artframework.skeleton.SkeletonSource;
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
    public void forwardsDevelopmentTrackControlsAndRecordsCommands() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        Sts1SkeletonBridge.setProviderId(FakeSkeletonProvider.ID);
        Sts1SkeletonBridge.play("hero", "", "");

        assertTrue(Sts1SkeletonBridge.setTrackTime("hero", 0f));
        assertTrue(Sts1SkeletonBridge.setTimeScale("hero", 0f));
        assertEquals(0f, fake.trackTime("hero"), 0.001f);
        assertEquals(0f, fake.timeScale("hero"), 0.001f);
        assertEquals("timeScale:hero:0.0", Sts1SkeletonBridge.probeSlice().get("lastDevCommand"));
        assertTrue(((java.util.List<?>) Sts1SkeletonBridge.probeSlice().get("events"))
                .contains("setTrackTime:hero:0.0"));
        assertTrue(((java.util.List<?>) Sts1SkeletonBridge.probeSlice().get("events"))
                .contains("setTimeScale:hero:0.0"));
    }

    @Test
    public void rejectsNonFiniteDevelopmentTrackControls() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        Sts1SkeletonBridge.setProviderId(FakeSkeletonProvider.ID);
        Sts1SkeletonBridge.play("hero", "", "");

        assertFalse(Sts1SkeletonBridge.setTrackTime("hero", Float.NaN));
        assertFalse(Sts1SkeletonBridge.setTimeScale("hero", Float.POSITIVE_INFINITY));
        assertEquals("timeScale:hero:invalid", Sts1SkeletonBridge.probeSlice().get("lastDevCommand"));
        assertEquals("timeScale must be finite", Sts1SkeletonBridge.probeSlice().get("lastError"));
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
    public void observedNativeSkeletonExposesDependencyNeutralAnchorAndEntityProjection() {
        String entityKey = "monster-cultist-1";
        FakeSkeletonProvider fake = registerAndMountSkeletonProvider();
        Skeleton skeleton = new Skeleton(new SkeletonData());
        Sts1SkeletonBridge.observeNativeSkeletonForTests(skeleton, entityKey, EntityKind.MONSTER,
                "Cultist", 320f, 180f, 90f, 140f, true,
                "images/monsters/theBottom/cultist/skeleton.atlas",
                "images/monsters/theBottom/cultist/skeleton.json");

        Sts1SkeletonBridge.syncPresentation(1L,
                java.util.Arrays.asList(viewFor(entityKey, fake.id())));

        EntityAnchorView anchor = Sts1SkeletonBridge.nativeAnchorView(skeleton);
        assertNotNull(anchor);
        assertEquals(entityKey, anchor.entityId);
        assertEquals(EntityKind.MONSTER, anchor.kind);
        assertEquals("Cultist", anchor.name);
        assertEquals(320f, anchor.x, 0.001f);
        assertEquals(180f, anchor.y, 0.001f);
        assertEquals(275f, anchor.bounds.x, 0.001f);
        assertEquals(90f, anchor.bounds.width, 0.001f);
        assertTrue(anchor.visible);
        assertTrue(anchor.claimed);
        assertEquals("char.skeleton.skeleton", anchor.assetId);

        EntitySlot slot = ArtFramework.entities().get("sts1.native.skeleton/" + entityKey);
        assertNotNull(slot);
        assertEquals(EntityKind.MONSTER, slot.kind);
        assertEquals("Cultist", slot.snapshot().label);
        assertEquals("", slot.snapshot().artResourceId);
        assertEquals(Boolean.FALSE, slot.snapshot().extras.get("nativePixelsAuthoritative"));
        assertEquals(anchor.assetId, slot.snapshot().extras.get("anchorAssetId"));
    }

    @Test
    public void duplicateNativeSkeletonClaimKeepsOnlyOneInstanceSuppressed() {
        String entityKey = "duplicate-creature";
        FakeSkeletonProvider fake = registerAndMountSkeletonProvider();
        Skeleton first = new Skeleton(new SkeletonData());
        Skeleton second = new Skeleton(new SkeletonData());
        Sts1SkeletonBridge.observeNativeSkeletonForTests(first, entityKey, EntityKind.PLAYER,
                "Ironclad", 100f, 200f, 120f, 160f, true, "char.ironclad", "ironclad.json");
        Sts1SkeletonBridge.observeNativeSkeletonForTests(second, entityKey, EntityKind.PLAYER,
                "Ironclad", 100f, 200f, 120f, 160f, true, "char.ironclad", "ironclad.json");

        Sts1SkeletonBridge.syncPresentation(1L,
                java.util.Arrays.asList(viewFor(entityKey, fake.id())));

        int claimed = (Sts1SkeletonBridge.canRenderClaimedNative(first) ? 1 : 0)
                + (Sts1SkeletonBridge.canRenderClaimedNative(second) ? 1 : 0);
        assertEquals(1, claimed);
        assertEquals(Integer.valueOf(1), Sts1SkeletonBridge.probeSlice().get("nativeClaimedCount"));
        assertEquals(Integer.valueOf(1), Sts1SkeletonBridge.probeSlice().get("duplicateNativeClaims"));
    }

    @Test
    public void claimReleaseAndUnmountFailOpenCloseLedger() {
        String entityKey = "claimed-release";
        FakeSkeletonProvider fake = registerAndMountSkeletonProvider();
        Skeleton skeleton = new Skeleton(new SkeletonData());
        Sts1SkeletonBridge.observeNativeSkeletonForTests(skeleton, entityKey,
                "atlas", "skeleton");
        Sts1SkeletonBridge.syncPresentation(1L,
                java.util.Arrays.asList(viewFor(entityKey, fake.id())));
        assertTrue(Sts1SkeletonBridge.canRenderClaimedNative(skeleton));

        FullPresentMode.setSkeletonLevel(PresentLevel.OFF);
        RenderDisposition failOpen = NativeRenderBridge.beginSkeletonRender(skeleton);
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, failOpen.mode);
        assertTrue(failOpen.nativeContinuation);

        FullPresentMode.setSkeletonLevel(PresentLevel.FULL);
        Sts1SkeletonBridge.syncPresentation(2L, java.util.Collections.<SkeletonPresentationView>emptyList());
        assertFalse(Sts1SkeletonBridge.canRenderClaimedNative(skeleton));
        assertEquals(Integer.valueOf(0), Sts1SkeletonBridge.probeSlice().get("nativeClaimedCount"));
        assertTrue(((Number) Sts1SkeletonBridge.probeSlice().get("nativeClaimReleases")).intValue() >= 1);
        assertEquals(RenderDisposition.Mode.PASS_THROUGH,
                NativeRenderBridge.beginSkeletonRender(skeleton).mode);
        assertFalse(ArtFramework.entities().isAttached("sts1.native.skeleton/" + entityKey));
    }

    @Test
    public void delegatedSkeletonRenderFailureFallsBackToNativeContinuation() {
        String entityKey = "missing-renderer";
        registerAndMountSkeletonProvider();
        ArtFramework.skeletons().register(new PlainSkeletonProvider());
        Skeleton skeleton = new Skeleton(new SkeletonData());
        Sts1SkeletonBridge.observeNativeSkeletonForTests(skeleton, entityKey,
                "atlas", "skeleton");
        Sts1SkeletonBridge.syncPresentation(1L, java.util.Arrays.asList(
                viewFor(entityKey, PlainSkeletonProvider.ID)));

        RenderDisposition disposition = NativeRenderBridge.beginSkeletonRender(skeleton);
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, disposition.mode);
        assertTrue(disposition.nativeContinuation);
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

    private static final class PlainSkeletonProvider implements SkeletonProvider {
        private static final String ID = "plain";

        @Override public String id() { return ID; }

        @Override public SkeletonHandle load(SkeletonSource source) {
            return new SkeletonHandle(ID, source.skeletonId, source);
        }

        @Override public void unload(SkeletonHandle handle) {
            if (handle != null) handle.markDisposed();
        }
    }
}
