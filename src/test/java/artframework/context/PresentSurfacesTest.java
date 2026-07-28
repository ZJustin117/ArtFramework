package artframework.context;

import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.assets.AssetPack;
import artframework.assets.AssetDomain;
import artframework.assets.ResourceIds;
import artframework.core.SignalHandler;
import artframework.core.SignalNames;
import artframework.core.UiComponent;
import artframework.core.UiSignalInterceptor;
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

    private void mountCombatWithTwoStrikes(FakeBackend backend) {
        ArtFramework.bindPresentationBackend(backend);
        backend.pushFrame(
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
        ArtFramework.frames().syncFromBackend();
        UiOpResult mount =
                ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
        assertTrue(mount.isOk());
    }

    @Test
    public void handHardSyncAndProbe() {
        FakeBackend backend = new FakeBackend();
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
        FakeBackend backend = new FakeBackend();
        mountCombatWithTwoStrikes(backend);
        artframework.sts1.FullPresentMode.setCombatHandLevel(artframework.sts1.PresentLevel.FULL);
        java.util.Map<String, Object> slice =
                ArtFramework.component(SurfaceIds.COMBAT_HAND).probeSlice();
        assertEquals("FULL", slice.get("presentLevel"));
        assertEquals(Boolean.TRUE, slice.get("fullPresent"));
        assertEquals(Boolean.TRUE, slice.get("maySuppressNative"));
    }

    @Test
    public void controlsAndMapStrongViewsInProbe() {
        FakeBackend backend = new FakeBackend();
        ArtFramework.bindPresentationBackend(backend);
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
        backend.pushFrame(
                ContextFrame.of(
                        1L,
                        1L,
                        "map",
                        java.util.Collections.<CardView>emptyList(),
                        controls,
                        map,
                        new ViewportView(1280, 720, 1280, 720)));
        ArtFramework.frames().syncFromBackend();
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
        FakeBackend backend = new FakeBackend();
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
        assertTrue(backend.intentLog().size() >= 2);
    }

    @Test
    public void dragBlockedByInterceptor() {
        FakeBackend backend = new FakeBackend();
        mountCombatWithTwoStrikes(backend);
        ArtFramework.frames()
                .addIntentInterceptor(
                        new UiSignalInterceptor() {
                            @Override
                            public Result intercept(
                                    String windowId, String controlId, String signal, Object... args) {
                                return IntentNames.BEGIN_DRAG.equals(signal)
                                        ? Result.BLOCK
                                        : Result.ALLOW;
                            }
                        });
        UiOpResult begin =
                ArtFramework.ops().invoke(SurfaceIds.COMBAT_HAND, "begin_drag", "h1");
        assertEquals(UiOpResult.Status.BLOCKED, begin.status);
        assertEquals(0, backend.intentLog().size());
    }

    @Test
    public void controlsAndMapSurfaces() {
        FakeBackend backend = new FakeBackend();
        ArtFramework.bindPresentationBackend(backend);
        ArtFramework.component(SurfaceIds.COMBAT_CONTROLS).mount();
        ArtFramework.component(SurfaceIds.MAP).mount();

        assertTrue(ArtFramework.ops().invoke(SurfaceIds.COMBAT_CONTROLS, "press").isOk());
        assertEquals(IntentNames.PRESS_END_TURN, backend.intentLog().get(0).name);

        assertTrue(
                ArtFramework.ops()
                        .invoke(SurfaceIds.MAP, "click_node", new artframework.c2.MapNodeRef(1, 2, "M"))
                        .isOk());
        assertEquals(IntentNames.CLICK_MAP_NODE, backend.intentLog().get(1).name);
    }

    @Test
    public void skeletonSurfaceActions() {
        FakeBackend backend = new FakeBackend();
        ArtFramework.bindPresentationBackend(backend);
        ArtFramework.component(SurfaceIds.SKELETON).mount();
        assertTrue(ArtFramework.ops().invoke(SurfaceIds.SKELETON, "play", "idle").isOk());
        assertEquals("play", backend.intentLog().get(0).name);
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
        FakeBackend backend = new FakeBackend();
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
        FakeBackend backend = new FakeBackend();
        mountCombatWithTwoStrikes(backend);
        UiOpResult r = ArtFramework.ops().playHandCard("Strike_R", "self");
        assertTrue(r.isOk());
        UiIntent intent = backend.intentLog().get(backend.intentLog().size() - 1);
        assertEquals(IntentNames.PLAY_CARD, intent.name);
        assertTrue(intent.args[0] instanceof CardRef);
        assertEquals("h1", ((CardRef) intent.args[0]).instanceId);
    }

    @Test
    public void probeIncludesPresentAndAssets() {
        FakeBackend backend = new FakeBackend();
        mountCombatWithTwoStrikes(backend);
        java.util.Map<String, Object> map = ArtFramework.probe().asMap();
        assertTrue(map.containsKey("present"));
        assertTrue(map.containsKey("projection"));
        assertTrue(map.containsKey("backend"));
        assertTrue(map.containsKey("assets"));
        assertEquals("fake", ((java.util.Map<?, ?>) map.get("backend")).get("id"));
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
