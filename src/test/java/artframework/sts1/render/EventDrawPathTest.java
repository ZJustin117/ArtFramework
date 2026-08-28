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
        assertEquals(2, EventDrawPath.buildFromProjection().size());
        Map<String, Object> probe = EventDrawPath.probeSlice();
        assertEquals(Integer.valueOf(2), probe.get("count"));
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
        assertEquals(2, items.size());
        assertEquals(0, items.get(0).index);
        assertEquals("Take", items.get(0).label);
        assertTrue(items.get(0).enabled);
        assertEquals(111f, items.get(0).x, 0.001f);
        assertEquals(222f, items.get(0).y, 0.001f);
        assertEquals(333f, items.get(0).w, 0.001f);
        assertEquals(44f, items.get(0).h, 0.001f);
        assertEquals(1, items.get(1).index);
        assertEquals("Leave", items.get(1).label);
        assertFalse("disabled event option state must be exposed to C2", items.get(1).enabled);
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
        assertEquals(2, first.size());
        assertEquals("Take", first.get("option:0").text);
        assertEquals(ResourceIds.UI_EVENT_BUTTON_ENABLED, first.get("option:0").resourceId);
        assertEquals("Leave", first.get("option:1").text);
        assertEquals(ResourceIds.UI_EVENT_BUTTON_DISABLED, first.get("option:1").resourceId);

        publishEventFrame(
                "Golden Idol",
                EventOptionView.of(1, "Leave", false, 100f, 150f, 300f, 40f));
        invokePrepareEventVisuals(Sts1RenderPipeline.plan());

        Map<String, DrawComponent> retained = c2Draws(SurfaceIds.EVENT);
        assertEquals("stale option:0 item must be removed when projection shrinks", 1, retained.size());
        assertFalse(retained.containsKey("option:0"));
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
        assertEquals(EventDrawPath.buildFromProjection().size(), evidence.drawCount);
        assertEquals(3, evidence.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
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
}
