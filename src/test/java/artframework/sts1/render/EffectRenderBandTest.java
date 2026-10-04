package artframework.sts1.render;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Pure classification of the three instrumented {@code AbstractDungeon.render} effect call sites by
 * their native LineNumberTable line. These line numbers live in the shipped STS jar; the constants
 * are the single source of truth shared by the container seam and the probe.
 */
public class EffectRenderBandTest {

    @Test
    public void theThreeKnownNativeLinesClassifyToTheirBands() {
        assertEquals(EffectRenderBand.Band.EFFECT_LIST_BEHIND,
                EffectRenderBand.classify(2674));
        assertEquals(EffectRenderBand.Band.EFFECT_LIST_FRONT,
                EffectRenderBand.classify(2697));
        assertEquals(EffectRenderBand.Band.TOP_LEVEL_FRONT,
                EffectRenderBand.classify(2802));
    }

    @Test
    public void rankOrdersTheThreeRealBandsAndExcludesUnknown() {
        assertEquals(0, EffectRenderBand.rank(EffectRenderBand.Band.EFFECT_LIST_BEHIND));
        assertEquals(1, EffectRenderBand.rank(EffectRenderBand.Band.EFFECT_LIST_FRONT));
        assertEquals(2, EffectRenderBand.rank(EffectRenderBand.Band.TOP_LEVEL_FRONT));
        assertEquals(-1, EffectRenderBand.rank(EffectRenderBand.Band.UNKNOWN));
        assertEquals(-1, EffectRenderBand.rank(null));
        // Ranks increase with the documented band probe order (A < B < C).
        assertEquals(EffectRenderBand.Band.EFFECT_LIST_BEHIND, EffectRenderBand.bands().get(0));
        assertEquals(EffectRenderBand.Band.EFFECT_LIST_FRONT, EffectRenderBand.bands().get(1));
        assertEquals(EffectRenderBand.Band.TOP_LEVEL_FRONT, EffectRenderBand.bands().get(2));
    }

    @Test
    public void everyOtherLineIsUnknown() {
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(-1));
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(0));
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(1234));
        // Neighbors of a known line must NOT classify by proximity.
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(2673));
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(2675));
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(2698));
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(2801));
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(Integer.MIN_VALUE));
        assertEquals(EffectRenderBand.Band.UNKNOWN, EffectRenderBand.classify(Integer.MAX_VALUE));
    }

    @Test
    public void probeNamesAreExact() {
        assertEquals("effectListBehind",
                EffectRenderBand.name(EffectRenderBand.Band.EFFECT_LIST_BEHIND));
        assertEquals("effectListFront",
                EffectRenderBand.name(EffectRenderBand.Band.EFFECT_LIST_FRONT));
        assertEquals("topLevelFront",
                EffectRenderBand.name(EffectRenderBand.Band.TOP_LEVEL_FRONT));
        assertEquals("unknown", EffectRenderBand.name(EffectRenderBand.Band.UNKNOWN));
        assertEquals("unknown", EffectRenderBand.name(null));
    }

    @Test
    public void lineForRoundTripsTheKnownBandsAndMapsUnknownToMinusOne() {
        assertEquals(2674,
                EffectRenderBand.lineFor(EffectRenderBand.Band.EFFECT_LIST_BEHIND));
        assertEquals(2697,
                EffectRenderBand.lineFor(EffectRenderBand.Band.EFFECT_LIST_FRONT));
        assertEquals(2802,
                EffectRenderBand.lineFor(EffectRenderBand.Band.TOP_LEVEL_FRONT));
        assertEquals(-1, EffectRenderBand.lineFor(EffectRenderBand.Band.UNKNOWN));
        assertEquals(-1, EffectRenderBand.lineFor(null));
    }

    @Test
    public void bandsAndNamesAreStableAndDistinct() {
        assertEquals(4, EffectRenderBand.bands().size());
        Set<String> names = new HashSet<String>();
        for (EffectRenderBand.Band band : EffectRenderBand.bands()) {
            assertEquals(band, EffectRenderBand.classify(EffectRenderBand.lineFor(band)));
            names.add(EffectRenderBand.name(band));
        }
        assertEquals(4, names.size());
    }
}
