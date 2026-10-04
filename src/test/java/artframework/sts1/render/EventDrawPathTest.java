package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SelectView;
import artframework.context.SurfaceIds;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EventDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishEventFrame() {
        publishEventFrame(
                "Neow",
                EventOptionView.of(0, "Talk", true),
                EventOptionView.of(1, "Leave", true));
    }

    private void publishEventFrame(String title, EventOptionView... options) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        EventView event = EventView.of(title, Arrays.asList(options));
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "event",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        event,
                        SelectView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildFromProjectionListsOptions() {
        publishEventFrame();
        assertEquals(4, EventDrawPath.buildFromProjection().size());
        Map<String, Object> probe = EventDrawPath.probeSlice();
        assertEquals(Integer.valueOf(4), probe.get("count"));
        assertEquals("Neow", probe.get("title"));
        assertEquals(Boolean.FALSE, probe.get("suppressNativeEvent"));
    }

    @Test
    public void fullMountedEventDrawsAndSuppressesNative() {
        publishEventFrame();
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.EVENT).action("mount_event");
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.EVENT));
        assertTrue(EventDrawPath.shouldSuppressNativeEvent());
        assertTrue(plan.shouldSuppressNative(SurfaceIds.EVENT));
        assertEquals(Boolean.TRUE, EventDrawPath.probeSlice().get("suppressNativeEvent"));
    }

    @Test
    public void offDoesNotSuppress() {
        publishEventFrame();
        ArtFramework.component(SurfaceIds.EVENT).action("mount_event");
        assertFalse(EventDrawPath.shouldSuppressNativeEvent());
    }

    @Test
    public void projectionCarriesTitleOptionsDisabledAndGeometry() {
        publishEventFrame(
                "Golden Idol",
                EventOptionView.of(0, "Take", true, 111f, 222f, 333f, 44f),
                EventOptionView.of(1, "Leave", false));

        List<EventDrawPath.DrawItem> items = EventDrawPath.buildFromProjection();
        Map<String, Object> probe = EventDrawPath.probeSlice();

        assertEquals("Golden Idol", probe.get("title"));
        assertEquals(Boolean.TRUE, probe.get("available"));
        assertEquals(Integer.valueOf(items.size()), probe.get("count"));
        assertEquals(4, items.size());
        assertEquals("panel", items.get(0).id);
        assertEquals(ResourceIds.UI_EVENT_PANEL, items.get(0).resourceId);
        assertEquals("title", items.get(1).id);
        assertEquals(ResourceIds.UI_EVENT_TITLE, items.get(1).resourceId);
        assertEquals(0, items.get(2).index);
        assertEquals("Take", items.get(2).label);
        assertTrue(items.get(2).enabled);
        assertEquals(ResourceIds.UI_EVENT_BUTTON_ENABLED, items.get(2).resourceId);
        assertEquals(111f, items.get(2).x, 0.001f);
        assertEquals(222f, items.get(2).y, 0.001f);
        assertEquals(333f, items.get(2).w, 0.001f);
        assertEquals(44f, items.get(2).h, 0.001f);
        assertEquals(1, items.get(3).index);
        assertEquals("Leave", items.get(3).label);
        assertEquals(ResourceIds.UI_EVENT_BUTTON_DISABLED, items.get(3).resourceId);
        assertFalse("disabled event option state must be exposed to C2", items.get(3).enabled);
    }

    @Test
    public void prepareEventVisualsUsesOptionIdsResourcesAndRetainsCurrentProjection() {
        publishEventFrame(
                "Golden Idol",
                EventOptionView.of(0, "Take", true, 100f, 200f, 300f, 40f),
                EventOptionView.of(1, "Leave", false, 100f, 150f, 300f, 40f));
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.EVENT).mount();

        invokePrepareEventVisuals(Sts1RenderPipeline.plan());

        Map<String, DrawComponent> first = c2Draws(SurfaceIds.EVENT);
        assertEquals(4, first.size());
        assertEquals(ResourceIds.UI_EVENT_PANEL, first.get("panel").resourceId);
        assertEquals("Golden Idol", first.get("title").text);
        assertEquals(ResourceIds.UI_EVENT_TITLE, first.get("title").resourceId);
        assertEquals("Take", first.get("option:0").text);
        assertEquals(ResourceIds.UI_EVENT_BUTTON_ENABLED, first.get("option:0").resourceId);
        assertEquals(300f, c2Bounds(SurfaceIds.EVENT).get("option:0").rect.width, 0.001f);
        assertEquals(40f, c2Bounds(SurfaceIds.EVENT).get("option:0").rect.height, 0.001f);
        assertEquals("Leave", first.get("option:1").text);
        assertEquals(ResourceIds.UI_EVENT_BUTTON_DISABLED, first.get("option:1").resourceId);

        publishEventFrame(
                "Golden Idol",
                EventOptionView.of(1, "Leave", false, 100f, 150f, 300f, 40f));
        invokePrepareEventVisuals(Sts1RenderPipeline.plan());

        Map<String, DrawComponent> retained = c2Draws(SurfaceIds.EVENT);
        assertEquals("stale option:0 item must be removed when projection shrinks", 3, retained.size());
        assertFalse(retained.containsKey("option:0"));
        assertTrue(retained.containsKey("panel"));
        assertTrue(retained.containsKey("title"));
        assertEquals("Leave", retained.get("option:1").text);
    }

    @Test
    public void eventEvidenceDrawCountComesFromProjectionItems() {
        publishEventFrame(
                "Neow",
                EventOptionView.of(0, "Talk", true),
                EventOptionView.of(1, "Leave", true),
                EventOptionView.of(2, "Locked", false));
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.EVENT).mount();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.EVENT, "com.megacrit.cardcrawl.events.GenericEventDialog", "render", "event");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        invokeRenderEvent();

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(5, evidence.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void eventEvidenceDrawCountIgnoresInvisibleProjectedItems() {
        publishEventFrame(
                "Neow",
                EventOptionView.of(0, "Talk", true),
                new EventOptionView(1, "Hidden", true, false, 100f, 100f, 300f, 40f));
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.EVENT).mount();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.EVENT, "com.megacrit.cardcrawl.events.GenericEventDialog", "render", "event");

        invokeRenderEvent();

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(EventDrawPath.materializedDrawCount(), evidence.drawCount);
        assertEquals(3, evidence.drawCount);
    }

    @Test
    public void eventRenderFailureStillRecordsEvidenceWithoutStrictPollution() {
        publishEventFrame(
                "Neow",
                EventOptionView.of(0, "Talk", true),
                EventOptionView.of(1, "Leave", true));
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.EVENT).mount();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.EVENT, "com.megacrit.cardcrawl.events.GenericEventDialog", "render", "event");

        // A null SpriteBatch makes the native draw calls fail; the renderer must still close
        // evidence for the delegated invocation and keep strict diagnostics clean.
        invokeRenderEvent();

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(EventDrawPath.materializedDrawCount(), evidence.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void eventPanelUsesNativeGeometryAtUnitScale() throws Exception {
        // Native GenericEventDialog.render (verified bytecode) draws the panel via
        // sb.draw(eventBackgroundImg, WIDTH/2-881.5-12*xScale, EVENT_Y-403-64*scale,
        //         881.5, 403, 1763, 806, xScale, scale, ...). DrawItem x/y is the CENTER, so the
        // center is (WIDTH/2 - 881.5 - 12*xScale + 881.5*xScale, EVENT_Y - 403 - 64*scale + 403*scale).
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousEventY = com.megacrit.cardcrawl.core.Settings.EVENT_Y;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1f));
            // Settings.EVENT_Y = HEIGHT/2 - 128*scale = 412 at 1080/1.
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "EVENT_Y", Float.valueOf(412f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();
            publishEventFrame();

            EventDrawPath.DrawItem panel = EventDrawPath.panelItem();
            assertNotNull("panel is supplied while the event is available", panel);
            assertEquals(ResourceIds.UI_EVENT_PANEL, panel.resourceId);
            assertEquals("native center x = WIDTH/2 - 12*xScale", 948f, panel.x, 0.01f);
            assertEquals("native center y = EVENT_Y - 64*scale at unit scale", 348f, panel.y, 0.01f);
            assertEquals(1763f, panel.w, 0.01f);
            assertEquals(806f, panel.h, 0.01f);
            // renderer applies x - w/2, y - h/2 -> native bottom-left (948 - 881.5, 348 - 403).
            assertEquals(66.5f, panel.x - panel.w / 2f, 0.01f);
            assertEquals(-55f, panel.y - panel.h / 2f, 0.01f);

            // title center = (TITLE_X, TITLE_Y) = (570*xScale, EVENT_Y + 408*scale)
            EventDrawPath.DrawItem title = EventDrawPath.buildFromProjection().get(1);
            assertEquals("title", title.id);
            assertTrue("title must be text-only (no texture draw)", title.textOnly);
            assertEquals(570f, title.x, 0.01f);
            assertEquals(820f, title.y, 0.01f);

            Map<String, Object> probe = EventDrawPath.probeSlice();
            @SuppressWarnings("unchecked")
            Map<String, Object> panelMap = (Map<String, Object>) probe.get("panel");
            assertNotNull("probe exposes the native panel sub-map", panelMap);
            assertEquals(ResourceIds.UI_EVENT_PANEL, panelMap.get("resourceId"));
            assertEquals(Float.valueOf(948f), panelMap.get("x"));
            assertEquals(Float.valueOf(348f), panelMap.get("y"));
            assertEquals(Float.valueOf(1763f), panelMap.get("w"));
            assertEquals(Float.valueOf(806f), panelMap.get("h"));
            assertEquals(Integer.valueOf(1), probe.get("panelCount"));
            assertEquals("submitCount equals the visible panel-bearing item count",
                    Integer.valueOf(EventDrawPath.materializedDrawCount()), probe.get("submitCount"));
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(previousXScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(previousWidth));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "EVENT_Y", Float.valueOf(previousEventY));
        }
    }

    @Test
    public void eventPanelNativeGeometryCatchesXScaleVersusScaleMixups() throws Exception {
        // Non-unit, xScale != scale: the SIZE must carry 1763*xScale / 806*scale and the center
        // delta terms 881.5*xScale / 403*scale, while EVENT_Y uses 128*scale.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousEventY = com.megacrit.cardcrawl.core.Settings.EVENT_Y;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1.25f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1.5f));
            // Settings.EVENT_Y = HEIGHT/2 - 128*scale = 540 - 160 = 380.
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "EVENT_Y", Float.valueOf(380f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            CombatInputRouter.resetForTests();
            publishEventFrame();

            EventDrawPath.DrawItem panel = EventDrawPath.panelItem();
            assertNotNull(panel);
            assertEquals(1763f * 1.5f, panel.w, 0.01f);
            assertEquals(806f * 1.25f, panel.h, 0.01f);
            assertEquals(960f - 12f * 1.5f, panel.x, 0.01f);
            assertEquals(380f - 64f * 1.25f, panel.y, 0.01f);
            // renderer's bottom-left = center - size/2; the native origin term bakes into the
            // center, so left = WIDTH/2 - 12*xScale - 881.5*xScale (carries xScale).
            assertEquals(960f - 12f * 1.5f - 881.5f * 1.5f, panel.x - panel.w / 2f, 0.01f);

            EventDrawPath.DrawItem title = EventDrawPath.buildFromProjection().get(1);
            assertEquals(570f * 1.5f, title.x, 0.01f);
            assertEquals(380f + 408f * 1.25f, title.y, 0.01f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(previousXScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(previousWidth));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "EVENT_Y", Float.valueOf(previousEventY));
        }
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

    private static void invokePrepareEventVisuals(SurfaceDrawPlan plan) {
        invokePrivate("prepareEventVisuals", new Class<?>[] {SurfaceDrawPlan.class}, plan);
    }

    private static void invokeRenderEvent() {
        invokePrivate("renderEvent",
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

    private static Map<String, DrawComponent> c2Draws(String surfaceId) {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + surfaceId;
        Set<String> duplicateGuard = new LinkedHashSet<String>();
        Map<String, DrawComponent> out = new LinkedHashMap<String, DrawComponent>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity == null || !scope.equals(identity.key.scope)) continue;
            assertTrue("duplicate C2 item id " + identity.key.localId,
                    duplicateGuard.add(identity.key.localId));
            out.put(identity.key.localId, context.world().get(entity, DrawComponent.class));
        }
        return out;
    }

    private static Map<String, BoundsComponent> c2Bounds(String surfaceId) {
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        String scope = "sts1.visual." + surfaceId;
        Map<String, BoundsComponent> out = new LinkedHashMap<String, BoundsComponent>();
        for (EntityId entity : context.entities()) {
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            if (identity != null && scope.equals(identity.key.scope)) {
                out.put(identity.key.localId, context.world().get(entity, BoundsComponent.class));
            }
        }
        return out;
    }
}
