package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.context.ViewportView;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import artframework.sts1.assets.Sts1HostAssets;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.MapRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MapDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
        MapDrawPath.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private void mapFrame() {
        Sts1HostAssets.install();
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        MapNodeView n =
                new MapNodeView(
                        1, 2, 100f, 200f, false, true, "M", "monster", ResourceIds.MAP_NODE_MONSTER);
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "map",
                        null,
                        ControlsView.empty(),
                        new MapView(Collections.singletonList(n), 1920, 1080),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.component(SurfaceIds.MAP).mount();
    }

    @Test
    public void buildProjectsNodesWithPanZoom() {
        mapFrame();
        MapDrawPath.panZoom().setPan(10f, 20f);
        MapDrawPath.panZoom().setZoom(2f);
        List<MapDrawPath.DrawItem> items = MapDrawPath.buildFromProjection();
        assertEquals(1, items.size());
        assertEquals(1, items.get(0).row);
        assertEquals(2, items.get(0).col);
        assertEquals(100f * 2f + 10f, items.get(0).screenX, 0.01f);
        assertEquals(200f * 2f + 20f, items.get(0).screenY, 0.01f);
        assertTrue(items.get(0).artFound);
        assertTrue(items.get(0).artSource.startsWith("sts1:images/"));
    }

    @Test
    public void hitTestFindsNode() {
        mapFrame();
        MapDrawPath.DrawItem hit = MapDrawPath.hitTest(100f, 200f, 30f);
        assertNotNull(hit);
        assertEquals(1, hit.row);
        assertNull(MapDrawPath.hitTest(0f, 0f, 5f));
    }

    @Test
    public void suppressesNativeMapOnlyWhenFullReady() {
        mapFrame();
        assertFalse(MapDrawPath.shouldSuppressNativeMap());
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        assertFalse(MapDrawPath.shouldSuppressNativeMap());
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue(MapDrawPath.shouldSuppressNativeMap());
        FullPresentMode.setMapLevel(PresentLevel.OBSERVE);
        assertFalse(MapDrawPath.shouldSuppressNativeMap());
    }

    @Test
    public void mapPatchSuppressesOnlyWhenFullMountedSceneAndExecutorReady() {
        assertContinuesWithoutDelegation(Scenario.OFF);
        assertContinuesWithoutDelegation(Scenario.EXECUTORLESS);
        assertContinuesWithoutDelegation(Scenario.UNMOUNTED);
        assertContinuesWithoutDelegation(Scenario.SCENE_MISMATCH);

        resetRuntime();
        publishMapFrame("map", projectedNodes());
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        SpireReturn<Void> result = mapPrefix();

        assertTrue("FULL + map scene + mounted + executor-ready must suppress native map", result.isPresent());
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("delegateToArt"));
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("delegatedWithoutEvidence"));
    }

    @Test
    public void panicAndUnknownMapOwnerFailOpenWithoutDelegatedCoverage() {
        resetRuntime();
        publishMapFrame("map", projectedNodes());
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        PresentSafety.panic("map-test");

        SpireReturn<Void> panic = mapPrefix();

        assertFalse("panic must fail open to native map", panic.isPresent());
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("failOpen"));
        assertNoDelegatedCoverage();

        resetRuntime();
        publishMapFrame("map", projectedNodes());
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                SurfaceIds.MAP + ".unknown", "native.Unknown", "render", "unknown-owner");

        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertTrue(unknown.nativeContinuation);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("runtimeUNKNOWN"));
        assertNoDelegatedCoverage();
    }

    @Test
    public void mapVisualsUseProjectedNodeIdentityKindLabelPositionHighlightAndEvidenceCount() {
        resetRuntime();
        Sts1HostAssets.install();
        List<MapNodeView> nodes = projectedNodes();
        publishMapFrame("map", nodes);
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        MapDrawPath.panZoom().setPan(10f, 20f);
        MapDrawPath.panZoom().setZoom(1.5f);

        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.MAP));
        assertTrue(plan.shouldSuppressNative(SurfaceIds.MAP));
        invokePrepareMapVisuals(plan);

        Map<String, DrawComponent> draws = c2Draws();
        Map<String, BoundsComponent> bounds = c2Bounds();
        List<MapDrawPath.DrawItem> projected = MapDrawPath.buildFromProjection();
        assertEquals("fixture must cover multiple real projected nodes", 2, projected.size());
        assertEquals(Integer.valueOf(projected.size()), MapDrawPath.probeSlice().get("count"));
        assertEquals("projected drawCount must be node-derived", projected.size(), draws.size());

        for (MapDrawPath.DrawItem item : projected) {
            String id = "node:" + item.row + ":" + item.col;
            DrawComponent draw = draws.get(id);
            assertNotNull("missing C2 map node " + id, draw);
            assertEquals("map-node", draw.role);
            assertEquals("symbol/label must come from projected node " + id, item.symbol, draw.text);
            assertEquals("resource must resolve from projected kind/resource " + id, item.artSource, draw.resourceId);
            BoundsComponent b = bounds.get(id);
            float size = item.highlighted ? 80f : 64f;
            assertEquals(item.screenX - size / 2f, b.rect.x, 0.01f);
            assertEquals(item.screenY - size / 2f, b.rect.y, 0.01f);
            assertEquals("highlight controls node chrome size " + id, size, b.rect.width, 0.01f);
            assertEquals(size, b.rect.height, 0.01f);
        }
        assertEquals("M", draws.get("node:1:2").text);
        assertEquals("R", draws.get("node:3:1").text);
        assertTrue("highlighted node must be larger than non-highlighted node",
                bounds.get("node:1:2").rect.width > bounds.get("node:3:1").rect.width);

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.MAP, "com.megacrit.cardcrawl.screens.DungeonMapScreen", "render", "map-screen");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        invokeRenderMap();
        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals("map evidence count must come from current projected nodes",
                projected.size(), evidence.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void panZoomLimits() {
        MapPanZoom pz = new MapPanZoom();
        pz.setZoom(10f);
        assertEquals(2.5f, pz.zoom(), 0.01f);
        pz.zoomBy(0.1f);
        assertTrue(pz.zoom() >= 0.5f);
    }

    @Test
    public void probeSlice() {
        mapFrame();
        FullPresentMode.setMapLevel(PresentLevel.OBSERVE);
        Map<String, Object> m = MapDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), m.get("count"));
        assertEquals("OBSERVE", m.get("presentLevel"));
        assertEquals("map", m.get("scene"));
    }

    @Test
    public void sceneChangeResetsPanZoom() {
        mapFrame();
        MapDrawPath.buildFromProjection();
        MapDrawPath.panZoom().setPan(50f, 60f);
        MapDrawPath.panZoom().setZoom(2f);

        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        2L,
                        2L,
                        "combat",
                        Arrays.asList(),
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());

        MapDrawPath.buildFromProjection();
        assertEquals(0f, MapDrawPath.panZoom().panX(), 0.01f);
        assertEquals(0f, MapDrawPath.panZoom().panY(), 0.01f);
        assertEquals(1f, MapDrawPath.panZoom().zoom(), 0.01f);
    }

    @Test
    public void mapSceneExitClearsStaleC2NodesAndResetsPanZoom() {
        resetRuntime();
        Sts1HostAssets.install();
        publishMapFrame("map", projectedNodes());
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        MapDrawPath.panZoom().setPan(50f, 60f);
        MapDrawPath.panZoom().setZoom(2f);
        MapDrawPath.buildFromProjection();
        invokePrepareMapVisuals(Sts1RenderPipeline.plan());
        assertFalse(c2Draws().isEmpty());

        publishMapFrame("combat", Collections.<MapNodeView>emptyList());
        invokePrivate("disableInactiveSurfaceEffects", new Class<?>[] {SurfaceDrawPlan.class},
                Sts1RenderPipeline.plan());
        MapDrawPath.buildFromProjection();

        assertTrue("map C2 nodes must be removed after leaving map scene", c2Draws().isEmpty());
        assertEquals(0f, MapDrawPath.panZoom().panX(), 0.01f);
        assertEquals(0f, MapDrawPath.panZoom().panY(), 0.01f);
        assertEquals(1f, MapDrawPath.panZoom().zoom(), 0.01f);
    }

    @Test
    public void hostRecreationClearsMapVisualsEvidenceAndPanZoom() {
        resetRuntime();
        Sts1HostAssets.install();
        publishMapFrame("map", projectedNodes());
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        MapDrawPath.panZoom().setPan(7f, 8f);
        invokePrepareMapVisuals(Sts1RenderPipeline.plan());
        assertFalse(c2Draws().isEmpty());
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.MAP, "com.megacrit.cardcrawl.screens.DungeonMapScreen", "render", "map-screen");
        invokeRenderMap();
        assertNotNull(NativeRenderBridge.ledger().evidence(disposition.invocationId));

        PresentSafety.onHostRecreated();

        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertTrue("map visual nodes must be removed on host recreation", c2Draws().isEmpty());
        assertEquals(0f, MapDrawPath.panZoom().panX(), 0.01f);
        assertEquals(0f, MapDrawPath.panZoom().panY(), 0.01f);
        assertEquals(1f, MapDrawPath.panZoom().zoom(), 0.01f);
    }

    @Test
    public void epochChangeResetsPanZoom() {
        mapFrame();
        MapDrawPath.buildFromProjection();
        MapDrawPath.panZoom().setPan(50f, 60f);
        MapDrawPath.panZoom().setZoom(2f);

        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        2L,
                        2L,
                        "map",
                        Arrays.asList(),
                        ControlsView.empty(),
                        MapView.empty(),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());

        MapDrawPath.buildFromProjection();
        assertEquals(0f, MapDrawPath.panZoom().panX(), 0.01f);
        assertEquals(0f, MapDrawPath.panZoom().panY(), 0.01f);
        assertEquals(1f, MapDrawPath.panZoom().zoom(), 0.01f);
    }

    private static List<MapNodeView> projectedNodes() {
        return Arrays.asList(
                new MapNodeView(1, 2, 100f, 200f, false, true,
                        "M", "monster", ResourceIds.MAP_NODE_MONSTER),
                new MapNodeView(3, 1, 250f, 325f, true, false,
                        "R", "rest", ResourceIds.MAP_NODE_REST));
    }

    private static void publishMapFrame(String scene, List<MapNodeView> nodes) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        scene,
                        Collections.<artframework.context.CardView>emptyList(),
                        ControlsView.empty(),
                        new MapView(nodes, 1920, 1080),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private static void assertContinuesWithoutDelegation(Scenario scenario) {
        resetRuntime();
        publishMapFrame(scenario == Scenario.SCENE_MISMATCH ? "combat" : "map", projectedNodes());
        if (scenario != Scenario.UNMOUNTED) {
            ArtFramework.component(SurfaceIds.MAP).mount();
        }
        if (scenario != Scenario.OFF) {
            FullPresentMode.setMapLevel(PresentLevel.FULL);
        }
        if (scenario != Scenario.EXECUTORLESS) {
            CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        }

        SpireReturn<Void> result = mapPrefix();

        assertFalse("map " + scenario + " must continue native render", result.isPresent());
        assertNoDelegatedCoverage();
    }

    private static SpireReturn<Void> mapPrefix() {
        return MapRenderPatches.ObserveNativeMapRender.Prefix(null, null);
    }

    private static void assertNoDelegatedCoverage() {
        Map<String, Object> probe = NativeRenderBridge.probeSlice();
        assertEquals(Integer.valueOf(0), probe.get("delegateToArt"));
        assertEquals(Integer.valueOf(0), probe.get("evidenceCount"));
        assertEquals(Integer.valueOf(0), probe.get("delegatedWithoutEvidence"));
    }

    private static void resetRuntime() {
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
        MapDrawPath.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private static void invokePrepareMapVisuals(SurfaceDrawPlan plan) {
        invokePrivate("prepareMapVisuals", new Class<?>[] {SurfaceDrawPlan.class}, plan);
    }

    private static void invokeRenderMap() {
        invokePrivate("renderMap",
                new Class<?>[] {com.badlogic.gdx.graphics.g2d.SpriteBatch.class},
                new Object[] {null});
    }

    private static void invokePrivate(String name, Class<?>[] types, Object... args) {
        try {
            Method method = Sts1SurfaceRenderer.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Map<String, DrawComponent> c2Draws() {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + SurfaceIds.MAP;
        Set<String> duplicateGuard = new LinkedHashSet<String>();
        Map<String, DrawComponent> out = new LinkedHashMap<String, DrawComponent>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity == null || !scope.equals(identity.key.scope)) continue;
            assertTrue("duplicate C2 map item id " + identity.key.localId,
                    duplicateGuard.add(identity.key.localId));
            out.put(identity.key.localId, context.world().get(entity, DrawComponent.class));
        }
        return out;
    }

    private static Map<String, BoundsComponent> c2Bounds() {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + SurfaceIds.MAP;
        Map<String, BoundsComponent> out = new LinkedHashMap<String, BoundsComponent>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity == null || !scope.equals(identity.key.scope)) continue;
            out.put(identity.key.localId, context.world().get(entity, BoundsComponent.class));
        }
        return out;
    }

    private enum Scenario {
        OFF,
        EXECUTORLESS,
        UNMOUNTED,
        SCENE_MISMATCH
    }
}
