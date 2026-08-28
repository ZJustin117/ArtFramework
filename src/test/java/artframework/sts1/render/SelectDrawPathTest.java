package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardPose;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SelectDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void publishSelectFrame() {
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .selected(true)
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        true));
    }

    private void publishSelectFrame(SelectView select) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "select",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        EventView.empty(),
                        select,
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    @Test
    public void buildIncludesPoolAndConfirm() {
        publishSelectFrame();
        assertTrue(SelectDrawPath.buildFromProjection().size() >= 2);
        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(SelectView.KIND_GRID, probe.get("kind"));
        assertEquals(Integer.valueOf(1), probe.get("poolCount"));
        assertEquals(Boolean.TRUE, probe.get("confirmEnabled"));
    }

    @Test
    public void fullMountedGridDrawsAndSuppressesNative() {
        publishSelectFrame();
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SELECT_GRID).action("mount_select");
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldDraw(SurfaceIds.SELECT_GRID));
        assertTrue(SelectDrawPath.shouldSuppressNativeGrid());
        assertTrue(SelectDrawPath.shouldSuppressNativeSelect());
        assertTrue(plan.shouldSuppressNative(SurfaceIds.SELECT_GRID));
    }

    @Test
    public void projectionCarriesGridCardsConfirmSelectionAndCounts() {
        publishSelectFrame(
                SelectView.grid(
                        Arrays.asList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .pose(new CardPose(111f, 222f, 0f, 1f, 0f, true))
                                        .selected(false)
                                        .build(),
                                CardView.builder(new CardRef("g2", "Defend_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(1)
                                        .pose(new CardPose(333f, 444f, 0f, 1f, 0f, true))
                                        .selected(false)
                                        .build()),
                        Collections.singletonList("g2"),
                        true,
                        true));

        List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
        Map<String, Object> probe = SelectDrawPath.probeSlice();

        assertEquals(3, items.size());
        assertEquals(Integer.valueOf(items.size()), probe.get("count"));
        assertEquals(SelectView.KIND_GRID, probe.get("kind"));
        assertEquals(Boolean.TRUE, probe.get("available"));
        assertEquals(Integer.valueOf(2), probe.get("poolCount"));
        assertEquals(Integer.valueOf(1), probe.get("selectedCount"));
        assertEquals(Boolean.TRUE, probe.get("confirmEnabled"));
        assertEquals(Boolean.TRUE, probe.get("confirmVisible"));
        assertEquals("g1", items.get(0).instanceId);
        assertEquals("Strike_R", items.get(0).cardId);
        assertFalse(items.get(0).selected);
        assertEquals(111f, items.get(0).x, 0.001f);
        assertEquals(222f, items.get(0).y, 0.001f);
        assertEquals("g2", items.get(1).instanceId);
        assertTrue("selectedInstanceIds must mark the draw item selected", items.get(1).selected);
        assertTrue("confirm row must be a projected item, not an evidence constant", items.get(2).confirm);
    }

    @Test
    public void projectionCarriesHandCardsAndDisabledConfirmState() {
        publishSelectFrame(
                SelectView.hand(
                        Collections.singletonList(
                                CardView.builder(new CardRef("h1", "Bash"))
                                        .zone(CardZone.HAND)
                                        .slot(0)
                                        .selected(true)
                                        .build()),
                        Collections.<String>emptyList(),
                        false,
                        true));

        List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
        Map<String, Object> probe = SelectDrawPath.probeSlice();

        assertEquals(2, items.size());
        assertEquals(SelectView.KIND_HAND, probe.get("kind"));
        assertEquals(Boolean.FALSE, probe.get("confirmEnabled"));
        assertEquals(Boolean.TRUE, probe.get("confirmVisible"));
        assertEquals("h1", items.get(0).instanceId);
        assertEquals("Bash", items.get(0).cardId);
        assertTrue(items.get(0).selected);
        assertTrue(items.get(1).confirm);
    }

    @Test
    public void prepareSelectVisualsUsesInstanceIdsAndRetainsCurrentProjection() {
        publishSelectFrame(
                SelectView.grid(
                        Arrays.asList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .pose(CardPose.at(100f, 200f))
                                        .build(),
                                CardView.builder(new CardRef("g2", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(1)
                                        .pose(CardPose.at(220f, 200f))
                                        .build()),
                        Collections.singletonList("g2"),
                        true,
                        true));
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SELECT_GRID).mount();

        invokePrepareSelectVisuals(Sts1RenderPipeline.plan());

        Map<String, DrawComponent> first = c2Draws(SurfaceIds.SELECT_GRID);
        assertEquals(3, first.size());
        assertEquals("Strike_R", first.get("card:g1").text);
        assertEquals("Strike_R", first.get("card:g2").text);
        assertEquals("Confirm", first.get("confirm").text);

        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g2", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .pose(CardPose.at(220f, 200f))
                                        .build()),
                        Collections.singletonList("g2"),
                        false,
                        false));
        invokePrepareSelectVisuals(Sts1RenderPipeline.plan());

        Map<String, DrawComponent> retained = c2Draws(SurfaceIds.SELECT_GRID);
        assertEquals("stale card and confirm items must be removed when projection shrinks",
                1, retained.size());
        assertFalse(retained.containsKey("card:g1"));
        assertFalse(retained.containsKey("confirm"));
        assertEquals("Strike_R", retained.get("card:g2").text);
    }

    @Test
    public void handSelectVisualsMaterializeOnHandSurface() {
        publishSelectFrame(
                SelectView.hand(
                        Collections.singletonList(
                                CardView.builder(new CardRef("h1", "Bash"))
                                        .zone(CardZone.HAND)
                                        .slot(0)
                                        .pose(CardPose.at(300f, 250f))
                                        .build()),
                        Collections.<String>emptyList(),
                        true,
                        true));
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SELECT_HAND).mount();

        invokePrepareSelectVisuals(Sts1RenderPipeline.plan());

        Map<String, DrawComponent> handItems = c2Draws(SurfaceIds.SELECT_HAND);
        assertEquals(2, handItems.size());
        assertEquals("Bash", handItems.get("card:h1").text);
        assertEquals("Confirm", handItems.get("confirm").text);
        assertTrue(c2Draws(SurfaceIds.SELECT_GRID).isEmpty());
    }

    @Test
    public void selectEvidenceDrawCountComesFromProjectionItems() {
        publishSelectFrame(
                SelectView.grid(
                        Arrays.asList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .build(),
                                CardView.builder(new CardRef("g2", "Defend_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(1)
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        true));
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SELECT_GRID).mount();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.SELECT_GRID,
                "com.megacrit.cardcrawl.screens.select.GridCardSelectScreen",
                "render",
                "grid");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        invokeRenderSelect(SurfaceIds.SELECT_GRID);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(SelectDrawPath.buildFromProjection().size(), evidence.drawCount);
        assertEquals(3, evidence.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    private static void invokePrepareSelectVisuals(SurfaceDrawPlan plan) {
        invokePrivate("prepareSelectVisuals", new Class<?>[] {SurfaceDrawPlan.class}, plan);
    }

    private static void invokeRenderSelect(String surfaceId) {
        invokePrivate("renderSelect",
                new Class<?>[] {com.badlogic.gdx.graphics.g2d.SpriteBatch.class, String.class},
                null,
                surfaceId);
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
