package artframework.sts1.backend;

import artframework.api.ArtFramework;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SelectView;
import artframework.sts1.FullPresentMode;
import artframework.sts1.assets.Sts1AtlasMaterializer;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.render.SelectDrawPath;
import artframework.sts1.render.Sts1RenderPipeline;
import org.junit.After;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * U05 Red: the HAND-select cards must publish the PORTRAIT descriptor of the native
 * {@code cards/cards.atlas} page, exactly as U04 already does for the GRID select pool.
 *
 * <p>Accepted design: {@code Sts1PresentationBackend.readHandSelect} must pass
 * {@code .art(cardArtRegionDescriptor(card))} - the SAME helper the grid path uses - instead of
 * {@code .art(ResourceIds.cardArt(card.cardID))}. The card id is not the atlas region name; the
 * native region name IS {@code AbstractCard.assetUrl}, so the descriptor is
 * {@code "cards#<assetUrl>"} and {@code SelectDrawPath.portraitDescriptor} turns it into the
 * {@code portraitResourceId} the renderer draws.
 *
 * <p><strong>(a) COVERAGE - NOT the real Red for this slice.</strong> The probe assertions on a
 * HAND-kind {@code SelectView} plus the pure {@code cardArtRegionDescriptor(String)} mapping already
 * pass today: the projection and the
 * mapping are U04-complete, only the backend WIRING is missing. They are kept because they pin the
 * observable contract the wiring must satisfy (criteria 1, 2, 4, 5); they cannot detect a revert of
 * the single {@code readHandSelect} line.
 *
 * <p><strong>(b) THE REAL RED</strong> - {@link #handSelectCardArtUsesTheSharedRegionDescriptor()} is
 * a source-level structural assertion on
 * {@code src/main/java/artframework/sts1/backend/Sts1PresentationBackend.java}. Structural, not
 * behavioural, because a live {@code AbstractCard} / {@code HandCardSelectScreen} cannot be
 * constructed in the unit suite (same precedent as {@code SelectCardTitleGeometryTest}); the test
 * strips comments first, isolates each method body by brace matching, and asserts the exact
 * {@code .art(...)} wiring in {@code readHandSelect} while keeping {@code readGridSelect} pinned.
 *
 * <p>Geometry note: {@code readHandSelect} projects NO pose, so {@code SelectDrawPath} falls back to
 * {@code defaultCardX/defaultCardY} and the fixed {@code 250f/350f} item box; the hand-select card
 * geometry is therefore NOT native-accurate today. Any such hand-geometry correction is a SEPARATE
 * slice (U05b) and is deliberately not asserted here.
 */
public class HandSelectPortraitWiringTest {

    private static final String BACKEND =
            "src/main/java/artframework/sts1/backend/Sts1PresentationBackend.java";

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.clearCache();
    }

    // ------------------------------------------------------------------ (a) coverage, passes today

    /**
     * Coverage (not the Red): a HAND-kind select view whose card carries the native art url as its
     * projected {@code artResourceId} already yields {@code cards#<url>} in the probe row, because
     * {@code SelectDrawPath.portraitDescriptor} is U04-complete. This is what the backend must feed.
     */
    @Test
    public void handSelectProbeExposesPortraitForNativeArtUrl() {
        publishHandSelectFrame(card("red/attack/strike"));

        Map<String, Object> row = firstProbeItemRow();
        assertEquals("U05: a HAND-select card with assetUrl red/attack/strike must expose the native "
                        + "cards.atlas portrait descriptor through the probe",
                "cards#red/attack/strike", row.get("portraitResourceId"));
        assertEquals("U05: the HAND select kind must be preserved on the probe",
                SelectView.KIND_HAND, SelectDrawPath.probeSlice().get("kind"));
    }

    /**
     * Coverage (not the Red): fail-open for the values a native card can carry instead of an atlas
     * region name - absent/blank/{@code "null"} and a {@code loadCardImage()} FILE path. No portrait
     * is fabricated and nothing throws.
     */
    @Test
    public void handSelectProbeFailsOpenWithoutAnAtlasRegion() {
        publishHandSelectFrame(card(""));
        assertEquals("U05: an absent art url on a HAND-select card must draw no portrait", "",
                firstProbeItemRow().get("portraitResourceId"));

        publishHandSelectFrame(card("null"));
        assertEquals("U05: the literal \"null\" art url must draw no portrait", "",
                firstProbeItemRow().get("portraitResourceId"));

        publishHandSelectFrame(card("img/cards/foo.png"));
        assertEquals("U05: a mod card that used loadCardImage() stores a FILE path; it is not a "
                        + "cards.atlas region and must draw no portrait", "",
                firstProbeItemRow().get("portraitResourceId"));
    }

    /**
     * Coverage (not the Red): pins the pure mapping the HAND path must reuse, via the public
     * package-visible entry point the backend calls.
     */
    @Test
    public void sharedHelperMapsHandCardArtUrlToTheCardsPage() {
        assertEquals("U05: the hand path must reuse the grid mapping helper",
                "cards#red/attack/strike",
                Sts1PresentationBackend.cardArtRegionDescriptor("red/attack/strike"));
        assertEquals("U05: blank must fail open", "",
                Sts1PresentationBackend.cardArtRegionDescriptor(""));
        assertEquals("U05: \"null\" must fail open", "",
                Sts1PresentationBackend.cardArtRegionDescriptor("null"));
        assertEquals("U05: a dotted loadCardImage() path must fail open", "",
                Sts1PresentationBackend.cardArtRegionDescriptor("img/cards/foo.png"));
    }

    // ------------------------------------------------------------------ (b) THE REAL RED

    /**
     * The real Red for U05. Reverting the single {@code readHandSelect} art line (back to
     * {@code ResourceIds.cardArt(card.cardID)}) must fail this test.
     */
    @Test
    public void handSelectCardArtUsesTheSharedRegionDescriptor() throws Exception {
        String source = new String(
                Files.readAllBytes(Paths.get(BACKEND)), StandardCharsets.UTF_8);
        // Strip comments FIRST: the file is heavily commented and the U04 javadoc mentions
        // ResourceIds.cardArt; a comment must never decide this test.
        String stripped = stripComments(source);

        String hand = methodBody(stripped, "private static SelectView readHandSelect(");
        String grid = methodBody(stripped, "private static SelectView readGridSelect(");

        assertTrue("U05: readHandSelect must supply the portrait via the shared helper - it has no "
                        + ".art(cardArtRegionDescriptor( call, so hand-select cards draw no portrait",
                hand.contains(".art(cardArtRegionDescriptor("));
        assertEquals("U05: readHandSelect must not keep the card-id art mapping "
                        + "(ResourceIds.cardArt(card.cardID)); the card id is not a cards.atlas "
                        + "region name - the native assetUrl is",
                0, countOccurrences(hand, "ResourceIds.cardArt("));
        assertEquals("U05: readHandSelect must contain exactly ONE .art( call (the card art field); "
                        + "a stray second .art(...) must not be able to satisfy the helper check "
                        + "while the real field stays wrong",
                1, countOccurrences(hand, ".art("));

        assertTrue("U04 regression: readGridSelect must keep the shared portrait helper",
                grid.contains(".art(cardArtRegionDescriptor("));
        assertEquals("U04 regression: readGridSelect must contain exactly one .art( call",
                1, countOccurrences(grid, ".art("));

        assertEquals("U05: the mapping logic must not be duplicated - the backend defines "
                        + "cardArtRegionDescriptor(String) exactly once",
                1, countOccurrences(stripped, "static String cardArtRegionDescriptor(String"));
        assertEquals("U05: the mapping logic must not be duplicated - the backend defines "
                        + "cardArtRegionDescriptor(AbstractCard) exactly once",
                1, countOccurrences(stripped, "private static String cardArtRegionDescriptor(AbstractCard"));
    }

    // ------------------------------------------------------------------ helpers

    /** A HAND-zone select card carrying the given native art url as the projected descriptor. */
    private static CardView card(String artResourceId) {
        return CardView.builder(new CardRef("h1", "Strike_R"))
                .zone(CardZone.HAND)
                .slot(0)
                .type("ATTACK")
                .color("RED")
                .art(artResourceId)
                .build();
    }

    private void publishHandSelectFrame(CardView card) {
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
                        SelectView.hand(
                                Collections.singletonList(card),
                                Collections.<String>emptyList(),
                                true,
                                true),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    /** The probe's {@code items[0]} row of the currently published select frame. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstProbeItemRow() {
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) SelectDrawPath.probeSlice().get("items");
        assertNotNull("U05: the select probe must expose item rows", rows);
        assertTrue("U05: the hand-select probe must expose the projected card row",
                rows.size() >= 1);
        return rows.get(0);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                return count;
            }
            count++;
            from = at + needle.length();
        }
    }

    /** Removes {@code //} line comments and {@code /* *\/} block comments (structural checks only). */
    private static String stripComments(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < text.length()
                        && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Isolates one method body by brace matching, from its signature to the matching close. */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue("U05: Sts1PresentationBackend must define '" + signature + "...'", start >= 0);
        int open = source.indexOf('{', start);
        assertTrue("U05: malformed source - no body brace after '" + signature + "'", open >= 0);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError("U05: malformed source - unbalanced braces in '" + signature + "'");
    }
}
