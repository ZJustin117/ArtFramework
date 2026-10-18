package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.context.CardPose;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SelectView;
import artframework.context.SurfaceIds;
import artframework.core.UiComponent;
import artframework.sts1.FullPresentMode;
import artframework.sts1.input.CombatInputRouter;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * U05b: the HAND-select row must land ON SCREEN. {@code Sts1PresentationBackend.readHandSelect}
 * projects no pose at all, so {@code SelectDrawPath.buildFromProjection} is expected to fall back to
 * the {@code defaultCardX(i, poolCount)} / {@code defaultCardY()} layout. That fallback is only
 * reachable when a pose-less {@link CardView} really carries {@code pose == null}; the synthetic
 * {@code CardPose.at(0,0)} default pins the whole row to the screen origin (D1-observed: every hand
 * select item reported x=0.0, y=0.0 with one ART card sliver in the bottom-left corner).
 *
 * <p>The expected coordinates below are HARD-CODED from the accepted design formula only
 * ({@code start = WIDTH/2 - (n-1)*140*0.5}, {@code step = 140}, {@code y = HEIGHT*0.45}) because the
 * two helpers are private. The grid case pins the unchanged posed path (grid regression), and the
 * combat-hand case pins the fail-open probe contract for a pose-less card.
 */
public class SelectDrawPathHandFallbackTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    // ------------------------------------------------------------------ the placement Red

    /**
     * Three pose-less HAND-select cards (exactly what {@code readHandSelect} produces: zone SELECT,
     * slot index, no pose) must be laid out centred on a 1920x1080 screen: x = 820, 960, 1100 and
     * y = 486. Today the synthetic (0,0) pose wins and all three report 0.0/0.0.
     */
    @Test
    public void handSelectCardsWithoutPoseFallBackToTheOnScreenRow() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();

            publishSelectFrame(
                    SelectView.hand(
                            Arrays.asList(
                                    handCard("h1", 0),
                                    handCard("h2", 1),
                                    handCard("h3", 2)),
                            Collections.<String>emptyList(),
                            false,
                            false));

            List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
            assertEquals("three pose-less hand cards must project three card rows", 3, items.size());

            assertEquals("start = 1920/2 - (3-1)*140*0.5", 820f, items.get(0).x, 0.001f);
            assertEquals("centred slot", 960f, items.get(1).x, 0.001f);
            assertEquals("last slot = start + 2*140", 1100f, items.get(2).x, 0.001f);
            for (int i = 0; i < items.size(); i++) {
                assertTrue("U05b: hand-select card " + i + " must be on screen (x > 0), was "
                                + items.get(i).x,
                        items.get(i).x > 0f);
                assertEquals("y = 1080*0.45 for every slot", 486f, items.get(i).y, 0.001f);
                assertTrue("U05b: hand-select card " + i + " must be on screen (y > 0), was "
                                + items.get(i).y,
                        items.get(i).y > 0f);
                assertEquals("the fixed select box is unchanged", 250f, items.get(i).w, 0.001f);
                assertEquals("the fixed select box is unchanged", 350f, items.get(i).h, 0.001f);
            }
            assertFalse("the row must be distinct per slot, not stacked at the origin",
                    items.get(0).x == items.get(1).x || items.get(1).x == items.get(2).x);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    /**
     * Fail-open on the probe side: a null pose must not throw and the item row must report the same
     * on-screen placement and stay visible.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void poseLessHandSelectProbeIsNullSafeAndReportsTheRow() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();

            publishSelectFrame(
                    SelectView.hand(
                            Arrays.asList(
                                    handCard("h1", 0),
                                    handCard("h2", 1),
                                    handCard("h3", 2)),
                            Collections.<String>emptyList(),
                            false,
                            false));

            Map<String, Object> probe = SelectDrawPath.probeSlice();
            assertEquals(SelectView.KIND_HAND, probe.get("kind"));
            List<Map<String, Object>> rows = (List<Map<String, Object>>) probe.get("items");
            assertNotNull("the select probe must expose item rows", rows);
            assertTrue("the select probe must expose the hand card rows", rows.size() >= 3);

            Map<String, Object> first = rows.get(0);
            assertEquals("probe x must match the projected on-screen x", Float.valueOf(820f),
                    first.get("x"));
            assertEquals("probe y must match the projected on-screen y", Float.valueOf(486f),
                    first.get("y"));
            assertEquals("an absent pose must stay visible (fail-open)", Boolean.TRUE,
                    first.get("visible"));
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    // ------------------------------------------------------------------ grid regression

    /** A posed GRID pool keeps its native pose verbatim; the fixed 250x350 box is untouched. */
    @Test
    public void gridCardsKeepTheirProjectedPose() {
        publishSelectFrame(
                SelectView.grid(
                        Arrays.asList(
                                posedCard("g1", 0, new CardPose(111f, 222f, 0f, 1f, 0f, true)),
                                posedCard("g2", 1, new CardPose(333f, 444f, 0f, 1f, 0f, true))),
                        Collections.singletonList("g2"),
                        false,
                        false));

        List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
        assertEquals(2, items.size());
        assertEquals("grid pose x is authoritative", 111f, items.get(0).x, 0.001f);
        assertEquals("grid pose y is authoritative", 222f, items.get(0).y, 0.001f);
        assertEquals("grid pose x is authoritative", 333f, items.get(1).x, 0.001f);
        assertEquals("grid pose y is authoritative", 444f, items.get(1).y, 0.001f);
        assertEquals("scale authority is untouched", 250f, items.get(0).w, 0.001f);
        assertEquals("scale authority is untouched", 350f, items.get(0).h, 0.001f);
        assertEquals("scale authority is untouched", 250f, items.get(1).w, 0.001f);
        assertEquals("scale authority is untouched", 350f, items.get(1).h, 0.001f);
    }

    // ------------------------------------------------------------------ combat hand fail-open

    /** A pose-less combat hand card must not make the COMBAT_HAND probe throw or lose its rows. */
    @Test
    public void combatHandProbeToleratesPoseLessCards() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        "combat",
                        Arrays.asList(
                                CardView.builder(new CardRef("h1", "Strike_R")).slot(0).build(),
                                CardView.builder(new CardRef("h2", "Strike_R")).slot(1).build())));
        ArtFramework.publishFrame(backend.currentFrame());

        UiOpResult mount =
                ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
        assertTrue("combat surfaces must mount", mount.isOk());

        UiComponent hand = ArtFramework.component(SurfaceIds.COMBAT_HAND);
        assertNotNull(hand);
        Map<String, Object> slice = hand.probeSlice();
        Object rows = slice.get("hand");
        assertNotNull("the combat-hand probe must expose a hand list", rows);
        assertTrue("the combat-hand probe hand list must be a list", rows instanceof List);
        assertFalse("a pose-less hand card must still be projected", ((List<?>) rows).isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    /** Exactly what {@code readHandSelect} projects: zone SELECT, slot index, NO pose. */
    private static CardView handCard(String instanceId, int slot) {
        return CardView.builder(new CardRef(instanceId, "Strike_R"))
                .zone(CardZone.SELECT)
                .slot(slot)
                .type("ATTACK")
                .color("RED")
                .build();
    }

    private static CardView posedCard(String instanceId, int slot, CardPose pose) {
        return CardView.builder(new CardRef(instanceId, "Strike_R"))
                .zone(CardZone.SELECT)
                .slot(slot)
                .pose(pose)
                .type("ATTACK")
                .color("RED")
                .build();
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
