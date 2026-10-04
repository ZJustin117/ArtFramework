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
        assertEquals("map evidence count must come from current projected nodes/overlays",
                5, evidence.drawCount);
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
    public void submissionPlanSubmitsNodeIconOutlineHighlightAndLegend() {
        // NRO-04 D03 defect fix: renderMap must SUBMIT mapped pixels, not merely count them.
        // Three nodes exercise node-icon + outline + highlight variants:
        //  - reachable+highlighted monster: icon + outline + highlight
        //  - pinned shop:                    icon + highlight only (no reachable/highlighted)
        //  - taken rest (neither):           icon only
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
            // first 8 entries, then the 6 node entries.
            assertEquals("legend block + node block", 8 + 6, plan.size());

            // Legend panel (index 0, no label) + title (index 1) + 6 icon rows (2..7).
            assertEquals("legend panel resource", ResourceIds.UI_MAP_LEGEND, plan.get(0).resourceId);
            assertEquals("legend title", "Legend", plan.get(1).label);
            assertEquals("Event", plan.get(2).label);
            assertEquals("Elite", plan.get(7).label);
            assertEquals(ResourceIds.MAP_NODE_EVENT, plan.get(2).resourceId);
            assertEquals(ResourceIds.MAP_NODE_ELITE, plan.get(7).resourceId);

            // Node submissions follow the legend, in sync z order: icon, outline, overlay.
            String[] expectedNodeOrder = {
                ResourceIds.MAP_NODE_MONSTER, ResourceIds.mapOutline("monster"),
                ResourceIds.UI_MAP_HIGHLIGHT,
                ResourceIds.MAP_NODE_SHOP, ResourceIds.UI_MAP_PIN,
                ResourceIds.MAP_NODE_REST
            };
            for (int i = 0; i < expectedNodeOrder.length; i++) {
                assertEquals("node submission " + i, expectedNodeOrder[i],
                        plan.get(8 + i).resourceId);
            }
            assertEquals("node icon+outline+overlay bounds are the projected node bounds",
                    MapDrawPath.buildFromProjection().get(0).bounds.x,
                    plan.get(8).bounds.x, 0.01f);
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
            ResourceIds.UI_MAP_PIN, ResourceIds.UI_MAP_LEGEND
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
