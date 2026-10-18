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

import java.util.ArrayList;
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
 * <p>U05c: the fallback row uses the PREFERRED pitch 262f (the fixed 250f card width plus a documented
 * 12 px gap) and stays centred on {@code WIDTH/2}, shrinking only when the row cannot fit:
 * {@code effectivePitch = min(262f, (WIDTH - 2*8f - 250f) / (count - 1))} with an 8 px edge margin per
 * side. The expected x coordinates below are HARD-CODED from that accepted formula
 * ({@code start = WIDTH/2 - (count-1)*effectivePitch*0.5}) because the two helpers are private.
 *
 * <p>U05d: the row y is no longer {@code HEIGHT*0.45}. The native 确认 ({@code CardSelectConfirmButton})
 * pill is FROZEN at texture {@code 512x256}, centre {@code (WIDTH/2, 475*scale)}, i.e. band
 * {@code 347..603} at scale 1; the card row must clear it instead of the confirm button moving. The
 * accepted derivation is
 * {@code y = 475*scale + 256*0.5*scale + 350*0.5 + 40 = 603 + 175 + 40 = 818} at scale 1, so the
 * frozen {@code 250x350} card band is {@code 643..993} (40 px above the pill, 87 px below the
 * 1080 px screen bottom). Every test that pins y therefore pins {@code Settings.scale = 1f}.
 *
 * <p>The grid case pins the unchanged posed path (grid regression), and the combat-hand case pins the
 * fail-open probe contract for a pose-less card.
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
     * slot index, no pose) must be laid out centred on a 1920x1080 screen at the 262f preferred pitch
     * (250f card + 12 px gap): x = 698, 960, 1222. U05d re-derives the row y to clear the frozen
     * 确认 pill: {@code 603 + 175 + 40 = 818}.
     */
    @Test
    public void handSelectCardsWithoutPoseFallBackToTheOnScreenRow() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
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

            assertEquals("start = 1920/2 - (3-1)*262*0.5", 698f, items.get(0).x, 0.001f);
            assertEquals("centred slot", 960f, items.get(1).x, 0.001f);
            assertEquals("last slot = start + 2*262", 1222f, items.get(2).x, 0.001f);
            for (int i = 0; i < items.size(); i++) {
                assertTrue("U05b: hand-select card " + i + " must be on screen (x > 0), was "
                                + items.get(i).x,
                        items.get(i).x > 0f);
                assertEquals("U05d row y = 603 (pill bottom) + 175 (card half-height) + 40 (clearance)"
                                + " = 818, clears the frozen 确认 pill",
                        818f, items.get(i).y, 0.001f);
                assertTrue("U05b: hand-select card " + i + " must be on screen (y > 0), was "
                                + items.get(i).y,
                        items.get(i).y > 0f);
                assertEquals("the fixed select box is unchanged", 250f, items.get(i).w, 0.001f);
                assertEquals("the fixed select box is unchanged", 350f, items.get(i).h, 0.001f);
            }
            assertFalse("the row must be distinct per slot, not stacked at the origin",
                    items.get(0).x == items.get(1).x || items.get(1).x == items.get(2).x);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    /**
     * Fail-open on the probe side: a null pose must not throw and the item row must report the same
     * on-screen placement (U05d {@code y = 818}) and stay visible.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void poseLessHandSelectProbeIsNullSafeAndReportsTheRow() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
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
            assertEquals("probe x must match the projected on-screen x", Float.valueOf(698f),
                    first.get("x"));
            assertEquals("probe y must match the projected on-screen y (U05d: 603 + 175 + 40)",
                    Float.valueOf(818f), first.get("y"));
            assertEquals("an absent pose must stay visible (fail-open)", Boolean.TRUE,
                    first.get("visible"));
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    /**
     * U05c: five pose-less HAND-select cards must not overlap. With the 262f preferred pitch the cards
     * sit at x = 436, 698, 960, 1222, 1484, so consecutive centres are at least one card width (250f)
     * apart, the row is still centred on 960 and every 250x350 box stays inside 1920. U05d puts that
     * frozen row at y = 818 (603 pill bottom + 175 + 40 clearance).
     */
    @Test
    public void fivePoseLessHandCardsDoNotOverlapAndStayCentred() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();

            publishSelectFrame(
                    SelectView.hand(
                            handCards(5),
                            Collections.<String>emptyList(),
                            false,
                            false));

            List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
            assertEquals("five pose-less hand cards must project five card rows", 5, items.size());

            for (int i = 0; i < items.size() - 1; i++) {
                float pitch = items.get(i + 1).x - items.get(i).x;
                assertTrue("U05c: card " + i + " and " + (i + 1) + " must not overlap; pitch was "
                                + pitch + " but the fixed card width is 250",
                        pitch >= 250f);
            }
            assertEquals("the five-card row stays centred on WIDTH/2",
                    960f, (items.get(0).x + items.get(4).x) / 2f, 0.001f);

            float[] expectedX = {436f, 698f, 960f, 1222f, 1484f};
            for (int i = 0; i < items.size(); i++) {
                SelectDrawPath.DrawItem item = items.get(i);
                assertEquals("262f pitch start = 960 - 4*262*0.5", expectedX[i], item.x, 0.001f);
                assertTrue("U05c: card " + i + " left edge must stay on screen, was "
                        + (item.x - item.w / 2f), item.x - item.w / 2f >= 0f);
                assertTrue("U05c: card " + i + " right edge must stay on screen, was "
                        + (item.x + item.w / 2f), item.x + item.w / 2f <= 1920f);
                assertEquals("the fixed select box width is unchanged", 250f, item.w, 0.001f);
                assertEquals("the fixed select box height is unchanged", 350f, item.h, 0.001f);
                assertEquals("U05d row y = 603 + 175 + 40 = 818", 818f, item.y, 0.001f);
            }
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    /**
     * U05c overflow rule: ten cards cannot fit at 262f, so the row shrinks uniformly to
     * {@code (1920 - 16 - 250)/9 = 183.777...} and the outermost box edges land exactly on the 8 px
     * margins (8 and 1912) instead of pushing cards off screen.
     */
    @Test
    public void tenPoseLessHandCardsShrinkUniformlyInsideTheEdgeMargins() {
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
                            handCards(10),
                            Collections.<String>emptyList(),
                            false,
                            false));

            List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
            assertEquals("ten pose-less hand cards must project ten card rows", 10, items.size());

            for (int i = 0; i < items.size(); i++) {
                SelectDrawPath.DrawItem item = items.get(i);
                assertTrue("U05c: card " + i + " must not be pushed left off screen, left edge was "
                        + (item.x - 125f), item.x - 125f >= 0f);
                assertTrue("U05c: card " + i + " must not be pushed right off screen, right edge was "
                        + (item.x + 125f), item.x + 125f <= 1920f);
            }
            assertEquals("the shrunk row is still centred on WIDTH/2",
                    960f, (items.get(0).x + items.get(9).x) / 2f, 0.001f);

            float clampedPitch = items.get(1).x - items.get(0).x;
            assertTrue("U05c: the 10-card row exercises the CLAMPED pitch, was " + clampedPitch,
                    clampedPitch < 262f);
            assertEquals("clamped pitch = (1920 - 2*8 - 250)/9", 1654f / 9f, clampedPitch, 0.01f);
            float[] expectedX = {
                    133f,
                    316.77777f,
                    500.55555f,
                    684.33333f,
                    868.11111f,
                    1051.88888f,
                    1235.66666f,
                    1419.44444f,
                    1603.22222f,
                    1787f
            };
            for (int i = 0; i < items.size(); i++) {
                assertEquals("shrunk slot " + i + " position", expectedX[i], items.get(i).x, 0.01f);
            }
            assertEquals("start edge sits on the 8 px margin", 8f,
                    items.get(0).x - 125f, 0.01f);
            assertEquals("end edge sits on the 8 px margin", 1912f,
                    items.get(9).x + 125f, 0.01f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    /**
     * U05c: a single pose-less hand card sits exactly on the screen centre; U05d re-derives its row y
     * to clear the frozen 确认 pill ({@code y = 818} at scale 1).
     */
    @Test
    public void singlePoseLessHandCardSitsAtTheScreenCentre() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();

            publishSelectFrame(
                    SelectView.hand(
                            handCards(1),
                            Collections.<String>emptyList(),
                            false,
                            false));

            List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
            assertEquals(1, items.size());
            assertEquals("a lone card is centred with no pitch", 960f, items.get(0).x, 0.001f);
            assertEquals("U05d row y = 603 + 175 + 40 = 818", 818f, items.get(0).y, 0.001f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    // ------------------------------------------------------------------ U05d clearance

    /**
     * U05d (the real Red). The hand-select card ROW must clear the native 确认
     * ({@code CardSelectConfirmButton}) pill rather than the pill moving: the pill is FROZEN at
     * texture {@code 512x256}, centre {@code (WIDTH/2, 475*scale)}, so its bottom edge at scale 1 is
     * {@code 475 + 128 = 603}. The accepted derivation for the row is
     * {@code 475*scale + 256*0.5*scale + 350*0.5 + 40 = 603 + 175 + 40 = 818}, i.e. the card band top
     * ({@code y - 175}) sits at least the documented 40 px CLEARANCE below the pill bottom. On the
     * pre-U05d code the row is {@code HEIGHT*0.45 = 486}, so the card band is {@code 311..661} and
     * overlaps the pill band {@code 347..603} by 314 px - the overlap this slice removes. The pill
     * geometry is read from
     * {@link SelectDrawPath#confirmItem()} instead of re-hardcoding 475/128, because
     * {@code SelectDrawPathTest} already pins those constants.
     */
    @Test
    public void handSelectRowClearsTheFrozenConfirmPillByFortyPixels() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();

            publishSelectFrame(
                    SelectView.hand(
                            handCards(5),
                            Collections.<String>emptyList(),
                            true,
                            true));

            List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
            List<SelectDrawPath.DrawItem> cards = cardItems(items);
            assertEquals("five pose-less hand cards must project five card rows", 5, cards.size());
            SelectDrawPath.DrawItem confirm = confirmOf(items);
            assertNotNull("confirmEnabled/confirmVisible must project the 确认 pill too", confirm);
            assertTrue("the separated pill item must be the confirm pseudo-item", confirm.confirm);

            SelectDrawPath.DrawItem pill = SelectDrawPath.confirmItem();
            assertNotNull("the frozen 确认 pill geometry must be available", pill);
            float pillTop = pill.y - pill.h / 2f;
            float pillBottom = pill.y + pill.h / 2f;

            for (int i = 0; i < cards.size(); i++) {
                SelectDrawPath.DrawItem card = cards.get(i);
                float cardTop = card.y - card.h / 2f;
                float cardBottom = card.y + card.h / 2f;
                assertTrue("U05d: hand-select card " + i + " (band " + cardTop + ".." + cardBottom
                                + ", centre y=" + card.y + ") must clear the 确认 pill band "
                                + pillTop + ".." + pillBottom + " (centre y=" + pill.y + ", h="
                                + pill.h + ") by at least 40 px; measured gap was "
                                + (cardTop - pillBottom) + " px",
                        cardTop >= pillBottom + 40f);
                assertEquals("U05d: row y = pill bottom 603 + card half-height 175 + clearance 40 = 818",
                        818f, card.y, 0.001f);
            }
            assertEquals("the pill must not move as a side effect of the row change",
                    960f, confirm.x, 0.001f);
            assertEquals("the pill must not move as a side effect of the row change",
                    475f, confirm.y, 0.001f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    /**
     * U05d-01 (review): the row y must be DERIVED from the native frozen pill geometry, not pinned as
     * a literal. Every other U05d test runs at {@code scale = 1f}, where a hardcoded {@code 818f} (or a
     * lost {@code * scale} on the textured terms) would still pass. This runs at a NON-UNIT scale and
     * recomputes the expectation from the pill the code itself projects.
     */
    @Test
    public void rowYIsDerivedFromTheNativePillAtNonUnitScale() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1350));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1.25f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();

            publishSelectFrame(
                    SelectView.hand(
                            handCards(5), Collections.<String>emptyList(), true, true));

            List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
            List<SelectDrawPath.DrawItem> cards = cardItems(items);
            assertEquals("five pose-less hand cards must project five card rows", 5, cards.size());
            SelectDrawPath.DrawItem pill = SelectDrawPath.confirmItem();
            assertNotNull("the frozen 确认 pill geometry must be available", pill);

            // Derived expectation: the pill's own projected band plus the frozen card half-height
            // plus the documented clearance. A hardcoded 818 cannot satisfy this at scale 1.25.
            float expectedY = (pill.y + pill.h / 2f) + (cards.get(0).h / 2f) + 40f;
            assertEquals("U05d: the row y must be DERIVED from the native pill band (not a literal)",
                    expectedY, cards.get(0).y, 0.001f);
            assertEquals("U05d: the pill must keep its own native geometry: 475 * 1.25 = 593.75",
                    593.75f, pill.y, 0.001f);
            assertTrue("U05d: at scale 1.25 the row is still above the pill with the full clearance; "
                            + "measured gap " + ((cards.get(0).y - cards.get(0).h / 2f)
                            - (pill.y + pill.h / 2f)) + " px",
                    cards.get(0).y - cards.get(0).h / 2f
                            >= pill.y + pill.h / 2f + 40f);
            assertTrue("U05d: the 350-tall card band must stay on the 1350-tall screen; band top "
                            + (cards.get(0).y + cards.get(0).h / 2f) + " <= 1350",
                    cards.get(0).y + cards.get(0).h / 2f <= 1350f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT",
                    Integer.valueOf(previousHeight));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH",
                    Integer.valueOf(previousWidth));
        }
    }

    /**
     * U05d guard for the NEW lower position: at 1920x1080 the re-derived row keeps every frozen
     * {@code 250x350} card box fully on screen ({@code y - 175 >= 0} and {@code y + 175 <= 1080}) and
     * leaves the confirm pill exactly where native froze it ({@code 512x256} centred at
     * {@code (960, 475)}). May pass before Green; it exists so a future row tweak cannot silently
     * push cards off the bottom or "fix" the overlap by moving the pill instead.
     */
    @Test
    public void handSelectRowStaysOnScreenAndLeavesTheConfirmPillGeometryFrozen() {
        int previousWidth = com.megacrit.cardcrawl.core.Settings.WIDTH;
        int previousHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
        float previousScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "WIDTH", Integer.valueOf(1920));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "HEIGHT", Integer.valueOf(1080));
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale", Float.valueOf(1f));
            ArtFramework.resetForTests();
            Sts1RenderPipeline.resetForTests();
            FullPresentMode.resetForTests();

            publishSelectFrame(
                    SelectView.hand(
                            handCards(5),
                            Collections.<String>emptyList(),
                            true,
                            true));

            List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
            List<SelectDrawPath.DrawItem> cards = cardItems(items);
            assertEquals(5, cards.size());
            SelectDrawPath.DrawItem confirm = confirmOf(items);
            assertNotNull(confirm);

            float screenHeight = com.megacrit.cardcrawl.core.Settings.HEIGHT;
            for (int i = 0; i < cards.size(); i++) {
                SelectDrawPath.DrawItem card = cards.get(i);
                assertTrue("U05d: card " + i + " top edge must stay on screen (y - 175 >= 0), was "
                                + (card.y - card.h / 2f),
                        card.y - card.h / 2f >= 0f);
                assertTrue("U05d: card " + i + " bottom edge must stay on screen (y + 175 <= "
                                + screenHeight + "), was " + (card.y + card.h / 2f),
                        card.y + card.h / 2f <= screenHeight);
            }

            assertEquals("the 确认 pill keeps its frozen centre x (WIDTH/2)", 960f, confirm.x, 0.001f);
            assertEquals("the 确认 pill keeps its frozen centre y (475*scale)", 475f, confirm.y,
                    0.001f);
            assertEquals("the 确认 pill keeps its frozen texture width 512", 512f, confirm.w,
                    0.001f);
            assertEquals("the 确认 pill keeps its frozen texture height 256", 256f, confirm.h,
                    0.001f);
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousScale));
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

    private static List<CardView> handCards(int count) {
        List<CardView> cards = new ArrayList<CardView>();
        for (int i = 0; i < count; i++) {
            cards.add(handCard("h" + (i + 1), i));
        }
        return cards;
    }

    /** The projected card rows (the confirm pseudo-item excluded), in projection order. */
    private static List<SelectDrawPath.DrawItem> cardItems(List<SelectDrawPath.DrawItem> items) {
        List<SelectDrawPath.DrawItem> cards = new ArrayList<SelectDrawPath.DrawItem>();
        for (SelectDrawPath.DrawItem item : items) {
            if (!item.confirm) {
                cards.add(item);
            }
        }
        return cards;
    }

    /** The single projected confirm pseudo-item, or {@code null} when the frame projected none. */
    private static SelectDrawPath.DrawItem confirmOf(List<SelectDrawPath.DrawItem> items) {
        for (SelectDrawPath.DrawItem item : items) {
            if (item.confirm) {
                return item;
            }
        }
        return null;
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
