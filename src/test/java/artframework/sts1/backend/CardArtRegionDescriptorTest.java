package artframework.sts1.backend;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * U04 review finding: the only new production read in the card-portrait slice is the
 * {@code AbstractCard.assetUrl -> "cards#<region>"} mapping, and it was previously reachable only
 * through an {@code AbstractCard} the unit suite never constructs (so reverting it stayed green).
 * These pin the pure mapping directly.
 *
 * <p>Native authority: the card art region name IS {@code AbstractCard.assetUrl}
 * ({@code cardAtlas.findRegion(assetUrl)}), so a slash-separated region name maps to
 * {@code cards#<url>}. A {@code loadCardImage()} card stores a FILE path in the same field, which is
 * not an atlas region and must map to {@code ""} (no fabricated, unresolvable descriptor).
 */
public class CardArtRegionDescriptorTest {

    @Test
    public void nativeAtlasRegionUrlMapsToCardsDescriptor() {
        assertEquals("cards#red/attack/strike",
                Sts1PresentationBackend.cardArtRegionDescriptor("red/attack/strike"));
        assertEquals("cards#blue/skill/aggregate",
                Sts1PresentationBackend.cardArtRegionDescriptor("blue/skill/aggregate"));
        assertEquals("cards#curse/ascenders_bane",
                Sts1PresentationBackend.cardArtRegionDescriptor("curse/ascenders_bane"));
    }

    @Test
    public void surroundingWhitespaceIsTrimmedBeforeMapping() {
        assertEquals("cards#red/attack/strike",
                Sts1PresentationBackend.cardArtRegionDescriptor("  red/attack/strike  "));
    }

    @Test
    public void alreadyQualifiedValuePassesThroughUnchanged() {
        assertEquals("cards#red/attack/strike",
                Sts1PresentationBackend.cardArtRegionDescriptor("cards#red/attack/strike"));
    }

    @Test
    public void absentOrNullishValueMapsToNoPortrait() {
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor((String) null));
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor(""));
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor("   "));
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor("null"));
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor("NULL"));
    }

    /**
     * A mod card using native {@code loadCardImage()} stores a FILE path in {@code assetUrl}
     * (e.g. {@code img/cards/foo.png}). That is not a cards.atlas region, so it must not become a
     * {@code cards#...} descriptor that can never resolve.
     */
    @Test
    public void filePathUrlIsNotTreatedAsAnAtlasRegion() {
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor("img/cards/foo.png"));
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor("foo.png"));
        assertEquals("", Sts1PresentationBackend.cardArtRegionDescriptor("cards/art/foo.jpg"));
    }
}
