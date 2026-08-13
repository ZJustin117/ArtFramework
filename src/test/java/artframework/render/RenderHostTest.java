package artframework.render;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.EffectDecl;
import artframework.component.LayoutSpec;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.EffectAttachment;
import artframework.presentation.EffectsComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.PresentationKey;
import artframework.presentation.VisibilityComponent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RenderHostTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void builtinsRegistered() {
        RenderHost host = ArtFramework.render();
        assertTrue(host.effects().contains(TintEffect.ID));
        assertTrue(host.effects().contains(GlowEffect.ID));
        assertTrue(host.effects().contains(LightwaveEffect.ID));
        assertTrue(host.shaders().contains(GlowEffect.SHADER_ID));
        assertTrue(host.shaders().contains(LightwaveEffect.SHADER_ID));
        ShaderRegistry.ShaderDef glow = host.shaders().get(GlowEffect.SHADER_ID);
        assertEquals("shaders/glow.vert", glow.vertexClasspath);
        assertEquals("shaders/glow.frag", glow.fragmentClasspath);
    }

    @Test
    public void bindEffectAndProbe() {
        RenderHost host = ArtFramework.render();
        host.ensureTarget("t1", RenderTargetKind.OVERLAY);
        host.bindEffect("t1", TintEffect.ID, Collections.<String, Object>singletonMap("alpha", 0.2f));
        assertEquals(1, host.bindingCount());
        Map<String, Object> probe = host.probeMap();
        assertEquals(Integer.valueOf(1), probe.get("targetCount"));
        assertEquals(Integer.valueOf(1), probe.get("bindingCount"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fullFrameDisabledByDefault() {
        RenderHost host = ArtFramework.render();
        host.ensureTarget("ff", RenderTargetKind.FULL_FRAME);
        host.bindEffect("ff", TintEffect.ID, null);
    }

    @Test
    public void syncWidgetSessionFromCompositionSample() {
        ArtFramework.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        ArtFramework.open("comp");
        RenderHost host = ArtFramework.render();
        assertTrue(host.targetCount() >= 1);
        assertNotNull(host.getTarget("c1:comp"));
        assertNotNull(host.getTarget("c1:comp:intensity"));
        List<EffectBinding> fx = host.effectsOf("c1:comp:intensity");
        assertEquals(1, fx.size());
        assertEquals(GlowEffect.ID, fx.get(0).effectId);

        Map<String, Object> snap = ArtFramework.probe().asMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> render = (Map<String, Object>) snap.get("render");
        assertNotNull(render);
        assertTrue(((Number) render.get("targetCount")).intValue() >= 1);

        ArtFramework.close("comp");
        assertNullTarget("c1:comp");
    }

    @Test
    public void entitySlotSyncAndDetach() {
        ArtFramework.entities().attach("e1", "card", "Strike");
        ArtFramework.entities().layout("e1", 100f, 200f, 1f);
        RenderTarget t = ArtFramework.render().getTarget("c2:entity:e1");
        assertNotNull(t);
        assertEquals(RenderTargetKind.ENTITY_SLOT, t.kind);
        assertTrue(t.isEnabled());
        ArtFramework.entities().detach("e1");
        assertNullTarget("c2:entity:e1");
    }

    @Test
    public void entityPresentSyncRebuildsTargetCacheFromEcsSlots() {
        artframework.c2.DefaultEntityPresent present = new artframework.c2.DefaultEntityPresent();
        present.attach("hero", "player", "ironclad");
        present.layout("hero", 12f, 34f, 1f);
        RenderHost host = new RenderHost();
        host.syncEntityPresent();
        assertNotNull(host.getTarget("c2:entity:hero"));
        present.detach("hero");
        host.syncEntityPresent();
        assertNull(host.getTarget("c2:entity:hero"));
    }

    @Test
    public void c2VisualSyncRebuildsItemTargetsFromEcsVisualEntities() {
        artframework.presentation.PresentationVisuals.syncC2Item(
                "combat.hand", "card-1", new Rect(8f, 9f, 60f, 90f), 3f,
                "card", "card.art", "Strike", true);
        RenderHost host = new RenderHost();
        host.syncC2Visuals();
        String targetId = RenderHost.c2ItemTargetId("combat.hand", "card-1");
        RenderTarget target = host.getTarget(targetId);
        assertNotNull(target);
        assertEquals(new Rect(8f, 9f, 60f, 90f), target.bounds());
        artframework.presentation.PresentationVisuals.removeC2Item("combat.hand", "card-1");
        host.syncC2Visuals();
        assertNull(host.getTarget(targetId));
    }

    @Test
    public void manualBindOnEntityTarget() {
        ArtFramework.entities().attach("e2", "monster", "Hexaghost");
        ArtFramework.entities().layout("e2", 10f, 20f, 1.5f);
        ArtFramework.render().bindEffect(
                "c2:entity:e2",
                GlowEffect.ID,
                Collections.<String, Object>singletonMap("intensity", 0.8f));
        assertEquals(1, ArtFramework.render().effectsOf("c2:entity:e2").size());
    }

    @Test
    public void drawFrameNoOpWithoutGl() {
        RenderHost host = ArtFramework.render();
        host.ensureTarget("ov", RenderTargetKind.OVERLAY).setBounds(0, 0, 10, 10);
        host.bindEffect("ov", TintEffect.ID, null);
        host.tick(0.016f);
        host.drawFrame(null);
        assertTrue(host.timeSeconds() > 0f);
    }

    @Test
    public void retainC2ItemsPreservesExistingTargets() {
        RenderHost host = ArtFramework.render();
        RenderTarget first = host.syncC2Item("hand", "a", 1f, 2f, 3f, 4f);
        host.syncC2Item("hand", "b", 5f, 6f, 7f, 8f);
        host.retainC2Items("hand", new HashSet<String>(Collections.singletonList("a")));
        assertSame(first, host.getTarget(RenderHost.c2ItemTargetId("hand", "a")));
        assertNullTarget(host, RenderHost.c2ItemTargetId("hand", "b"));
    }

    @Test
    public void openTreeSyncsEffectsFromNode() {
        UiNode root = UiNode.of(UiTypes.WINDOW)
                .id("w")
                .prop("title", "T")
                .layout(new LayoutSpec(200f, 100f, 0f, 0f, false))
                .effect(new EffectDecl(TintEffect.ID, null))
                .child(UiNode.of(UiTypes.BUTTON).id("b").prop("text", "X").build())
                .build();
        ArtFramework.register(new artframework.api.WindowDef("w",
                artframework.api.WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.render().syncWidgetSession(new artframework.component.WidgetSession("w", root));
        assertEquals(1, ArtFramework.render().effectsOf("c1:w").size());
        ArtFramework.render().detachWidgetSession("w");
    }

    @Test
    public void syncFrameDerivesUiCacheFromPresentationEntities() {
        PresentationContext context = new PresentationContext("frame-test");
        try {
            EntityId id = context.create(
                    new PresentationKey("ui", "panel"), "panel", "panel", "c1");
            context.world().put(id, BoundsComponent.class,
                    new BoundsComponent(new Rect(4f, 5f, 60f, 20f), 3f));
            context.world().put(id, DrawComponent.class, new DrawComponent("panel", "", "P"));
            context.world().put(id, VisibilityComponent.class, new VisibilityComponent(true, 1f));
            EffectsComponent effects = context.world().get(id, EffectsComponent.class);
            context.world().put(id, EffectsComponent.class, effects.withAttachment(
                    new EffectAttachment(TintEffect.ID, "ambient",
                            Collections.<String, Object>singletonMap("alpha", 0.25f))));

            RenderHost host = new RenderHost();
            artframework.presentation.PresentationFrame frame = PresentationRuntime.frame(context);
            host.syncFrame(frame, RenderTargetKind.SYNTHETIC_WIDGET);
            RenderTarget target = host.getTarget("ui:panel");
            assertNotNull(target);
            assertEquals(new Rect(4f, 5f, 60f, 20f), target.bounds());
            assertEquals(3f, target.z(), 0.001f);
            assertEquals(1, host.effectsOf("ui:panel").size());
            assertSame(frame, host.lastPresentationFrame());
        } finally {
            context.close();
        }
    }

    @Test
    public void bindingCountIsSafeDuringConcurrentTargetUpdates() throws Exception {
        final RenderHost host = new RenderHost();
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final CountDownLatch start = new CountDownLatch(1);
        Thread writer =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    start.await();
                                    for (int i = 0; i < 500; i++) {
                                        String id = "concurrent-" + (i % 8);
                                        host.ensureTarget(id, RenderTargetKind.OVERLAY);
                                        host.bindEffect(id, TintEffect.ID, null);
                                        host.clearEffects(id);
                                    }
                                } catch (Throwable t) {
                                    failure.compareAndSet(null, t);
                                }
                            }
                        });
        Thread reader =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    start.await();
                                    for (int i = 0; i < 500; i++) {
                                        host.bindingCount();
                                        host.needsCapture();
                                        host.probeMap();
                                    }
                                } catch (Throwable t) {
                                    failure.compareAndSet(null, t);
                                }
                            }
                        });
        writer.start();
        reader.start();
        start.countDown();
        writer.join();
        reader.join();
        assertTrue(failure.get() == null);
    }

    private static void assertNullTarget(String id) {
        assertFalse(ArtFramework.render().listTargetIds().contains(id));
    }

    private static void assertNullTarget(RenderHost host, String id) {
        assertFalse(host.listTargetIds().contains(id));
    }
}
