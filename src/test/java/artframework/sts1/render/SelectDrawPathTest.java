package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
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
        assertTrue(items.get(0).enabled);
        assertEquals(ResourceIds.UI_SELECT_CARD, items.get(0).resourceId);
        assertEquals(ResourceIds.UI_SELECT_CARD_FRAME, items.get(0).frameResourceId);
        assertEquals(250f, items.get(0).w, 0.001f);
        assertEquals(350f, items.get(0).h, 0.001f);
        assertEquals(111f, items.get(0).x, 0.001f);
        assertEquals(222f, items.get(0).y, 0.001f);
        assertEquals("g2", items.get(1).instanceId);
        assertTrue("selectedInstanceIds must mark the draw item selected", items.get(1).selected);
        assertEquals(ResourceIds.UI_SELECT_CARD_SELECTED, items.get(1).resourceId);
        assertTrue("confirm row must be a projected item, not an evidence constant", items.get(2).confirm);
        assertTrue(items.get(2).enabled);
        assertEquals(ResourceIds.UI_SELECT_CONFIRM, items.get(2).resourceId);
        // NRO-04 D09 native geometry at fail-open unit scale (JUnit Settings.scale=0 -> default 1).
        assertEquals(960f, items.get(2).x, 0.001f);
        assertEquals(475f, items.get(2).y, 0.001f);
        assertEquals(512f, items.get(2).w, 0.001f);
        assertEquals(256f, items.get(2).h, 0.001f);
    }

    @Test
    public void localizedTitleBecomesLabelWhileCardIdStaysRaw() {
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .title("打击")
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        true));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);

        assertEquals("raw id must stay for identity/resource selection", "Strike_R", item.cardId);
        assertEquals("label must carry the localized title", "打击", item.label);
        assertFalse("label must differ from the raw id here", item.cardId.equals(item.label));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) SelectDrawPath.probeSlice().get("items");
        assertEquals("Strike_R", rows.get(0).get("cardId"));
        assertEquals("打击", rows.get(0).get("label"));
    }

    @Test
    public void emptyTitleFallsBackToRawCardIdAsLabel() {
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .title("")
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        true));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);

        assertEquals("Strike_R", item.label);
        assertEquals("Strike_R", item.cardId);
    }

    @Test
    public void confirmItemKeepsNeutralLabelFromRawId() {
        publishSelectFrame();
        SelectDrawPath.DrawItem confirm = SelectDrawPath.confirmItem();
        assertNotNull(confirm);
        assertTrue(confirm.confirm);
        assertEquals("Confirm", confirm.label);
    }

    @Test
    public void localizedConfirmLabelOverridesFallbackInItemAndProbe() {
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        true,
                        "确认"));

        SelectDrawPath.DrawItem confirm = SelectDrawPath.confirmItem();
        assertNotNull(confirm);
        assertTrue("the localized label must land on the confirm DrawItem", confirm.confirm);
        assertEquals("确认", confirm.label);
        // The raw-id field stays the neutral literal, not the localized text.
        assertEquals("Confirm", confirm.cardId);
        assertFalse("localized label must differ from the neutral fallback literal",
                "Confirm".equals(confirm.label));

        @SuppressWarnings("unchecked")
        Map<String, Object> c = (Map<String, Object>) SelectDrawPath.probeSlice().get("confirm");
        assertNotNull(c);
        assertEquals("确认", c.get("label"));
    }

    @Test
    public void blankOrAbsentConfirmLabelFallsBackToNeutralLiteral() {
        // confirmLabel explicitly blank.
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        true,
                        "   "));

        SelectDrawPath.DrawItem blank = SelectDrawPath.confirmItem();
        assertNotNull(blank);
        assertEquals("Confirm", blank.label);

        // No confirm label supplied at all (the 4-arg factory path).
        publishSelectFrame();
        SelectDrawPath.DrawItem absent = SelectDrawPath.confirmItem();
        assertNotNull(absent);
        assertEquals("Confirm", absent.label);

        @SuppressWarnings("unchecked")
        Map<String, Object> c = (Map<String, Object>) SelectDrawPath.probeSlice().get("confirm");
        assertNotNull(c);
        assertEquals("Confirm", c.get("label"));
    }

    @Test
    public void selectViewDefaultsConfirmLabelToEmptyWhenNotThreaded() {
        assertEquals("", SelectView.grid(null, null, false, false).confirmLabel);
        assertEquals("", SelectView.hand(null, null, false, false).confirmLabel);
        assertEquals("", SelectView.empty().confirmLabel);
        assertEquals(
                "确认",
                SelectView.hand(null, null, false, false, "确认").confirmLabel);
        assertEquals(
                "confirmLabel is exposed on the projection map",
                "确认",
                SelectView.grid(null, null, false, false, "确认").toMap().get("confirmLabel"));
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
        assertFalse(items.get(1).enabled);
        assertEquals(ResourceIds.UI_SELECT_CONFIRM_DISABLED, items.get(1).resourceId);
    }

    @Test
    public void disabledSelectCardUsesDisabledFallbackResource() {
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .playable(false)
                                        .build()),
                        Collections.<String>emptyList(),
                        false,
                        false));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);

        assertFalse(item.enabled);
        assertEquals(ResourceIds.UI_SELECT_CARD_DISABLED, item.resourceId);
        assertEquals(ResourceIds.UI_SELECT_CARD_FRAME, item.frameResourceId);
    }

    @Test
    public void confirmButtonUsesNativeCardSelectGeometryAndProbeSubMap() throws Exception {
        // Native CardSelectConfirmButton.renderButton draws a 512x256 texture with centre
        // (WIDTH/2, TAKE_Y = 475*scale) and size (512*scale, 256*scale). DrawItem x/y is CENTER.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            publishSelectFrame();

            SelectDrawPath.DrawItem confirm = SelectDrawPath.confirmItem();
            assertNotNull(confirm);
            assertTrue(confirm.confirm);
            assertEquals(ResourceIds.UI_SELECT_CONFIRM, confirm.resourceId);
            assertEquals("centre x = WIDTH/2", 960f, confirm.x, 0.001f);
            assertEquals("centre y = 475*scale", 475f, confirm.y, 0.001f);
            assertEquals("size w = 512*scale", 512f, confirm.w, 0.001f);
            assertEquals("size h = 256*scale", 256f, confirm.h, 0.001f);
            // renderer emits x - w/2, y - h/2.
            assertEquals("native top-left x = WIDTH/2 - 256", 704f, confirm.x - confirm.w / 2f, 0.001f);
            assertEquals("native top-left y = 475 - 128", 347f, confirm.y - confirm.h / 2f, 0.001f);

            Map<String, Object> probe = SelectDrawPath.probeSlice();
            @SuppressWarnings("unchecked")
            Map<String, Object> c = (Map<String, Object>) probe.get("confirm");
            assertNotNull("probe exposes the confirm sub-map", c);
            assertEquals(ResourceIds.UI_SELECT_CONFIRM, c.get("resourceId"));
            assertEquals(Float.valueOf(960f), c.get("x"));
            assertEquals(Float.valueOf(475f), c.get("y"));
            assertEquals(Float.valueOf(512f), c.get("w"));
            assertEquals(Float.valueOf(256f), c.get("h"));
            assertEquals(Boolean.TRUE, c.get("enabled"));
            assertEquals(Boolean.TRUE, c.get("visible"));
            // Existing keys stay intact.
            assertEquals(Boolean.TRUE, probe.get("confirmEnabled"));
            assertEquals(Boolean.TRUE, probe.get("confirmVisible"));
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    @Test
    public void confirmButtonGeometryCatchesAxisMixupsAtNonUnitScale() throws Exception {
        // Both native axes use Settings.scale; a WIDTH-only centre and a scale-only size mean
        // WIDTH affects x but not w, and xScale must NOT leak into either.
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        float previousXScale = com.megacrit.cardcrawl.core.Settings.xScale;
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1600));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1.25f));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale", Float.valueOf(1.5f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();
            publishSelectFrame();

            SelectDrawPath.DrawItem confirm = SelectDrawPath.confirmItem();
            assertNotNull(confirm);
            assertEquals("centre x follows WIDTH only", 800f, confirm.x, 0.01f);
            assertEquals("centre y = 475*scale", 593.75f, confirm.y, 0.01f);
            assertEquals("w uses scale, not xScale", 640f, confirm.w, 0.01f);
            assertEquals("h uses scale, not xScale", 320f, confirm.h, 0.01f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "xScale",
                    Float.valueOf(previousXScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    @Test
    public void confirmResourceMapsToExistingNativeTexture() throws Exception {
        Map<String, String> catalog = artframework.sts1.assets.Sts1VanillaCatalog.catalog();
        assertEquals("sts1:images/ui/reward/takeAll.png",
                catalog.get(ResourceIds.UI_SELECT_CONFIRM));
        assertEquals("sts1:images/ui/reward/takeAllUsed.png",
                catalog.get(ResourceIds.UI_SELECT_CONFIRM_DISABLED));
        assertTrue(artframework.sts1.assets.Sts1VanillaCatalog.isKnown(ResourceIds.UI_SELECT_CONFIRM));
        assertTrue(artframework.sts1.assets.Sts1VanillaCatalog
                .isKnown(ResourceIds.UI_SELECT_CONFIRM_DISABLED));

        // The mapped enabled/disabled textures must EXIST in the host jar at 512x256.
        java.net.URL enabled = SelectDrawPathTest.class.getClassLoader()
                .getResource("images/ui/reward/takeAll.png");
        assertNotNull("takeAll.png must exist in the host jar", enabled);
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(enabled);
        assertNotNull(img);
        assertEquals(512, img.getWidth());
        assertEquals(256, img.getHeight());
        java.net.URL disabled = SelectDrawPathTest.class.getClassLoader()
                .getResource("images/ui/reward/takeAllUsed.png");
        assertNotNull("takeAllUsed.png must exist in the host jar", disabled);
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
        assertEquals(ResourceIds.UI_SELECT_CARD, first.get("card:g1").resourceId);
        assertEquals(250f, c2Bounds(SurfaceIds.SELECT_GRID).get("card:g1").rect.width, 0.001f);
        assertEquals(350f, c2Bounds(SurfaceIds.SELECT_GRID).get("card:g1").rect.height, 0.001f);
        assertEquals("Strike_R", first.get("card:g2").text);
        assertEquals(ResourceIds.UI_SELECT_CARD_SELECTED, first.get("card:g2").resourceId);
        assertEquals("Confirm", first.get("confirm").text);
        assertEquals(ResourceIds.UI_SELECT_CONFIRM, first.get("confirm").resourceId);

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
        assertEquals(3, evidence.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void selectEvidenceDrawCountIgnoresInvisibleProjectedItems() {
        publishSelectFrame(
                SelectView.grid(
                        Arrays.asList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .pose(new CardPose(100f, 200f, 0f, 1f, 0f, true))
                                        .build(),
                                CardView.builder(new CardRef("g2", "Defend_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(1)
                                        .pose(new CardPose(220f, 200f, 0f, 1f, 0f, false))
                                        .build()),
                        Collections.singletonList("g1"),
                        true,
                        false));
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.SELECT_GRID).mount();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.SELECT_GRID,
                "com.megacrit.cardcrawl.screens.select.GridCardSelectScreen",
                "render",
                "grid");

        invokeRenderSelect(SurfaceIds.SELECT_GRID);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(SelectDrawPath.materializedDrawCount(), evidence.drawCount);
        assertEquals(1, evidence.drawCount);
    }

    @Test
    public void selectRenderFailureStillRecordsEvidenceWithoutStrictPollution() {
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

        // A null SpriteBatch makes the per-item native draw calls fail; evidence must still close
        // for the delegated invocation and strict diagnostics must remain clean.
        invokeRenderSelect(SurfaceIds.SELECT_GRID);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger().evidence(disposition.invocationId);
        assertNotNull(evidence);
        assertEquals(SelectDrawPath.materializedDrawCount(), evidence.drawCount);
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

    private static void invokePrivate(String name, Class<?>[] types, Object... args) {        try {
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

    private static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            java.lang.reflect.Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set static field " + owner + "." + name, failure);
        }
    }
}
