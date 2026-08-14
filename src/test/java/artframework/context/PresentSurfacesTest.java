package artframework.context;

import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.assets.AssetPack;
import artframework.assets.AssetDomain;
import artframework.assets.ResourceIds;
import artframework.core.SignalHandler;
import artframework.core.SignalNames;
import artframework.core.UiComponent;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationRegistry;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PresentSurfacesTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    private void mountCombatWithTwoStrikes(FakeSignalBackend backend) {
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(new CardRef("h1", "Strike_R"))
                                        .slot(0)
                                        .pose(CardPose.at(1, 2))
                                        .art(ResourceIds.cardArt("Strike_R"))
                                        .build(),
                                CardView.builder(new CardRef("h2", "Strike_R"))
                                        .slot(1)
                                        .pose(CardPose.at(3, 4))
                                        .art(ResourceIds.cardArt("Strike_R"))
                                        .build())));
        ArtFramework.publishFrame(backend.currentFrame());
        UiOpResult mount =
                ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
        assertTrue(mount.isOk());
    }

    @Test
    public void handHardSyncAndProbe() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountCombatWithTwoStrikes(backend);
        UiComponent hand = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        assertNotNull(hand);
        assertTrue(hand.isMounted());
        assertEquals(2, ArtFramework.projection().listZone(CardZone.HAND).size());
        assertEquals(1f, ArtFramework.projection().get("h1").pose.x, 0.01f);

        java.util.Map<String, Object> slice = hand.probeSlice();
        // Default policy is OFF — mount alone is not full-present (16.0).
        assertEquals(Boolean.FALSE, slice.get("fullPresent"));
        assertEquals("OFF", slice.get("presentLevel"));
        assertNotNull(slice.get("hand"));
    }

    @Test
    public void fullPresentLevelReflectedInProbe() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountCombatWithTwoStrikes(backend);
        artframework.sts1.FullPresentMode.setCombatHandLevel(artframework.sts1.PresentLevel.FULL);
        java.util.Map<String, Object> slice =
                ArtFramework.component(SurfaceIds.COMBAT_HAND).probeSlice();
        assertEquals("FULL", slice.get("presentLevel"));
        assertEquals(Boolean.TRUE, slice.get("fullPresent"));
        assertEquals(Boolean.FALSE, slice.get("maySuppressNative"));
    }

    @Test
    public void surfaceLifecycleAndPolicyAreMirroredByComponents() {
        UiComponent hand = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        hand.mount();
        hand.probeSlice();

        EntityId entity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);
        assertEquals(SurfaceIds.COMBAT_HAND,
                PresentSurfaces.world().get(entity, SurfaceIdentityComponent.class).id);
        assertTrue(PresentSurfaces.world().get(entity, SurfaceLifecycleComponent.class).mounted);
        assertEquals(artframework.sts1.PresentLevel.OFF,
                PresentSurfaces.world().get(entity, SurfacePolicyComponent.class).level);

        hand.unmount();
        assertFalse(PresentSurfaces.world().contains(entity));
    }

    @Test
    public void mountedFacadeStateIsDerivedFromSurfaceLifecycleComponent() {
        UiComponent hand = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        hand.mount();
        EntityId entity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);

        PresentSurfaces.world().put(entity, SurfaceLifecycleComponent.class,
                new SurfaceLifecycleComponent(false));

        assertFalse(hand.isMounted());
    }

    @Test
    public void facadeRebindsToCurrentEcsContextAfterRegistryRecreation() {
        UiComponent hand = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        hand.mount();
        EntityId oldEntity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);

        PresentationRegistry.close("c2-surfaces");

        UiComponent current = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        current.mount();
        EntityId currentEntity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);
        assertFalse(oldEntity.equals(currentEntity));
        assertTrue(current.isMounted());
        assertTrue(PresentSurfaces.world().contains(currentEntity));
    }

    @Test
    public void surfaceIntentAndResultAreStoredAsEcsData() {
        UiComponent hand = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        hand.mount();
        ArtFramework.publishFrame(ContextFrame.of(1L, "combat", Arrays.asList(
                CardView.builder(new CardRef("card", "Strike_R")).build())));
        ArtFramework.connect(ContextSignals.action(SurfaceIds.COMBAT_HAND, IntentNames.BEGIN_DRAG),
                signal -> artframework.core.SignalDecision.stopRejected("blocked: test"));

        UiOpResult result = hand.action("begin_drag", "card");
        assertEquals(UiOpResult.Status.BLOCKED, result.status);
        EntityId entity = PresentSurfaces.world().query(SurfaceIdentityComponent.class).get(0);
        SurfaceActionComponent action = PresentSurfaces.world().get(entity, SurfaceActionComponent.class);
        SurfaceIntentComponent intent = PresentSurfaces.world().get(entity, SurfaceIntentComponent.class);
        SurfaceResultComponent outcome = PresentSurfaces.world().get(entity, SurfaceResultComponent.class);
        assertEquals(IntentNames.BEGIN_DRAG, action.name);
        assertEquals(IntentNames.BEGIN_DRAG, intent.name);
        assertEquals(SurfaceIds.COMBAT_HAND, intent.surfaceId);
        assertEquals(IntentResult.Status.REJECTED, outcome.status);
    }

    @Test
    public void controlsAndMapStrongViewsInProbe() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ControlsView controls = ControlsView.combat(3, 2, 5, 1, 0, true, true);
        MapView map =
                new MapView(
                        java.util.Collections.singletonList(
                                new MapNodeView(
                                        2,
                                        1,
                                        5f,
                                        6f,
                                        false,
                                        true,
                                        "E",
                                        "elite",
                                        ResourceIds.mapNode("elite"))),
                        1280,
                        720);
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "map",
                        java.util.Collections.<CardView>emptyList(),
                        controls,
                        map,
                        new ViewportView(1280, 720, 1280, 720)));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).mount();
        ArtFramework.component(SurfaceIds.MAP).mount();

        java.util.Map<String, Object> cProbe =
                ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).probeSlice();
        assertEquals(Boolean.TRUE, cProbe.get("endTurnEnabled"));
        assertEquals(Integer.valueOf(3), cProbe.get("energy"));

        java.util.Map<String, Object> mProbe = ArtFramework.component(SurfaceIds.MAP).probeSlice();
        assertEquals(Integer.valueOf(1), mProbe.get("nodeCount"));
    }

    @Test
    public void dragPlayIntentChain() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountCombatWithTwoStrikes(backend);
        final AtomicReference<String> dragSig = new AtomicReference<String>();
        ArtFramework.component(SurfaceIds.COMBAT_HAND)
                .connect(
                        SignalNames.DRAG_STARTED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                dragSig.set(String.valueOf(args[0]));
                            }
                        });

        UiOpResult begin =
                ArtFramework.ops().invoke(SurfaceIds.COMBAT_HAND, "begin_drag", "h1");
        assertTrue(begin.isOk());
        assertEquals("h1", dragSig.get());
        assertEquals("h1", ArtFramework.projection().dragInstanceId());

        UiOpResult play =
                ArtFramework.ops()
                        .playHandCardRef(new CardRef("h2", "Strike_R"), "monster1");
        assertTrue(play.isOk());
        assertTrue(backend.signalLog().size() >= 2);
    }

    @Test
    public void dragBlockedByInterceptor() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountCombatWithTwoStrikes(backend);
        ArtFramework.connect(ContextSignals.action(SurfaceIds.COMBAT_HAND, IntentNames.BEGIN_DRAG),
                new artframework.core.SignalListener() {
                    @Override public artframework.core.SignalDecision onSignal(artframework.core.UiSignal signal) {
                        return artframework.core.SignalDecision.stopRejected("blocked: begin_drag");
                    }
                });
        UiOpResult begin =
                ArtFramework.ops().invoke(SurfaceIds.COMBAT_HAND, "begin_drag", "h1");
        assertEquals(UiOpResult.Status.BLOCKED, begin.status);
        // Backend subscribed during binding, so it observes this signal before a later stopper.
        assertEquals(1, backend.signalLog().size());
    }

    @Test
    public void controlsAndMapSurfaces() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).mount();
        ArtFramework.component(SurfaceIds.MAP).mount();

        assertTrue(ArtFramework.ops().invoke(SurfaceIds.COMBAT_CONTROLS, "press").isOk());
        assertEquals(IntentNames.PRESS_END_TURN, ((UiIntent) backend.signalLog().get(0).payload).name);

        assertTrue(
                ArtFramework.ops()
                        .invoke(SurfaceIds.MAP, "click_node", new artframework.c2.MapNodeRef(1, 2, "M"))
                        .isOk());
        assertEquals(IntentNames.CLICK_MAP_NODE, ((UiIntent) backend.signalLog().get(1).payload).name);
    }

    @Test
    public void skeletonSurfaceActions() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        ArtFramework.component(SurfaceIds.SKELETON).mount();
        assertTrue(ArtFramework.ops().invoke(SurfaceIds.SKELETON, "play", "idle").isOk());
        assertEquals("play", ((UiIntent) backend.signalLog().get(0).payload).name);
    }

    @Test
    public void handArtResolveUsesPack() {
        ArtFramework.assets()
                .registerPack(
                        AssetPack.builder("skin")
                                .priority(5)
                                .domain(AssetDomain.CARD)
                                .entry(ResourceIds.cardArt("Strike_R"), "skin-strike")
                                .build());
        FakeSignalBackend backend = new FakeSignalBackend();
        mountCombatWithTwoStrikes(backend);
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> hand =
                (java.util.List<java.util.Map<String, Object>>)
                        ArtFramework.component(SurfaceIds.COMBAT_HAND).probeSlice().get("hand");
        assertEquals("skin", hand.get(0).get("artPack"));
        assertEquals(Boolean.TRUE, hand.get(0).get("artFound"));
    }

    @Test
    public void playHandCardSugarUsesInstance() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountCombatWithTwoStrikes(backend);
        UiOpResult r = ArtFramework.ops().playHandCard("Strike_R", "self");
        assertTrue(r.isOk());
        UiIntent intent = (UiIntent) backend.signalLog().get(backend.signalLog().size() - 1).payload;
        assertEquals(IntentNames.PLAY_CARD, intent.name);
        assertTrue(intent.args[0] instanceof CardRef);
        assertEquals("h1", ((CardRef) intent.args[0]).instanceId);
    }

    @Test
    public void probeIncludesPresentAndAssets() {
        FakeSignalBackend backend = new FakeSignalBackend();
        mountCombatWithTwoStrikes(backend);
        java.util.Map<String, Object> map = ArtFramework.probe().asMap();
        assertTrue(map.containsKey("present"));
        assertTrue(map.containsKey("projection"));
        assertTrue(map.containsKey("backend"));
        assertTrue(map.containsKey("assets"));
        assertEquals("signals", ((java.util.Map<?, ?>) map.get("backend")).get("id"));
    }

    @Test
    public void endTurnLegacyStillNativeComponent() {
        // full-present controls must not steal sts1.endturn native id
        assertFalse(SurfaceIds.COMBAT_CONTROLS.equals(SurfaceIds.canonicalize("sts1.endturn")));
        UiComponent nativeEnd = artframework.c2.NativeComponents.get("sts1.endturn");
        assertNotNull(nativeEnd);
        assertEquals("sts1.endturn", nativeEnd.id());
    }
}
