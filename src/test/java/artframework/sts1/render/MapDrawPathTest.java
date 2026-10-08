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
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.MapRenderPatches;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
        assertEquals(ResourceIds.mapOutline("monster"), items.get(0).outlineResourceId);
        assertEquals(ResourceIds.UI_MAP_HIGHLIGHT, items.get(0).highlightResourceId);
        assertEquals(160f, items.get(0).bounds.width, 0.01f);
    }

    @Test
    public void projectionExposesReachablePinnedStateAndChoosesPinResource() {
        Sts1HostAssets.install();
        MapNodeView n = new MapNodeView(4, 5, 30f, 40f, false, false, false, true,
                48f, 52f, "P", "shop", ResourceIds.MAP_NODE_SHOP);
        publishMapFrame("map", Collections.singletonList(n));
        MapDrawPath.DrawItem item = MapDrawPath.buildFromProjection().get(0);
        assertFalse(item.reachable);
        assertTrue(item.pinned);
        assertEquals(ResourceIds.UI_MAP_PIN, item.highlightResourceId);
        assertEquals(48f, item.bounds.width, 0.01f);
        assertEquals(52f, item.bounds.height, 0.01f);
    }

    @Test
    public void resolvedNodeAndOutlineColorsMatchNativeConstantsByState() {
        // D03: derive the tint from the projected state inputs, not a copied constant, and pin the
        // result against the VERIFIED native MapRoomNode constants
        // (AVAILABLE=(0.09,0.13,0.17,1), NOT_TAKEN=(0.34,0.34,0.34,1),
        //  OUTLINE=8c8c80ff=(0.549,0.549,0.502,1), highlight=(0.9,0.9,0.9,1)).
        Sts1HostAssets.install();
        List<MapNodeView> nodes = Arrays.asList(
                // taken (available false): must resolve to AVAILABLE.
                new MapNodeView(3, 1, 10f, 10f, true, false, true, false,
                        false, false, 64f, 64f, "R", "rest", ResourceIds.MAP_NODE_REST),
                // untaken + available: must resolve to AVAILABLE.
                new MapNodeView(2, 2, 20f, 20f, false, false, true, false,
                        true, false, 64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER),
                // untaken + unavailable: must resolve to NOT_TAKEN.
                new MapNodeView(1, 3, 30f, 30f, false, false, false, false,
                        false, false, 64f, 64f, "E", "elite", ResourceIds.MAP_NODE_ELITE),
                // highlighted + unavailable: node resolves to NOT_TAKEN, outline to highlight.
                new MapNodeView(0, 4, 40f, 40f, false, true, true, false,
                        false, false, 80f, 80f, "S", "shop", ResourceIds.MAP_NODE_SHOP));
        publishMapFrame("map", nodes);
        List<MapDrawPath.DrawItem> items = MapDrawPath.buildFromProjection();
        assertEquals(4, items.size());

        assertColor("taken -> AVAILABLE", MapDrawPath.AVAILABLE_COLOR, items.get(0));
        assertColor("available-untaken -> AVAILABLE", MapDrawPath.AVAILABLE_COLOR, items.get(1));
        assertColor("untaken-unavailable -> NOT_TAKEN", MapDrawPath.NOT_TAKEN_COLOR, items.get(2));
        assertColor("untaken-unavailable -> NOT_TAKEN (highlighted outline)",
                MapDrawPath.NOT_TAKEN_COLOR, items.get(3));

        // Outline: highlighted (index 3) uses the 0.9 highlight color; the rest use OUTLINE.
        for (int i = 0; i < 3; i++) {
            assertEquals("outline color index " + i, MapDrawPath.OUTLINE_COLOR[0], items.get(i).outlineR, 0.001f);
            assertEquals(MapDrawPath.OUTLINE_COLOR[1], items.get(i).outlineG, 0.001f);
            assertEquals(MapDrawPath.OUTLINE_COLOR[2], items.get(i).outlineB, 0.001f);
        }
        assertEquals("highlighted outline r", 0.9f, items.get(3).outlineR, 0.001f);
        assertEquals("highlighted outline g", 0.9f, items.get(3).outlineG, 0.001f);
        assertEquals("highlighted outline b", 0.9f, items.get(3).outlineB, 0.001f);
    }

    private static void assertColor(
            String label, float[] expected, MapDrawPath.DrawItem item) {
        assertEquals(label + " r", expected[0], item.nodeR, 0.001f);
        assertEquals(label + " g", expected[1], item.nodeG, 0.001f);
        assertEquals(label + " b", expected[2], item.nodeB, 0.001f);
        assertEquals(label + " a", expected[3], item.nodeA, 0.001f);
    }

    @Test
    public void currentNodeFlagDrivesRingSubmissionAtAvailableColor() {
        Sts1HostAssets.install();
        List<MapNodeView> nodes = Collections.singletonList(
                new MapNodeView(5, 2, 100f, 200f, false, false, true, false,
                        true, true, 64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER));
        publishMapFrame("map", nodes);
        MapDrawPath.DrawItem item = MapDrawPath.buildFromProjection().get(0);
        assertTrue("current flag must project", item.currentNode);
        assertEquals(Boolean.TRUE, item.toMap().get("currentNode"));

        List<MapDrawPath.Submission> plan = MapDrawPath.mapSubmissionPlan();
        assertTrue("current node must submit the MAP_CIRCLE_5 ring",
                containsResource(plan, ResourceIds.UI_MAP_CIRCLE_5));
        MapDrawPath.Submission ring = null;
        for (MapDrawPath.Submission s : plan) {
            if (ResourceIds.UI_MAP_CIRCLE_5.equals(s.resourceId)) ring = s;
        }
        assertNotNull(ring);
        assertEquals(MapDrawPath.AVAILABLE_COLOR[0], ring.r, 0.001f);
        assertEquals(MapDrawPath.AVAILABLE_COLOR[1], ring.g, 0.001f);
        assertEquals(MapDrawPath.AVAILABLE_COLOR[2], ring.b, 0.001f);

        // Non-current node must NOT submit a ring.
        publishMapFrame("map", Collections.singletonList(
                new MapNodeView(5, 2, 100f, 200f, false, false, true, false,
                        true, false, 64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER)));
        assertFalse(containsResource(MapDrawPath.mapSubmissionPlan(), ResourceIds.UI_MAP_CIRCLE_5));
    }

    @Test
    public void legacyMapShapeRoundTripsAvailableAndCurrent() {
        // FINDING E: the legacy map-shaped ContextFrame path (coerceMap) must not drop the D03
        // available/current fields, otherwise a round-tripped available node would render grey.
        Sts1HostAssets.install();
        Map<String, Object> rawMap = new LinkedHashMap<String, Object>();
        rawMap.put("viewportWidth", Integer.valueOf(800));
        rawMap.put("viewportHeight", Integer.valueOf(600));
        List<Map<String, Object>> nodes = new java.util.ArrayList<Map<String, Object>>();
        Map<String, Object> n = new LinkedHashMap<String, Object>();
        n.put("row", Integer.valueOf(0));
        n.put("col", Integer.valueOf(1));
        n.put("taken", Boolean.FALSE);
        n.put("highlighted", Boolean.FALSE);
        n.put("available", Boolean.TRUE);
        n.put("current", Boolean.TRUE);
        n.put("symbol", "R");
        n.put("roomKind", "rest");
        n.put("resourceId", ResourceIds.MAP_NODE_REST);
        nodes.add(n);
        rawMap.put("nodes", nodes);
        ContextFrame frame =
                new ContextFrame(9L, 3L, "map", null,
                        new LinkedHashMap<String, Object>(), rawMap, true, null);
        MapNodeView view = frame.mapView.find(0, 1);
        assertNotNull(view);
        assertTrue("available must round-trip through coerceMap", view.available);
        assertTrue("current must round-trip through coerceMap", view.current);

        // Absent fields must default false (graceful), not throw.
        Map<String, Object> n2 = new LinkedHashMap<String, Object>();
        n2.put("row", Integer.valueOf(1));
        n2.put("col", Integer.valueOf(1));
        n2.put("roomKind", "monster");
        n2.put("resourceId", ResourceIds.MAP_NODE_MONSTER);
        List<Map<String, Object>> nodes2 = new java.util.ArrayList<Map<String, Object>>();
        nodes2.add(n2);
        Map<String, Object> rawMap2 = new LinkedHashMap<String, Object>();
        rawMap2.put("nodes", nodes2);
        ContextFrame frame2 =
                new ContextFrame(10L, 4L, "map", null,
                        new LinkedHashMap<String, Object>(), rawMap2, true, null);
        assertFalse(frame2.mapView.find(1, 1).available);
        assertFalse(frame2.mapView.find(1, 1).current);
    }

    @Test
    public void nativeRingPredicateIsTakenOrCurrent() {
        // D03/native parity: MapRoomNode.render draws MAP_CIRCLE_5 when
        // `taken || (firstRoomChosen && curr)`. A TAKEN non-current node MUST submit a ring; a plain
        // untaken/unavailable non-current node MUST NOT.
        Sts1HostAssets.install();
        publishMapFrame("map", Collections.singletonList(
                new MapNodeView(1, 1, 10f, 10f, true, false, false, false,
                        false, false, 64f, 64f, "R", "rest", ResourceIds.MAP_NODE_REST)));
        MapDrawPath.DrawItem taken = MapDrawPath.buildFromProjection().get(0);
        assertTrue("taken node", taken.taken);
        assertFalse("taken node is not the current node", taken.currentNode);
        List<MapDrawPath.Submission> takenPlan = MapDrawPath.mapSubmissionPlan();
        assertTrue("taken non-current node MUST submit the MAP_CIRCLE_5 ring",
                containsResource(takenPlan, ResourceIds.UI_MAP_CIRCLE_5));

        publishMapFrame("map", Collections.singletonList(
                new MapNodeView(1, 1, 10f, 10f, false, false, false, false,
                        false, false, 64f, 64f, "R", "rest", ResourceIds.MAP_NODE_REST)));
        MapDrawPath.DrawItem plain = MapDrawPath.buildFromProjection().get(0);
        assertFalse("plain node", plain.taken);
        assertFalse("plain node is not the current node", plain.currentNode);
        assertFalse("plain untaken/unavailable node MUST NOT submit a ring",
                containsResource(MapDrawPath.mapSubmissionPlan(), ResourceIds.UI_MAP_CIRCLE_5));
    }

    @Test
    public void probeNodeColorMatchesNativeConstantForUntaken() {
        // The probe-visible color the D1 scenario asserts on: an untaken/unavailable node reports
        // NOT_TAKEN (0.34,0.34,0.34,1).
        Sts1HostAssets.install();
        MapNodeView n = new MapNodeView(1, 1, 50f, 50f, false, false, true, false,
                false, false, 64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER);
        publishMapFrame("map", Collections.singletonList(n));
        Map<String, Object> probe = MapDrawPath.probeSlice();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) probe.get("items");
        assertEquals(1, items.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> color = (Map<String, Object>) items.get(0).get("color");
        assertNotNull(color);
        assertEquals(Boolean.FALSE, items.get(0).get("available"));
        assertEquals(0.34f, ((Float) color.get("r")).floatValue(), 0.001f);
        assertEquals(0.34f, ((Float) color.get("g")).floatValue(), 0.001f);
        assertEquals(0.34f, ((Float) color.get("b")).floatValue(), 0.001f);
        assertEquals(1f, ((Float) color.get("a")).floatValue(), 0.001f);
    }

    @Test
    public void colorSamplesDeriveNativeConstantsFromStatePredicates() {
        // D03: the pure probe samples must equal the VERIFIED native constants, derived from the
        // state predicates (taken/available/highlighted), computed here independently.
        Sts1HostAssets.install();
        publishMapFrame("map", Collections.<MapNodeView>emptyList());
        Map<String, Object> probe = MapDrawPath.probeSlice();
        @SuppressWarnings("unchecked")
        Map<String, Object> samples = (Map<String, Object>) probe.get("colorSamples");
        assertNotNull(samples);
        // native AVAILABLE_COLOR = (0.09,0.13,0.17,1)
        assertEquals("17212bff", samples.get("availableColorHex"));
        assertEquals("17212bff", samples.get("takenColorHex"));
        // native NOT_TAKEN_COLOR = (0.34,0.34,0.34,1)
        assertEquals("575757ff", samples.get("untakenColorHex"));
        // native OUTLINE_COLOR = 8c8c80ff
        assertEquals("8c8c80ff", samples.get("outlineColorHex"));
        // native highlight = (0.9,0.9,0.9,1)
        assertEquals("e6e6e6ff", samples.get("highlightColorHex"));
    }

    @Test
    public void hitTestFindsNode() {        mapFrame();
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

    /**
     * G5 native-continuation guard (V05). The failure mode under test is "read nothing -> blank map
     * -> still suppress native": when the map projection is UNUSABLE (no presentable nodes) while the
     * surface is otherwise FULL + mounted + in the map scene, ART must NOT suppress the native
     * {@code DungeonMapScreen.render}. Derived from the production rule
     * ({@code Sts1RenderPipeline.mapProjectionUsable()}), not restated: the map is ready only when the
     * projection carries >= 1 node. Here the projection is {@code MapView.empty()}, so
     * {@code mapSubmissionPlan()} is empty and the native patch must Continue.
     */
    @Test
    public void unusableEmptyMapProjectionKeepsNativeMapInsteadOfSuppressing() {
        resetRuntime();
        Sts1HostAssets.install();
        publishMapFrame("map", Collections.<MapNodeView>emptyList());
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        // Empty projection => map not usable => no delegation, native continues.
        assertFalse("empty map projection must NOT suppress native map",
                MapDrawPath.shouldSuppressNativeMap());
        assertTrue("empty map projection contributes no submissions (nothing to draw)",
                MapDrawPath.mapSubmissionPlan().isEmpty());
        assertFalse("map patch must continue native render for an empty projection",
                mapPrefix().isPresent());
        assertNoDelegatedCoverage();

        // Positive control: the SAME fixture with a usable projection DOES suppress native, proving
        // the guard keys on projection usability (not merely scene/mount/executor).
        publishMapFrame("map", projectedNodes());
        assertTrue("usable map projection must suppress native map",
                MapDrawPath.shouldSuppressNativeMap());
    }

    /**
     * G5 degeneracy note (V05): {@code MapNodeView} normalizes a non-positive width/height to 64f
     * ({@code MapNodeView} ~:100), so a "nodes present but zero-size bounds" projection is not a
     * reachable state through the normal backend; the only unusable projection is the empty one
     * (covered above). This pins the normalization so a future zero-size node cannot silently be
     * treated as an unusable map (which would wrongly keep native while ART still has node pixels).
     */
    @Test
    public void zeroSizeMapNodeIsNormalizedSoItRemainsAUsableNode() {
        Sts1HostAssets.install();
        MapNodeView n = new MapNodeView(0, 0, 5f, 5f, false, false, true, false, 0f, 0f,
                "M", "monster", ResourceIds.MAP_NODE_MONSTER);
        assertEquals("non-positive width must normalize to a positive node size", 64f, n.width, 0.01f);
        assertEquals("non-positive height must normalize to a positive node size", 64f, n.height, 0.01f);
        publishMapFrame("map", Collections.singletonList(n));
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        assertTrue("a node-carrying projection is usable and delegates the map",
                MapDrawPath.shouldSuppressNativeMap());
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
        assertEquals("projected drawCount must include visible node overlays", 5, draws.size());

        for (MapDrawPath.DrawItem item : projected) {
            String id = "node:" + item.row + ":" + item.col;
            DrawComponent draw = draws.get(id);
            assertNotNull("missing C2 map node " + id, draw);
            assertEquals("map-node", draw.role);
            assertEquals("symbol/label must come from projected node " + id, item.symbol, draw.text);
            assertEquals("resource must resolve from projected kind/resource " + id, item.artSource, draw.resourceId);
            BoundsComponent b = bounds.get(id);
            assertEquals(item.bounds.x, b.rect.x, 0.01f);
            assertEquals(item.bounds.y, b.rect.y, 0.01f);
            assertEquals("projected geometry must control node bounds", item.bounds.width,
                    b.rect.width, 0.01f);
            assertEquals(item.bounds.height, b.rect.height, 0.01f);
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
        assertEquals("map evidence count must come from current projected nodes/overlays/rings",
                6, evidence.drawCount);
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
    public void legendIsOmittedWhenSettingsUnavailable() {
        // JUnit leaves Settings.scale/xScale/yScale at their 0f defaults (no display init), so the
        // legend must fail open and contribute ZERO items, keeping the node-only supply unchanged.
        mapFrame();
        setSettingsScale(0f, 0f, 0f);
        assertTrue("legend must be omitted when Settings is unavailable",
                MapDrawPath.legendItems().isEmpty());
        Map<String, Object> probe = MapDrawPath.probeSlice();
        assertEquals(Integer.valueOf(0), probe.get("legendCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> legend = (Map<String, Object>) probe.get("legend");
        assertNotNull(legend);
        assertNull("no panel without Settings", legend.get("panel"));
        assertTrue(((java.util.List<?>) legend.get("items")).isEmpty());
        assertEquals("node supply must be unchanged when the legend is omitted",
                Integer.valueOf(1), probe.get("count"));
    }

    @Test
    public void legendMatchesNativePanelAndIconGeometryAtScaleOne() {
        // Native Legend.render / LegendItem.render REST constants (desktop), pinned at scale=1:
        //   X = 1670*xScale, Y = 600*yScale
        //   panel: (X-256, Y-400, 512*scale, 800*yScale)
        //   icon i: (ICON_X-64, Y - SPACE_Y*i + OFFSET_Y - 64, 128*scale/1.65, same)
        //           ICON_X = 1575*xScale, SPACE_Y = 58*yScale, OFFSET_Y = 100*yScale
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            mapFrame();

            List<MapDrawPath.LegendDrawItem> items = MapDrawPath.legendItems();
            assertEquals("panel + title + 6 room rows", 8, items.size());

            MapDrawPath.LegendDrawItem panel = legendById(items, "legend.panel");
            assertNotNull(panel);
            assertEquals(ResourceIds.UI_MAP_LEGEND, panel.resourceId);
            assertEquals("map-legend", panel.role);
            assertEquals(1670f - 256f, panel.bounds.x, 0.01f);
            assertEquals(600f - 400f, panel.bounds.y, 0.01f);
            assertEquals(512f, panel.bounds.width, 0.01f);
            assertEquals(800f, panel.bounds.height, 0.01f);

            MapDrawPath.LegendDrawItem title = legendById(items, "legend.title");
            assertNotNull(title);
            assertEquals("Legend", title.label);
            assertEquals(1670f, title.bounds.x + title.bounds.width / 2f, 0.01f);
            assertEquals(600f + 170f, title.bounds.y + title.bounds.height / 2f, 0.01f);

            String[] rooms = {"event", "merchant", "treasure", "rest", "enemy", "elite"};
            String[] resources = {
                ResourceIds.MAP_NODE_EVENT, ResourceIds.MAP_NODE_SHOP,
                ResourceIds.MAP_NODE_TREASURE, ResourceIds.MAP_NODE_REST,
                ResourceIds.MAP_NODE_MONSTER, ResourceIds.MAP_NODE_ELITE
            };
            float icon = 128f / 1.65f;
            for (int i = 0; i < rooms.length; i++) {
                MapDrawPath.LegendDrawItem row = legendById(items, "legend:" + rooms[i]);
                assertNotNull("missing legend row " + rooms[i], row);
                assertEquals(i, row.index);
                assertEquals(resources[i], row.resourceId);
                assertEquals("map-legend", row.role);
                assertEquals(1575f - 64f, row.bounds.x, 0.01f);
                assertEquals(600f - 58f * i + 100f - 64f, row.bounds.y, 0.01f);
                assertEquals(icon, row.bounds.width, 0.01f);
                assertEquals(icon, row.bounds.height, 0.01f);
            }
            assertEquals("Event", legendById(items, "legend:event").label);
            assertEquals("Merchant", legendById(items, "legend:merchant").label);
            assertEquals("Treasure", legendById(items, "legend:treasure").label);
            assertEquals("Rest", legendById(items, "legend:rest").label);
            assertEquals("Enemy", legendById(items, "legend:enemy").label);
            assertEquals("Elite", legendById(items, "legend:elite").label);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void legendScalesWithSettings() {
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1.25f, 1.5f, 1.1f);
            MapDrawPath.LegendDrawItem panel = legendById(MapDrawPath.legendItems(), "legend.panel");
            assertNotNull(panel);
            assertEquals(1670f * 1.5f - 256f, panel.bounds.x, 0.01f);
            assertEquals(600f * 1.1f - 400f, panel.bounds.y, 0.01f);
            assertEquals(512f * 1.25f, panel.bounds.width, 0.01f);
            assertEquals(800f * 1.1f, panel.bounds.height, 0.01f);
            MapDrawPath.LegendDrawItem event = legendById(MapDrawPath.legendItems(), "legend:event");
            assertEquals(1575f * 1.5f - 64f, event.bounds.x, 0.01f);
            assertEquals(600f * 1.1f + 100f * 1.1f - 64f, event.bounds.y, 0.01f);
            assertEquals(128f * (1.25f / 1.65f), event.bounds.width, 0.01f);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void legendRoomIconsCarryAvailableColorTintNotWhite() {
        // V04 real visual defect: the 6 legend room icons were drawn with the generic WHITE tint, so
        // they were invisible on the light parchment. Native Legend.render computes
        // c2 = (MapRoomNode.AVAILABLE_COLOR.r/g/b, c.a) and sets it before each LegendItem.render, so
        // every room row (icon AND label) must resolve to (0.09,0.13,0.17), NOT white.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            mapFrame();
            String[] rooms = {"event", "merchant", "treasure", "rest", "enemy", "elite"};
            for (String room : rooms) {
                MapDrawPath.LegendDrawItem row =
                        legendById(MapDrawPath.legendItems(), "legend:" + room);
                assertNotNull("missing legend room row " + room, row);
                assertTrue("room row must be label-left-aligned", row.labelLeftAligned);
                assertEquals("room " + room + " r", MapDrawPath.AVAILABLE_COLOR[0], row.r, 0.001f);
                assertEquals("room " + room + " g", MapDrawPath.AVAILABLE_COLOR[1], row.g, 0.001f);
                assertEquals("room " + room + " b", MapDrawPath.AVAILABLE_COLOR[2], row.b, 0.001f);
                assertEquals("room " + room + " a", 1f, row.a, 0.001f);
                // Non-white proof (native AVAILABLE rgb is never 1): the pre-fix white tint would
                // read ffffffff and fail this assertion.
                assertFalse("room icon tint must not be white",
                        row.r == 1f && row.g == 1f && row.b == 1f);
                assertEquals("17212bff", row.toMap().get("colorHex"));
            }
            // Panel/title keep the neutral theme draw (no c2 tint).
            MapDrawPath.LegendDrawItem panel =
                    legendById(MapDrawPath.legendItems(), "legend.panel");
            assertNotNull(panel);
            assertEquals(1f, panel.r, 0.001f);
            assertEquals(1f, panel.g, 0.001f);
            assertEquals(1f, panel.b, 0.001f);
            assertFalse("the title is not a left-aligned room row",
                    legendById(MapDrawPath.legendItems(), "legend.title").labelLeftAligned);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void legendLabelsUseNativeLeftAlignedTextColumnNotIconCenter() {
        // V04: native LegendItem.render draws the label LEFT-TOP aligned at
        // (TEXT_X - 50*scale, Legend.Y - SPACE_Y*index + OFFSET_Y + 13*yScale), NOT centered on the
        // 128px icon box (the ART defect put the label in the icon column). Expected values are
        // derived here from the VERIFIED native constants TEXT_X=1670, SPACE_Y=58, OFFSET_Y=100.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            mapFrame();
            String[] rooms = {"event", "merchant", "treasure", "rest", "enemy", "elite"};
            for (int i = 0; i < rooms.length; i++) {
                MapDrawPath.LegendDrawItem row =
                        legendById(MapDrawPath.legendItems(), "legend:" + rooms[i]);
                assertNotNull(row);
                float expectedX = 1670f - 50f;
                float expectedTopY = 600f - 58f * i + 100f + 13f;
                assertEquals("room " + i + " label x", expectedX, row.labelX, 0.01f);
                assertEquals("room " + i + " label top y", expectedTopY, row.labelTopY, 0.01f);
                float iconCenterX = row.bounds.x + row.bounds.width * 0.5f;
                assertFalse("label must NOT be centered in the icon column for " + rooms[i],
                        Math.abs(row.labelX - iconCenterX) < 0.01f);
                assertTrue("native text column sits right of the icon box center",
                        row.labelX > iconCenterX);
            }

            // Scale dependence: with scale=1.25, xScale=1.5, yScale=1.1 the label is
            // (1670*1.5 - 50*1.25, 600*1.1 - 58*1.1*i + 100*1.1 + 13*1.1) — derived, not copied.
            setSettingsScale(1.25f, 1.5f, 1.1f);
            MapDrawPath.LegendDrawItem scaled =
                    legendById(MapDrawPath.legendItems(), "legend:event");
            assertNotNull(scaled);
            assertEquals(1670f * 1.5f - 50f * 1.25f, scaled.labelX, 0.01f);
            assertEquals(600f * 1.1f + 100f * 1.1f + 13f * 1.1f, scaled.labelTopY, 0.01f);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void legendIconScaleReproducesNativeDesktopRestBranch() {
        // The native LegendItem.render scale is a STATE branch (desktop rest /1.65, desktop hover
        // /1.2, mobile hover /1, mobile rest /1.3), NOT a per-row scale. ART draws the desktop REST
        // state, so EVERY row is 128*scale/1.65. The brief's "per-row branch" read is corrected here
        // against the decompiled bytecode.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1.25f, 1f, 1f);
            mapFrame();
            float expected = 128f * (1.25f / 1.65f);
            String[] rooms = {"event", "merchant", "treasure", "rest", "enemy", "elite"};
            for (String room : rooms) {
                MapDrawPath.LegendDrawItem row =
                        legendById(MapDrawPath.legendItems(), "legend:" + room);
                assertNotNull(row);
                assertEquals("room " + room + " icon width", expected, row.bounds.width, 0.01f);
                assertEquals("room " + room + " icon height", expected, row.bounds.height, 0.01f);
                assertEquals("room " + room + " iconScale", expected, row.iconScale, 0.01f);
            }
            // The REST branch must not be the HOVER branch (they differ by 1.65/1.2).
            assertFalse("rest icon scale must not equal the hover scale",
                    Math.abs(expected - 128f * (1.25f / 1.2f)) < 0.01f);
            assertEquals(1.25f / 1.2f, MapDrawPath.legendIconScaleHover(1.25f), 0.0001f);
            assertEquals(1.25f / 1.65f, MapDrawPath.legendIconScale(1.25f), 0.0001f);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void mapSubmissionPlanLegendRowsCarryC2TintAndLeftAlignedLabel() {
        // V04: the renderMap submission plan must carry the native c2 tint + left-aligned label
        // geometry for the 6 room rows (2..7), so renderMap can draw them without centering the label
        // in the icon box. Order/content is unchanged (panel, title, then 6 rows).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            Sts1HostAssets.install();
            publishMapFrame("map", projectedNodes());
            List<MapDrawPath.Submission> plan = MapDrawPath.mapSubmissionPlan();

            assertEquals(ResourceIds.UI_MAP_LEGEND, plan.get(0).resourceId);
            assertFalse("legend panel label is centered", plan.get(0).labelLeftAligned);
            assertEquals("Legend", plan.get(1).label);
            assertFalse("legend title label is centered", plan.get(1).labelLeftAligned);

            for (int i = 0; i < 6; i++) {
                MapDrawPath.Submission row = plan.get(2 + i);
                assertTrue("legend row " + i + " must be left-aligned", row.labelLeftAligned);
                assertFalse("legend row " + i + " label present", row.label.isEmpty());
                assertEquals(MapDrawPath.AVAILABLE_COLOR[0], row.r, 0.001f);
                assertEquals(MapDrawPath.AVAILABLE_COLOR[1], row.g, 0.001f);
                assertEquals(MapDrawPath.AVAILABLE_COLOR[2], row.b, 0.001f);
                assertEquals(1f, row.a, 0.001f);
                assertFalse("legend row tint must not be white",
                        row.r == 1f && row.g == 1f && row.b == 1f);
                assertEquals(1670f - 50f, row.labelX, 0.01f);
                assertEquals(600f - 58f * i + 100f + 13f, row.labelTopY, 0.01f);
            }
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void probeExposesLegendIconTintAndLabelPlacement() {
        // V04 device-visible evidence: backend.mapDraw.legend.items[i] must expose the resolved
        // AVAILABLE_COLOR c2 tint (non-white) and the left-aligned label geometry for each room row.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            mapFrame();
            Map<String, Object> probe = MapDrawPath.probeSlice();
            @SuppressWarnings("unchecked")
            Map<String, Object> legend = (Map<String, Object>) probe.get("legend");
            assertNotNull(legend);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) legend.get("items");
            // items[0] is the title; items[1..6] are the 6 room rows (panel excluded).
            assertEquals(7, rows.size());
            assertEquals("Legend", rows.get(0).get("label"));
            assertEquals(Boolean.FALSE, rows.get(0).get("labelLeftAligned"));

            String[] rooms = {"event", "merchant", "treasure", "rest", "enemy", "elite"};
            for (int i = 0; i < rooms.length; i++) {
                Map<String, Object> row = rows.get(1 + i);
                assertEquals("legend:" + rooms[i], row.get("id"));
                assertEquals("17212bff", row.get("colorHex"));
                @SuppressWarnings("unchecked")
                Map<String, Object> color = (Map<String, Object>) row.get("color");
                assertEquals(0.09f, ((Float) color.get("r")).floatValue(), 0.001f);
                assertEquals(0.13f, ((Float) color.get("g")).floatValue(), 0.001f);
                assertEquals(0.17f, ((Float) color.get("b")).floatValue(), 0.001f);
                assertEquals(Boolean.TRUE, row.get("labelLeftAligned"));
                assertEquals(1670f - 50f, ((Float) row.get("labelX")).floatValue(), 0.01f);
                assertEquals(600f - 58f * i + 100f + 13f,
                        ((Float) row.get("labelTopY")).floatValue(), 0.01f);
            }
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void probeSliceExposesLegendShape() {
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            mapFrame();
            Map<String, Object> probe = MapDrawPath.probeSlice();
            assertEquals(Integer.valueOf(8), probe.get("legendCount"));
            @SuppressWarnings("unchecked")
            Map<String, Object> legend = (Map<String, Object>) probe.get("legend");
            assertNotNull(legend);
            @SuppressWarnings("unchecked")
            Map<String, Object> panel = (Map<String, Object>) legend.get("panel");
            assertNotNull(panel);
            assertEquals(ResourceIds.UI_MAP_LEGEND, panel.get("resourceId"));
            assertEquals(1414f, ((Float) panel.get("x")).floatValue(), 0.01f);
            assertEquals(200f, ((Float) panel.get("y")).floatValue(), 0.01f);
            assertEquals(512f, ((Float) panel.get("w")).floatValue(), 0.01f);
            assertEquals(800f, ((Float) panel.get("h")).floatValue(), 0.01f);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (java.util.List<Map<String, Object>>) legend.get("items");
            assertEquals("panel excluded from the item list", 7, rows.size());
            assertEquals("legend.panel", panel.get("id"));
            assertEquals("legend:event", rows.get(1).get("id"));
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void legendCatalogMapsPanelAndIconTextures() {
        Map<String, String> catalog = Sts1VanillaCatalog.catalog();
        assertEquals("sts1:images/ui/map/legend2.png", catalog.get(ResourceIds.UI_MAP_LEGEND));
        assertTrue(Sts1VanillaCatalog.isKnown(ResourceIds.UI_MAP_LEGEND));
        // The 6 legend icon ids reuse the existing MAP_NODE_* textures (no new icon ids).
        assertEquals("sts1:images/ui/map/event.png", catalog.get(ResourceIds.MAP_NODE_EVENT));
        assertEquals("sts1:images/ui/map/shop.png", catalog.get(ResourceIds.MAP_NODE_SHOP));
        assertEquals("sts1:images/ui/map/chest.png", catalog.get(ResourceIds.MAP_NODE_TREASURE));
        assertEquals("sts1:images/ui/map/rest.png", catalog.get(ResourceIds.MAP_NODE_REST));
        assertEquals("sts1:images/ui/map/monster.png", catalog.get(ResourceIds.MAP_NODE_MONSTER));
        assertEquals("sts1:images/ui/map/elite.png", catalog.get(ResourceIds.MAP_NODE_ELITE));
        // Legend room ordering matches native Legend() construction index 0..5.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            String[] expected = {
                ResourceIds.MAP_NODE_EVENT, ResourceIds.MAP_NODE_SHOP,
                ResourceIds.MAP_NODE_TREASURE, ResourceIds.MAP_NODE_REST,
                ResourceIds.MAP_NODE_MONSTER, ResourceIds.MAP_NODE_ELITE
            };
            String[] actual = new String[6];
            for (MapDrawPath.LegendDrawItem item : MapDrawPath.legendItems()) {
                if (item.index >= 0 && item.index < 6) actual[item.index] = item.resourceId;
            }
            for (int i = 0; i < 6; i++) {
                assertEquals(expected[i], actual[i]);
            }
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void ringGeometryMatchesNativeFormulaAndRotation() {
        // M01: native MapRoomNode.render draws MAP_CIRCLE_5 with final size
        //   192 * (this.scale*0.95f + 0.2f) * Settings.scale
        // centered on the node and rotated by this.angle. The expected values are derived from the
        // VERIFIED native constants here (not copied from the implementation): at nodeScale=1 the
        // factor is (0.95+0.2)=1.15, so 192*1.15 = 220.8; at nodeScale=0.68 it is
        // (0.646+0.2)=0.846, so 192*0.846 = 162.432.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            Sts1HostAssets.install();
            // Two current nodes with different native scale + angle.
            List<MapNodeView> nodes = Arrays.asList(
                    new MapNodeView(5, 2, 100f, 200f, false, false, true, false,
                            true, true, 64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER,
                            1f, 30f),
                    new MapNodeView(4, 3, 300f, 400f, true, false, false, false,
                            false, false, 64f, 64f, "R", "rest", ResourceIds.MAP_NODE_REST,
                            0.68f, -45f));
            publishMapFrame("map", nodes);

            MapDrawPath.DrawItem current = MapDrawPath.buildFromProjection().get(0);
            MapDrawPath.DrawItem taken = MapDrawPath.buildFromProjection().get(1);
            assertEquals("projected nodeScale must round-trip", 1f, current.nodeScale, 0.0001f);
            assertEquals("projected angle must round-trip", 30f, current.angle, 0.0001f);
            assertEquals(0.68f, taken.nodeScale, 0.0001f);
            assertEquals(-45f, taken.angle, 0.0001f);

            List<MapDrawPath.Submission> plan = MapDrawPath.mapSubmissionPlan();
            List<MapDrawPath.Submission> rings = new ArrayList<MapDrawPath.Submission>();
            for (MapDrawPath.Submission s : plan) {
                if (ResourceIds.UI_MAP_CIRCLE_5.equals(s.resourceId)) rings.add(s);
            }
            assertEquals("both gated nodes submit a ring (taken || current)", 2, rings.size());

            // Ring 0: currentNode, nodeScale 1 -> 192*(1*0.95+0.2)*1 = 220.8, rotation 30.
            MapDrawPath.Submission r0 = rings.get(0);
            float expected0 = 192f * (1f * 0.95f + 0.2f) * 1f;
            assertEquals("native formula size at nodeScale=1", expected0, r0.bounds.width, 0.01f);
            assertEquals(expected0, r0.bounds.height, 0.01f);
            assertEquals("ring centered on node x", current.screenX, r0.bounds.x + r0.bounds.width / 2f, 0.01f);
            assertEquals("ring centered on node y", current.screenY, r0.bounds.y + r0.bounds.height / 2f, 0.01f);
            assertEquals("rotation == projected angle", 30f, r0.rotationDegrees, 0.0001f);
            assertEquals("ring tint = AVAILABLE_COLOR r", MapDrawPath.AVAILABLE_COLOR[0], r0.r, 0.001f);
            assertEquals(MapDrawPath.AVAILABLE_COLOR[1], r0.g, 0.001f);
            assertEquals(MapDrawPath.AVAILABLE_COLOR[2], r0.b, 0.001f);
            assertEquals(MapDrawPath.AVAILABLE_COLOR[3], r0.a, 0.001f);

            // Ring 1: taken, nodeScale 0.68 -> 192*(0.68*0.95+0.2)*1 = 162.432, rotation -45.
            MapDrawPath.Submission r1 = rings.get(1);
            float expected1 = 192f * (0.68f * 0.95f + 0.2f) * 1f;
            assertEquals(expected1, r1.bounds.width, 0.01f);
            assertEquals("the two rings must differ (nodeScale honored)", true,
                    Math.abs(r0.bounds.width - r1.bounds.width) > 1f);
            assertEquals("rotation == projected angle (taken node)", -45f, r1.rotationDegrees, 0.0001f);
            assertEquals("ring centered on taken node x", taken.screenX, r1.bounds.x + r1.bounds.width / 2f, 0.01f);
            assertEquals("ring centered on taken node y", taken.screenY, r1.bounds.y + r1.bounds.height / 2f, 0.01f);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void ringSizeHelperDerivesNativeFactorAndFailsOpen() {
        // Pure helper: 192*(nodeScale*0.95+0.2)*scale*zoom derived from the native expression.
        assertEquals(192f * 1.15f, MapDrawPath.ringSize(1f, 1f, 1f), 0.01f);
        assertEquals(192f * 0.846f, MapDrawPath.ringSize(0.68f, 1f, 1f), 0.01f);
        assertEquals(192f * 1.15f * 1.5f, MapDrawPath.ringSize(1f, 1f, 1.5f), 0.01f);
        assertEquals(192f * 1.15f * 1.25f, MapDrawPath.ringSize(1f, 1.25f, 1f), 0.01f);
        // Fail-open: a bad scale/zoom is treated as 1 (never collapses the ring).
        assertEquals(192f * 1.15f, MapDrawPath.ringSize(1f, 0f, 0f), 0.01f);
        assertEquals(192f * 1.15f, MapDrawPath.ringSize(Float.NaN, 1f, 1f), 0.01f);
    }

    @Test
    public void ringProbeExposesGeometryRotationAndResolvedResource() {
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            Sts1HostAssets.install();
            publishMapFrame("map", Collections.singletonList(
                    new MapNodeView(5, 2, 100f, 200f, false, false, true, false,
                            true, true, 64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER,
                            1f, 12.5f)));
            Map<String, Object> probe = MapDrawPath.probeSlice();
            assertEquals(Integer.valueOf(1), probe.get("ringCount"));
            @SuppressWarnings("unchecked")
            Map<String, Object> ring = (Map<String, Object>) probe.get("ring");
            assertNotNull(ring);
            assertEquals(ResourceIds.UI_MAP_CIRCLE_5, ring.get("resourceId"));
            assertEquals("ui.map.circle5", ResourceIds.UI_MAP_CIRCLE_5);
            assertEquals("17212bff", ring.get("colorHex"));
            assertEquals(12.5f, ((Float) ring.get("rotation")).floatValue(), 0.0001f);
            assertEquals(220.8f, ((Float) ring.get("w")).floatValue(), 0.01f);
            // The circle5 catalog id must resolve file-backed (native images/ui/map/circle5.png).
            artframework.assets.AssetResolveResult r = ArtFramework.assets().resolve(
                    ResourceIds.UI_MAP_CIRCLE_5);
            assertTrue(r.found);
            assertTrue(artframework.sts1.assets.Sts1AssetMaterializer.isFileBacked(r.source));
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    private static MapDrawPath.LegendDrawItem legendById(
            List<MapDrawPath.LegendDrawItem> items, String id) {
        for (MapDrawPath.LegendDrawItem item : items) {
            if (id.equals(item.id)) return item;
        }
        return null;
    }

    private static void setSettingsScale(float scale, float xScale, float yScale) {
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(scale));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(xScale));
        setStaticField(com.megacrit.cardcrawl.core.Settings.class, "yScale", Float.valueOf(yScale));
    }

    private static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            java.lang.reflect.Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set static field " + owner + "." + name, failure);
        }
    }

    @Test
    public void localizedLegendTextFlowsIntoItemsProbeAndSubmissionPlan() {
        // NRO-04 D03 follow-up: a projection carrying native-localized Legend UIStrings must drive
        // the resolved legend text (not the English defaults). Expected values are derived from the
        // input projection, not a copied constant.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            Sts1HostAssets.install();
            String title = "LGD-TITLE";
            List<String> labels = new ArrayList<String>();
            for (int i = 0; i < 6; i++) {
                labels.add("LGD-" + i);
            }
            publishMapFrame("map", projectedNodes(),
                    new MapView.LegendLabels(title, labels));

            List<MapDrawPath.LegendDrawItem> items = MapDrawPath.legendItems();
            MapDrawPath.LegendDrawItem titleItem = legendById(items, "legend.title");
            assertNotNull(titleItem);
            assertEquals("localized title must reach legend.title", title, titleItem.label);

            String[] rooms = {"event", "merchant", "treasure", "rest", "enemy", "elite"};
            for (int i = 0; i < rooms.length; i++) {
                MapDrawPath.LegendDrawItem row = legendById(items, "legend:" + rooms[i]);
                assertNotNull("missing legend row " + rooms[i], row);
                assertEquals("localized label " + i + " must reach the LegendDrawItem",
                        labels.get(i), row.label);
            }

            Map<String, Object> probe = MapDrawPath.probeSlice();
            assertEquals(title, probe.get("legendTitle"));
            @SuppressWarnings("unchecked")
            List<String> probeLabels = (List<String>) probe.get("legendLabels");
            assertEquals(labels, probeLabels);
            @SuppressWarnings("unchecked")
            Map<String, Object> legend = (Map<String, Object>) probe.get("legend");
            assertEquals(title, legend.get("title"));
            assertEquals(labels, legend.get("labels"));

            // The submission plan carries the localized labels/labels-only title in native order.
            List<MapDrawPath.Submission> plan = MapDrawPath.mapSubmissionPlan();
            assertEquals(title, plan.get(1).label);
            for (int i = 0; i < labels.size(); i++) {
                assertEquals(labels.get(i), plan.get(2 + i).label);
            }
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void emptyOrPartialLegendLabelsFallBackToEnglishAsWholeSet() {
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            Sts1HostAssets.install();
            String[] defaults = {"Event", "Merchant", "Treasure", "Rest", "Enemy", "Elite"};

            // Absent legend (legacy 3-arg MapView): must equal the English defaults.
            publishMapFrame("map", projectedNodes());
            assertLegendEquals(MapDrawPath.legendItems(), "Legend", defaults);

            // Explicit empty legend value: still the English defaults.
            publishMapFrame("map", projectedNodes(), MapView.LegendLabels.empty());
            assertLegendEquals(MapDrawPath.legendItems(), "Legend", defaults);
            Map<String, Object> probe = MapDrawPath.probeSlice();
            assertEquals("Legend", probe.get("legendTitle"));

            // ALL-OR-NOTHING: an incomplete localized set (blank + null + short) must make the WHOLE
            // legend English — localized event + English rest (mixed languages) is forbidden.
            publishMapFrame("map", projectedNodes(),
                    new MapView.LegendLabels("", Arrays.asList("LOC-0", "", null, "LOC-3")));
            assertLegendEquals(MapDrawPath.legendItems(), "Legend", defaults);
            Map<String, Object> partialProbe = MapDrawPath.probeSlice();
            assertEquals("Legend", partialProbe.get("legendTitle"));
            assertEquals(Arrays.asList(defaults),
                    (List<String>) partialProbe.get("legendLabels"));
            assertFalse("partial set must not keep the localized event label in the probe",
                    ((List<String>) partialProbe.get("legendLabels")).contains("LOC-0"));

            // A complete set WITH a localized title is used in full (no mixed languages).
            List<String> complete = Arrays.asList(
                    "LOC-0", "LOC-1", "LOC-2", "LOC-3", "LOC-4", "LOC-5");
            publishMapFrame("map", projectedNodes(),
                    new MapView.LegendLabels("LOC-TITLE", complete));
            assertLegendEquals(MapDrawPath.legendItems(), "LOC-TITLE",
                    complete.toArray(new String[0]));

            // A complete label set but a BLANK title must also fall the whole legend back to English
            // (never a localized label set under an English title, or vice versa).
            publishMapFrame("map", projectedNodes(),
                    new MapView.LegendLabels("", complete));
            assertLegendEquals(MapDrawPath.legendItems(), "Legend", defaults);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    private static void assertLegendEquals(
            List<MapDrawPath.LegendDrawItem> items, String title, String[] labels) {
        assertEquals(title, legendById(items, "legend.title").label);
        String[] rooms = {"event", "merchant", "treasure", "rest", "enemy", "elite"};
        for (int i = 0; i < rooms.length; i++) {
            assertEquals(labels[i], legendById(items, "legend:" + rooms[i]).label);
        }
    }

    @Test
    public void submissionPlanSubmitsNodeIconOutlineHighlightAndLegend() {
        // NRO-04 D03 defect fix: renderMap must SUBMIT mapped pixels, not merely count them.
        // Three nodes exercise node-icon + outline + highlight + ring variants:
        //  - reachable+highlighted monster: icon + outline + highlight
        //  - pinned shop:                    icon + highlight only (no reachable/highlighted)
        //  - taken rest (neither):           icon + MAP_CIRCLE_5 ring (native `taken || curr`)
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            Sts1HostAssets.install();
            List<MapNodeView> nodes = Arrays.asList(
                    new MapNodeView(1, 2, 100f, 200f, false, true, true, false,
                            64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER),
                    new MapNodeView(4, 5, 30f, 40f, false, false, false, true,
                            48f, 52f, "P", "shop", ResourceIds.MAP_NODE_SHOP),
                    new MapNodeView(3, 1, 250f, 325f, true, false, false, false,
                            40f, 40f, "R", "rest", ResourceIds.MAP_NODE_REST));
            publishMapFrame("map", nodes);

            List<MapDrawPath.Submission> plan = MapDrawPath.mapSubmissionPlan();

            // Native order: legend paints FIRST (under), nodes paint OVER it. The legend block is the
            // first 8 entries, then the 7 node entries (icon/outline/overlay per node + the
            // taken-node MAP_CIRCLE_5 ring).
            assertEquals("legend block + node block", 8 + 7, plan.size());

            // Legend panel (index 0, no label) + title (index 1) + 6 icon rows (2..7).
            assertEquals("legend panel resource", ResourceIds.UI_MAP_LEGEND, plan.get(0).resourceId);
            assertEquals("legend title", "Legend", plan.get(1).label);
            assertEquals("Event", plan.get(2).label);
            assertEquals("Elite", plan.get(7).label);
            assertEquals(ResourceIds.MAP_NODE_EVENT, plan.get(2).resourceId);
            assertEquals(ResourceIds.MAP_NODE_ELITE, plan.get(7).resourceId);

            // Node submissions follow the legend, in sync z order: outline, node icon, overlay.
            // Native MapRoomNode.render draws the OUTLINE before the FILL (both at the same node
            // box), so the fill covers the outline center (D03 wash fix).
            String[] expectedNodeOrder = {
                ResourceIds.mapOutline("monster"),
                ResourceIds.MAP_NODE_MONSTER, ResourceIds.UI_MAP_HIGHLIGHT,
                ResourceIds.MAP_NODE_SHOP, ResourceIds.UI_MAP_PIN,
                ResourceIds.MAP_NODE_REST, ResourceIds.UI_MAP_CIRCLE_5
            };
            for (int i = 0; i < expectedNodeOrder.length; i++) {
                assertEquals("node submission " + i, expectedNodeOrder[i],
                        plan.get(8 + i).resourceId);
            }

            // Outline precedes fill for the reachable node, and both use the SAME projected rect —
            // the precondition that lets the fill cover the outline's center (native order).
            MapDrawPath.DrawItem first = MapDrawPath.buildFromProjection().get(0);
            MapDrawPath.Submission outline001 = plan.get(8);
            MapDrawPath.Submission fill001 = plan.get(9);
            assertEquals("outline role", "outline", outline001.role);
            assertEquals("fill role", "node", fill001.role);
            assertEquals("outline then fill", ResourceIds.mapOutline("monster"),
                    outline001.resourceId);
            assertEquals("fill resource", ResourceIds.MAP_NODE_MONSTER, fill001.resourceId);
            assertEquals("outline bounds x", first.bounds.x, outline001.bounds.x, 0.01f);
            assertEquals("outline bounds y", first.bounds.y, outline001.bounds.y, 0.01f);
            assertEquals("outline bounds w", first.bounds.width, outline001.bounds.width, 0.01f);
            assertEquals("outline bounds h", first.bounds.height, outline001.bounds.height, 0.01f);
            assertEquals("fill bounds x", first.bounds.x, fill001.bounds.x, 0.01f);
            assertEquals("outline/fill rects must be equal (fill covers outline center)",
                    outline001.bounds.x, fill001.bounds.x, 0.01f);
            assertEquals(outline001.bounds.y, fill001.bounds.y, 0.01f);
            assertEquals(outline001.bounds.width, fill001.bounds.width, 0.01f);
            assertEquals(outline001.bounds.height, fill001.bounds.height, 0.01f);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void submissionPlanPaintsReachableNodeOutlineBeforeFillAtSameRect() {
        // D03 "map nodes render pale" root cause: native MapRoomNode.render draws the OUTLINE
        // (getMapImgOutline) BEFORE the node FILL (getMapImg) at the SAME node box, so the fill
        // covers the outline center and only the outline edges show. The plan must match that order
        // for a reachable node (else the ~0.53 grey outline paints over the fill and washes it).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            Sts1HostAssets.install();
            // reachable=true → outline submitted; not highlighted/pinned/taken/current → no overlay/ring.
            List<MapNodeView> nodes = Collections.singletonList(
                    new MapNodeView(2, 3, 100f, 200f, false, false, true, false,
                            64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER));
            publishMapFrame("map", nodes);

            List<MapDrawPath.Submission> plan = MapDrawPath.mapSubmissionPlan();
            // Only the legend block precedes this node, so the node's two entries are the last two.
            MapDrawPath.Submission outline = plan.get(plan.size() - 2);
            MapDrawPath.Submission fill = plan.get(plan.size() - 1);

            assertEquals("outline resource", ResourceIds.mapOutline("monster"), outline.resourceId);
            assertEquals("outline role", "outline", outline.role);
            assertEquals("fill resource", ResourceIds.MAP_NODE_MONSTER, fill.resourceId);
            assertEquals("fill role", "node", fill.role);

            // Same rect: this is what lets the fill cover the outline's center (native behavior).
            assertEquals("outline/fill x equal", outline.bounds.x, fill.bounds.x, 0.001f);
            assertEquals("outline/fill y equal", outline.bounds.y, fill.bounds.y, 0.001f);
            assertEquals("outline/fill w equal", outline.bounds.width, fill.bounds.width, 0.001f);
            assertEquals("outline/fill h equal", outline.bounds.height, fill.bounds.height, 0.001f);

            // Native tints unchanged: outline = OUTLINE_COLOR grey (0.549,0.549,0.502), fill = node
            // tint (NOT_TAKEN 0.34 grey when unavailable). The reorder must not alter colors.
            assertEquals("outline r", MapDrawPath.OUTLINE_COLOR[0], outline.r, 0.001f);
            assertEquals("outline g", MapDrawPath.OUTLINE_COLOR[1], outline.g, 0.001f);
            assertEquals("outline b", MapDrawPath.OUTLINE_COLOR[2], outline.b, 0.001f);
            assertEquals("fill uses the node tint (not outline grey)",
                    MapDrawPath.buildFromProjection().get(0).nodeR, fill.r, 0.001f);
            assertEquals(MapDrawPath.buildFromProjection().get(0).nodeG, fill.g, 0.001f);
            assertEquals(MapDrawPath.buildFromProjection().get(0).nodeB, fill.b, 0.001f);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void mapNodeAndLegendIdsResolveAsLogicalIds() {
        // The submission plan keeps the LOGICAL resource id (not artSource) because
        // drawResolvedTexture resolves via ArtFramework.assets().resolve(resourceId). Pin that the
        // vanilla catalog actually resolves these logical ids so the map is not register-only.
        Sts1HostAssets.install();
        for (String id : new String[] {
            ResourceIds.MAP_NODE_MONSTER, ResourceIds.MAP_NODE_REST,
            ResourceIds.mapOutline("monster"), ResourceIds.UI_MAP_HIGHLIGHT,
            ResourceIds.UI_MAP_PIN, ResourceIds.UI_MAP_LEGEND, ResourceIds.UI_MAP_CIRCLE_5
        }) {
            artframework.assets.AssetResolveResult r = ArtFramework.assets().resolve(id);
            assertTrue("logical id must resolve for map submission: " + id, r.found);
            assertTrue("resolved source must be file-backed for: " + id,
                    artframework.sts1.assets.Sts1AssetMaterializer.isFileBacked(r.source));
        }
    }

    @Test
    public void submissionPlanOmitsEmptyResourcesAndEmptyBounds() {
        // Fail-open shape: a node with no mapped resource and invalid bounds contributes nothing,
        // and a valid later node is still submitted (never aborted by the earlier gap).
        Sts1HostAssets.install();
        List<MapNodeView> nodes = Arrays.asList(
                new MapNodeView(1, 1, 10f, 10f, false, false, false, false,
                        0f, 0f, "", "unknown", ""),
                new MapNodeView(2, 2, 20f, 20f, false, false, false, false,
                        50f, 50f, "S", "shop", ResourceIds.MAP_NODE_SHOP));
        publishMapFrame("map", nodes);
        List<MapDrawPath.Submission> plan = MapDrawPath.mapSubmissionPlan();
        assertTrue("only the valid, resource-backed node is submitted",
                containsResource(plan, ResourceIds.MAP_NODE_SHOP));
        for (MapDrawPath.Submission s : plan) {
            assertFalse("no empty resource id in the plan", s.resourceId.isEmpty());
        }
    }

    private static boolean containsResource(List<MapDrawPath.Submission> plan, String resourceId) {
        for (MapDrawPath.Submission s : plan) {
            if (resourceId.equals(s.resourceId)) return true;
        }
        return false;
    }

    @Test
    public void legendZStaysBelowNodeBandInPlanAndSync() {
        // FINDING 1: native draws Legend.render BEFORE the nodes (nodes paint over the legend), so
        // the legend z must sit below the node band (node icon=1, outline=2, overlay=3).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            MapDrawPath.LegendDrawItem panel = legendById(MapDrawPath.legendItems(), "legend.panel");
            MapDrawPath.LegendDrawItem title = legendById(MapDrawPath.legendItems(), "legend.title");
            MapDrawPath.LegendDrawItem icon = legendById(MapDrawPath.legendItems(), "legend:event");
            assertNotNull(panel);
            assertNotNull(title);
            assertNotNull(icon);
            assertEquals("legend.panel z", 0.1f, panel.z, 0.0001f);
            assertEquals("legend icon z", 0.15f, icon.z, 0.0001f);
            assertEquals("legend.title z", 0.2f, title.z, 0.0001f);
            assertTrue("legend panel below nodes", panel.z < 1f);
            assertTrue("legend title below nodes", title.z < 1f);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
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

    @Test
    public void backgroundItemsMatchNativeRectsAndOrder() {
        // D03 map background follow-up: derive the five native parchment draw rects from the
        // VERIFIED native formulas (non-mobile DungeonMap.renderNormalMap/renderMapCenters/
        // renderMapBlender), computed here independently from the controlled MapBackground inputs.
        float scale = 1.25f;
        float offsetY = 123f;
        float mapMidDist = 777f;
        MapView.MapBackground bg = new MapView.MapBackground(
                mapMidDist,
                mapMidDist - 120f * scale,
                offsetY,
                scale,
                1920,
                1080,
                1020f * scale,
                512f * scale,
                0.4f);
        publishMapFrameWithBackground("map", projectedNodes(), bg);

        List<MapDrawPath.BackgroundDrawItem> items = MapDrawPath.backgroundItems();
        assertEquals("top, mid, bot, blend A, blend B", 5, items.size());

        float w = 1920f;
        float h1080 = 1080f * scale;
        float base = offsetY + (mapMidDist - 120f * scale);
        // order + rects, derived from the same inputs (not copied constants)
        assertBackground(items.get(0), "map.bg.top", ResourceIds.MAP_BG_TOP,
                0f, 1020f * scale + base, w, h1080, 0.4f);
        assertBackground(items.get(1), "map.bg.mid", ResourceIds.MAP_BG_MID,
                0f, base, w, h1080, 0.4f);
        assertBackground(items.get(2), "map.bg.bot", ResourceIds.MAP_BG_BOT,
                0f, -mapMidDist + base + 1f, w, h1080, 0.4f);
        assertBackground(items.get(3), "map.bg.blend.a", ResourceIds.MAP_BG_BLEND,
                0f, base + 800f * scale, w, 512f * scale, 0.4f);
        assertBackground(items.get(4), "map.bg.blend.b", ResourceIds.MAP_BG_BLEND,
                0f, base - 220f * scale, w, 512f * scale, 0.4f);
    }

    @Test
    public void backgroundItemsCarryLiveAlphaAndSplitOnTheBlendTexture() {
        // Distinct alpha proves the draw path carries the LIVE baseMapColor.a, not a constant; the
        // blend strip is drawn TWICE (native renderMapBlender) sharing one texture id.
        MapView.MapBackground bg = new MapView.MapBackground(
                1000f, 1000f - 120f, 0f, 1f, 1280, 720, 1020f, 512f, 0.31f);
        publishMapFrameWithBackground("map", projectedNodes(), bg);
        List<MapDrawPath.BackgroundDrawItem> items = MapDrawPath.backgroundItems();
        assertEquals(5, items.size());
        for (MapDrawPath.BackgroundDrawItem item : items) {
            assertEquals(0.31f, item.alpha, 0.0001f);
        }
        int blendCount = 0;
        for (MapDrawPath.BackgroundDrawItem item : items) {
            if (ResourceIds.MAP_BG_BLEND.equals(item.resourceId)) blendCount++;
        }
        assertEquals("blend strips A and B share the blend texture", 2, blendCount);
    }

    @Test
    public void backgroundItemsFailOpenWhenProjectionHasNoBackground() {
        // No native background fields readable -> the MapView carries none -> ZERO background
        // drawables (map supply unchanged, renderer never invents pixels).
        publishMapFrame("map", projectedNodes());
        assertTrue(MapDrawPath.backgroundItems().isEmpty());
        Map<String, Object> probe = MapDrawPath.probeSlice();
        assertEquals(Integer.valueOf(0), probe.get("backgroundCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> background = (List<Map<String, Object>>) probe.get("background");
        assertNotNull(background);
        assertTrue(background.isEmpty());
    }

    @Test
    public void backgroundProbeExposesRectAndAlphaAndIdsResolveToRealFiles() {
        MapView.MapBackground bg = new MapView.MapBackground(
                500f, 500f - 120f, 10f, 1f, 1920, 1080, 1020f, 512f, 0.75f);
        publishMapFrameWithBackground("map", projectedNodes(), bg);
        Map<String, Object> probe = MapDrawPath.probeSlice();
        assertEquals(Integer.valueOf(5), probe.get("backgroundCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> background = (List<Map<String, Object>>) probe.get("background");
        assertEquals(5, background.size());
        Map<String, Object> top = background.get(0);
        assertEquals("map.bg.top", top.get("id"));
        assertEquals(ResourceIds.MAP_BG_TOP, top.get("resourceId"));
        assertEquals(0.75f, ((Float) top.get("alpha")).floatValue(), 0.0001f);
        assertEquals(1920f, ((Float) top.get("w")).floatValue(), 0.0001f);
        assertEquals(1020f + 10f + 380f, ((Float) top.get("y")).floatValue(), 0.01f);

        // Every background id must map to a real file-backed source (not register-only).
        Sts1HostAssets.install();
        for (String id : new String[] {
            ResourceIds.MAP_BG_TOP, ResourceIds.MAP_BG_MID,
            ResourceIds.MAP_BG_BOT, ResourceIds.MAP_BG_BLEND
        }) {
            artframework.assets.AssetResolveResult r = ArtFramework.assets().resolve(id);
            assertTrue("background id must resolve: " + id, r.found);
            assertTrue("background id must be file-backed: " + id,
                    artframework.sts1.assets.Sts1AssetMaterializer.isFileBacked(r.source));
        }
        Map<String, String> catalog = Sts1VanillaCatalog.catalog();
        assertEquals("sts1:images/ui/map/mapTop.png", catalog.get(ResourceIds.MAP_BG_TOP));
        assertEquals("sts1:images/ui/map/mapMid.png", catalog.get(ResourceIds.MAP_BG_MID));
        assertEquals("sts1:images/ui/map/mapBot.png", catalog.get(ResourceIds.MAP_BG_BOT));
        assertEquals("sts1:images/ui/map/mapBlend.png", catalog.get(ResourceIds.MAP_BG_BLEND));
    }

    @Test
    public void backgroundItemsFinalActEmitsOnlyTopAndBot() {
        // Native renderFinalActMap (AbstractDungeon.id=="TheEnding", non-mobile) draws ONLY top then
        // bot — no mid and no blend strips (renderMapBlender is a no-op for the final act). Derive the
        // expected rects from the same controlled inputs and compare against the normal 5-item output.
        float scale = 1.25f;
        float offsetY = 123f;
        float mapMidDist = 777f;
        float mapOffsetY = mapMidDist - 120f * scale;
        MapView.MapBackground finalAct = new MapView.MapBackground(
                mapMidDist, mapOffsetY, offsetY, scale, 1920, 1080,
                1020f * scale, 512f * scale, 0.4f, true);
        assertTrue("finalAct flag must be carried", finalAct.finalAct);
        assertEquals(Boolean.TRUE, finalAct.toMap().get("finalAct"));
        publishMapFrameWithBackground("map", projectedNodes(), finalAct);

        List<MapDrawPath.BackgroundDrawItem> items = MapDrawPath.backgroundItems();
        assertEquals("final act: top then bot only", 2, items.size());
        float w = 1920f;
        float h1080 = 1080f * scale;
        float base = offsetY + mapOffsetY;
        assertBackground(items.get(0), "map.bg.top", ResourceIds.MAP_BG_TOP,
                0f, 1020f * scale + base, w, h1080, 0.4f);
        assertBackground(items.get(1), "map.bg.bot", ResourceIds.MAP_BG_BOT,
                0f, -mapMidDist + base + 1f, w, h1080, 0.4f);
        for (MapDrawPath.BackgroundDrawItem item : items) {
            assertFalse("final act must omit mid", ResourceIds.MAP_BG_MID.equals(item.resourceId));
            assertFalse("final act must omit blend", ResourceIds.MAP_BG_BLEND.equals(item.resourceId));
        }

        // The NORMAL-act projection over the SAME inputs emits the full 5 (top/mid/bot/blendA/blendB)
        // and the shared top/bot rects are identical to the final-act output.
        MapView.MapBackground normal = new MapView.MapBackground(
                mapMidDist, mapOffsetY, offsetY, scale, 1920, 1080,
                1020f * scale, 512f * scale, 0.4f, false);
        publishMapFrameWithBackground("map", projectedNodes(), normal);
        List<MapDrawPath.BackgroundDrawItem> normalItems = MapDrawPath.backgroundItems();
        assertEquals(5, normalItems.size());
        assertEquals(items.get(0).bounds.y, normalItems.get(0).bounds.y, 0.01f);
        assertEquals(items.get(1).bounds.y, normalItems.get(2).bounds.y, 0.01f);
        // Probe reflects the final-act shape too.
        publishMapFrameWithBackground("map", projectedNodes(), finalAct);
        Map<String, Object> probe = MapDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("backgroundCount"));
    }

    @Test
    public void edgeItemsResolveNativeDotDrawsFromStoredValuesAndEdgeColor() {
        // D03 map edges: derive the expected dot rect/formula/color independently from the controlled
        // edge inputs, NOT from a copied constant. Native MapDot.render:
        //   x = dot.x - 8 ; y = dot.y - 8 + offsetY + 172*scale ; size = 16*scale ; rotation = dot.rot
        // and MapEdge.render tints each dot with the public edge color.
        float scale = 1.25f;
        float offsetY = 111f;
        MapView.MapBackground bg = new MapView.MapBackground(
                800f, 800f - 120f * scale, offsetY, scale, 1920, 1080,
                1020f * scale, 512f * scale, 1f);
        List<MapView.MapDotView> dots = Arrays.asList(
                new MapView.MapDotView(100f, 200f, 12.5f),
                new MapView.MapDotView(150f, 250f, -7f));
        List<MapView.MapEdgeView> edges = Collections.singletonList(
                new MapView.MapEdgeView(0.09f, 0.13f, 0.17f, 1f, dots));
        publishMapFrameWithBackgroundAndEdges("map", projectedNodes(), bg, edges);

        List<MapDrawPath.EdgeDrawItem> items = MapDrawPath.edgeItems();
        assertEquals("2 dots on one edge", 2, items.size());

        for (int i = 0; i < dots.size(); i++) {
            MapView.MapDotView dot = dots.get(i);
            MapDrawPath.EdgeDrawItem item = items.get(i);
            assertEquals(ResourceIds.MAP_EDGE_DOT, item.resourceId);
            assertEquals(dot.x - 8f * scale, item.bounds.x, 0.01f);
            assertEquals(dot.y - 8f * scale + offsetY + 172f * scale, item.bounds.y, 0.01f);
            assertEquals(16f * scale, item.bounds.width, 0.01f);
            assertEquals(16f * scale, item.bounds.height, 0.01f);
            assertEquals("stored dot rotation must be carried verbatim",
                    dot.rotation, item.rotationDegrees, 0.0001f);
            assertEquals(0.09f, item.r, 0.001f);
            assertEquals(0.13f, item.g, 0.001f);
            assertEquals(0.17f, item.b, 0.001f);
            assertEquals(1f, item.a, 0.001f);
        }

        // Probe exposes the same draws under backend.mapDraw.edges.
        Map<String, Object> probe = MapDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("edgeCount"));
        assertEquals(Boolean.TRUE, probe.get("edgesPresent"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> probeEdges = (List<Map<String, Object>>) probe.get("edges");
        assertEquals(2, probeEdges.size());
        assertEquals(ResourceIds.MAP_EDGE_DOT, probeEdges.get(0).get("resourceId"));
        assertEquals(Float.valueOf(12.5f), probeEdges.get(0).get("rotation"));
        @SuppressWarnings("unchecked")
        Map<String, Object> color = (Map<String, Object>) probeEdges.get(0).get("color");
        assertEquals(0.09f, ((Float) color.get("r")).floatValue(), 0.001f);
    }

    @Test
    public void edgeItemsUseLiveOffsetYAndScale() {
        // Distinct offsetY/scale prove the draw path carries the LIVE background numbers (no
        // constant): recompute the rect from a second, different background and compare.
        float scale = 0.8f;
        float offsetY = -42f;
        MapView.MapBackground bg = new MapView.MapBackground(
                900f, 900f - 120f * scale, offsetY, scale, 1920, 1080,
                1020f * scale, 512f * scale, 1f);
        List<MapView.MapEdgeView> edges = Collections.singletonList(
                new MapView.MapEdgeView(0f, 0f, 0f, 0.25f,
                        Collections.singletonList(new MapView.MapDotView(10f, 20f, 3f))));
        publishMapFrameWithBackgroundAndEdges("map", projectedNodes(), bg, edges);
        MapDrawPath.EdgeDrawItem item = MapDrawPath.edgeItems().get(0);
        assertEquals(10f - 8f * scale, item.bounds.x, 0.01f);
        assertEquals(20f - 8f * scale + offsetY + 172f * scale, item.bounds.y, 0.01f);
        assertEquals(16f * scale, item.bounds.width, 0.01f);
        assertEquals(0.25f, item.a, 0.0001f);
    }

    @Test
    public void edgeItemsFailOpenWhenNoBackgroundOrNoEdges() {
        // No native background -> no offset/scale -> ZERO edge draws (the renderer never invents
        // pixels), even when the projection carries edges. And no edges -> ZERO.
        List<MapView.MapEdgeView> edges = Collections.singletonList(
                new MapView.MapEdgeView(0f, 0f, 0f, 0.25f,
                        Collections.singletonList(new MapView.MapDotView(5f, 6f, 0f))));
        publishMapFrameWithBackgroundAndEdges("map", projectedNodes(), null, edges);
        assertTrue("no background -> no edge draws", MapDrawPath.edgeItems().isEmpty());
        assertEquals(Integer.valueOf(0), MapDrawPath.probeSlice().get("edgeCount"));

        MapView.MapBackground bg = new MapView.MapBackground(
                800f, 680f, 0f, 1f, 1920, 1080, 1020f, 512f, 1f);
        publishMapFrameWithBackgroundAndEdges("map", projectedNodes(), bg, null);
        assertTrue("no edges -> no edge draws", MapDrawPath.edgeItems().isEmpty());
    }

    @Test
    public void paintOrderPutsEdgesBelowNodesAndAboveBackground() {
        // D03 map edges: native draws edges at the START of each node's render, so edges sit UNDER
        // the node band and OVER the parchment background. Only RELATIVE band indices are asserted
        // (deriving an exact list from the same helpers paintOrder() uses would be tautological).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            MapView.MapBackground bg = new MapView.MapBackground(
                    777f, 777f - 120f, 0f, 1f, 1920, 1080, 1020f, 512f, 1f);
            List<MapView.MapEdgeView> edges = Collections.singletonList(
                    new MapView.MapEdgeView(0.09f, 0.13f, 0.17f, 1f, Arrays.asList(
                            new MapView.MapDotView(1f, 2f, 0f),
                            new MapView.MapDotView(3f, 4f, 5f))));
            publishMapFrameWithBackgroundAndEdges("map", projectedNodes(), bg, edges);

            List<String> order = MapDrawPath.paintOrder();
            int lastBg = -1;
            int firstEdge = -1;
            int lastEdge = -1;
            int firstLegend = -1;
            int firstNode = -1;
            for (int i = 0; i < order.size(); i++) {
                String key = order.get(i);
                if (key.startsWith("bg:")) lastBg = i;
                if (key.startsWith("edge:")) {
                    if (firstEdge < 0) firstEdge = i;
                    lastEdge = i;
                }
                if (key.startsWith("legend:") && firstLegend < 0) firstLegend = i;
                if (key.startsWith("node:") && firstNode < 0) firstNode = i;
            }
            assertTrue("edge band must be present", firstEdge >= 0);
            assertTrue("background below edges", lastBg >= 0 && lastBg < firstEdge);
            assertTrue("edges above background", firstEdge > lastBg);
            assertTrue("edges below legend", lastEdge < firstLegend);
            assertTrue("edges below nodes", lastEdge < firstNode);

            // Independent band-membership check (does NOT rebuild the list from the same helpers
            // paintOrder() uses): the paint order must start with the background band and end with
            // the node band, with the edge band strictly inside.
            assertTrue("paint order must start with the background band",
                    order.get(0).startsWith("bg:"));
            assertFalse("paint order must not start with an edge",
                    order.get(0).startsWith("edge:"));
            assertTrue("paint order must end with the node band",
                    order.get(order.size() - 1).startsWith("node:")
                            || order.get(order.size() - 1).startsWith("outline:")
                            || order.get(order.size() - 1).startsWith("overlay:")
                            || order.get(order.size() - 1).startsWith("ring:"));
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void edgeDotResourceIsMappedToRealFileAndDotCountInProbe() {
        // The single edge-dot id must resolve to a real file-backed source (not register-only), and
        // its catalog source is the verified native ImageMaster.MAP_DOT_1 path.
        Sts1HostAssets.install();
        Map<String, String> catalog = Sts1VanillaCatalog.catalog();
        assertEquals("sts1:images/ui/map/dot1.png", catalog.get(ResourceIds.MAP_EDGE_DOT));
        artframework.assets.AssetResolveResult r = ArtFramework.assets().resolve(
                ResourceIds.MAP_EDGE_DOT);
        assertTrue("edge dot id must resolve", r.found);
        assertTrue("edge dot id must be file-backed",
                artframework.sts1.assets.Sts1AssetMaterializer.isFileBacked(r.source));
    }

    @Test
    public void paintOrderPutsBackgroundBelowLegendAndNodes() {
        // D03 layering fix: the opaque parchment background must be the BOTTOM map layer so it cannot
        // wash out the node/edge pixels. With a live background, a legend (Settings available) and two
        // projected nodes, the model-level paint order must be strictly bg -> legend -> node band.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            MapView.MapBackground bg = new MapView.MapBackground(
                    777f, 777f - 120f, 0f, 1f, 1920, 1080, 1020f, 512f, 1f);
            publishMapFrameWithBackground("map", projectedNodes(), bg);

            List<String> order = MapDrawPath.paintOrder();
            assertFalse("paint order must be non-empty", order.isEmpty());

            int firstLegend = -1;
            int firstNode = -1;
            int lastBg = -1;
            for (int i = 0; i < order.size(); i++) {
                String key = order.get(i);
                if (key.startsWith("bg:")) lastBg = i;
                if (key.startsWith("legend:") && firstLegend < 0) firstLegend = i;
                if (key.startsWith("node:") && firstNode < 0) firstNode = i;
            }
            assertTrue("background items must be present", lastBg >= 0);
            assertTrue("legend items must be present", firstLegend >= 0);
            assertTrue("node items must be present", firstNode >= 0);
            assertTrue("background must paint below the legend", lastBg < firstLegend);
            assertTrue("background must paint below the nodes", lastBg < firstNode);
            assertTrue("legend must paint below the nodes", firstLegend < firstNode);

            // Per-node order: the reachable node (projectedNodes() node 0 is reachable+highlighted)
            // must list its `outline:r:c` BEFORE its `node:r:c` (native MapRoomNode.render order).
            String reachableKey = null;
            for (MapDrawPath.DrawItem item : MapDrawPath.buildFromProjection()) {
                if (item.reachable || item.highlighted) {
                    reachableKey = item.row + ":" + item.col;
                    break;
                }
            }
            assertNotNull("fixture must carry a reachable node", reachableKey);
            int outlineIdx = order.indexOf("outline:" + reachableKey);
            int nodeIdx = order.indexOf("node:" + reachableKey);
            assertTrue("reachable node must emit an outline", outlineIdx >= 0);
            assertTrue("node band entry must be present", nodeIdx >= 0);
            assertTrue("outline must precede node for node " + reachableKey,
                    outlineIdx < nodeIdx);

            // The order must be EXACTLY backgroundItems() ids, then legendItems() ids, then the node
            // band — derived from the same sources, not a copied constant list. The per-node roles
            // mirror mapSubmissionPlan(): outline (when reachable/highlighted) BEFORE node.
            List<String> expected = new ArrayList<String>();
            for (MapDrawPath.BackgroundDrawItem item : MapDrawPath.backgroundItems()) {
                expected.add("bg:" + item.id);
            }
            for (MapDrawPath.LegendDrawItem item : MapDrawPath.legendItems()) {
                expected.add("legend:" + item.id);
            }
            for (MapDrawPath.DrawItem item : MapDrawPath.buildFromProjection()) {
                String key = item.row + ":" + item.col;
                if (item.reachable || item.highlighted) expected.add("outline:" + key);
                expected.add("node:" + key);
                if (item.pinned || item.highlighted) expected.add("overlay:" + key);
                if (item.taken || item.currentNode) expected.add("ring:" + key);
            }
            assertEquals(expected, order);

            // Probe exposes the same order (device-visible evidence).
            @SuppressWarnings("unchecked")
            List<String> probed = (List<String>) MapDrawPath.probeSlice().get("paintOrder");
            assertEquals(order, probed);
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    @Test
    public void renderMapSubmissionPlanExcludesBackgroundSoItIsNotDoubleDrawn() {
        // The background is painted separately (bottom layer) by renderMapBackground; renderMap's own
        // submission plan (legend + node band) must contain NO background resource, or the opaque
        // parchment would be re-drawn on top of the nodes.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        float previousYScale = com.megacrit.cardcrawl.core.Settings.yScale;
        try {
            setSettingsScale(1f, 1f, 1f);
            MapView.MapBackground bg = new MapView.MapBackground(
                    777f, 777f - 120f, 0f, 1f, 1920, 1080, 1020f, 512f, 1f);
            publishMapFrameWithBackground("map", projectedNodes(), bg);
            assertFalse("fixture must carry a background", MapDrawPath.backgroundItems().isEmpty());

            Set<String> backgroundResources = new LinkedHashSet<String>();
            for (MapDrawPath.BackgroundDrawItem item : MapDrawPath.backgroundItems()) {
                backgroundResources.add(item.resourceId);
            }
            for (MapDrawPath.Submission submission : MapDrawPath.mapSubmissionPlan()) {
                assertFalse("renderMap must not submit the background resource "
                                + submission.resourceId + " over the nodes",
                        backgroundResources.contains(submission.resourceId));
            }
        } finally {
            setSettingsScale(previousScale, previousXScale, previousYScale);
        }
    }

    private static void assertBackground(
            MapDrawPath.BackgroundDrawItem item, String id, String resourceId,
            float x, float y, float w, float h, float alpha) {
        assertEquals(id, item.id);
        assertEquals(resourceId, item.resourceId);
        assertEquals(x, item.bounds.x, 0.01f);
        assertEquals(y, item.bounds.y, 0.01f);
        assertEquals(w, item.bounds.width, 0.01f);
        assertEquals(h, item.bounds.height, 0.01f);
        assertEquals(alpha, item.alpha, 0.0001f);
    }

    private static List<MapNodeView> projectedNodes() {
        return Arrays.asList(
                new MapNodeView(1, 2, 100f, 200f, false, true,
                        "M", "monster", ResourceIds.MAP_NODE_MONSTER),
                new MapNodeView(3, 1, 250f, 325f, true, false,
                        "R", "rest", ResourceIds.MAP_NODE_REST));
    }

    private static void publishMapFrame(String scene, List<MapNodeView> nodes) {
        publishMapFrame(scene, nodes, null);
    }

    private static void publishMapFrameWithBackground(
            String scene, List<MapNodeView> nodes, MapView.MapBackground background) {
        publishMapFrameWithBackgroundAndEdges(scene, nodes, background, null);
    }

    private static void publishMapFrameWithBackgroundAndEdges(
            String scene, List<MapNodeView> nodes, MapView.MapBackground background,
            List<MapView.MapEdgeView> edges) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        scene,
                        Collections.<artframework.context.CardView>emptyList(),
                        ControlsView.empty(),
                        new MapView(nodes, 1920, 1080, null, background, edges),
                        new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private static void publishMapFrame(
            String scene, List<MapNodeView> nodes, MapView.LegendLabels legend) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        scene,
                        Collections.<artframework.context.CardView>emptyList(),
                        ControlsView.empty(),
                        new MapView(nodes, 1920, 1080, legend),
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
