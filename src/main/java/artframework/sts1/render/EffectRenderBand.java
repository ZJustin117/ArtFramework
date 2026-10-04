package artframework.sts1.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Native local render-order baseline for claimed transient effects: classifies the three
 * instrumented {@code AbstractGameEffect.render(SpriteBatch)} call sites inside
 * {@code com.megacrit.cardcrawl.dungeons.AbstractDungeon.render} by their LineNumberTable line.
 *
 * <p>The effect-container seam ({@code TransientEffectContainerPatches.observeThenRender}) replaces
 * those three call sites IN PLACE, so the native traversal order is inherently preserved and the
 * band an observation is recorded in IS native order. This class only names the band that a
 * recorded observation belongs to; it proves a claimed effect lands in the correct native band
 * instead of blindly trusting call-site replacement.
 *
 * <p>The three verified native sites (from {@code javap -l -c -p} on the shipped STS jar) are:
 * <ul>
 *   <li>{@link #LINE_EFFECT_LIST_BEHIND line 2674} — the first {@code effectList} iteration loop,
 *       drawing only effects with {@code renderBehind == true}. It runs before {@code room.render},
 *       so it is BEHIND the room/character ({@link Band#EFFECT_LIST_BEHIND}).</li>
 *   <li>{@link #LINE_EFFECT_LIST_FRONT line 2697} — the second {@code effectList} iteration loop,
 *       drawing only effects with {@code renderBehind == false}. It runs after the room/character
 *       and combat foreground, before the overlay/top panel ({@link Band#EFFECT_LIST_FRONT}).</li>
 *   <li>{@link #LINE_TOP_LEVEL_FRONT line 2802} — the {@code topLevelEffects} iteration loop,
 *       drawing only effects with {@code renderBehind == false}. It runs after
 *       {@code TopPanel.render}/{@code renderAboveTopPanel} ({@link Band#TOP_LEVEL_FRONT}).</li>
 * </ul>
 *
 * <p>Any other line number (including {@code -1}, the value used when the call-site line is not
 * known — e.g. the legacy 2-arg observation entry) classifies as {@link Band#UNKNOWN}. These line
 * numbers are baked into the shipped jar's LineNumberTable; if they ever drift, the classification
 * degrades to {@link Band#UNKNOWN} and a D1 assertion fails loudly rather than silently passing.
 *
 * <p>Pure data: no engine imports, no GL, no state.
 */
public final class EffectRenderBand {

    /** Native band an observed transient-effect render happened in. */
    public enum Band {
        /** {@code effectList}, {@code renderBehind == true}, before the room/character. */
        EFFECT_LIST_BEHIND,
        /** {@code effectList}, {@code renderBehind == false}, after the room/character. */
        EFFECT_LIST_FRONT,
        /** {@code topLevelEffects}, {@code renderBehind == false}, above the top panel. */
        TOP_LEVEL_FRONT,
        /** Any other line number (including {@code -1}); unknown native band. */
        UNKNOWN
    }

    /** First {@code effectList} loop, {@code renderBehind == true} (behind room/character). */
    public static final int LINE_EFFECT_LIST_BEHIND = 2674;
    /** Second {@code effectList} loop, {@code renderBehind == false} (after room/character). */
    public static final int LINE_EFFECT_LIST_FRONT = 2697;
    /** {@code topLevelEffects} loop, {@code renderBehind == false} (above the top panel). */
    public static final int LINE_TOP_LEVEL_FRONT = 2802;

    /** Probe key for {@link Band#EFFECT_LIST_BEHIND}. */
    public static final String NAME_EFFECT_LIST_BEHIND = "effectListBehind";
    /** Probe key for {@link Band#EFFECT_LIST_FRONT}. */
    public static final String NAME_EFFECT_LIST_FRONT = "effectListFront";
    /** Probe key for {@link Band#TOP_LEVEL_FRONT}. */
    public static final String NAME_TOP_LEVEL_FRONT = "topLevelFront";
    /** Probe key for {@link Band#UNKNOWN}. */
    public static final String NAME_UNKNOWN = "unknown";

    /** Stable probe/list order of the bands (all four always present, zero-filled). */
    private static final List<Band> BANDS = Collections.unmodifiableList(Arrays.asList(
            Band.EFFECT_LIST_BEHIND, Band.EFFECT_LIST_FRONT, Band.TOP_LEVEL_FRONT, Band.UNKNOWN));

    private EffectRenderBand() {}

    /** All bands in stable probe order. */
    public static List<Band> bands() {
        return BANDS;
    }

    /** Classifies a native call-site line number; every unrecognized value is {@link Band#UNKNOWN}. */
    public static Band classify(int lineNumber) {
        if (lineNumber == LINE_EFFECT_LIST_BEHIND) return Band.EFFECT_LIST_BEHIND;
        if (lineNumber == LINE_EFFECT_LIST_FRONT) return Band.EFFECT_LIST_FRONT;
        if (lineNumber == LINE_TOP_LEVEL_FRONT) return Band.TOP_LEVEL_FRONT;
        return Band.UNKNOWN;
    }

    /** The native LineNumberTable line of a band, or {@code -1} for {@link Band#UNKNOWN}. */
    public static int lineFor(Band band) {
        if (band == null) return -1;
        switch (band) {
            case EFFECT_LIST_BEHIND:
                return LINE_EFFECT_LIST_BEHIND;
            case EFFECT_LIST_FRONT:
                return LINE_EFFECT_LIST_FRONT;
            case TOP_LEVEL_FRONT:
                return LINE_TOP_LEVEL_FRONT;
            default:
                return -1;
        }
    }

    /**
     * Native within-frame draw ordinal of a band: {@code EFFECT_LIST_BEHIND = 0},
     * {@code EFFECT_LIST_FRONT = 1}, {@code TOP_LEVEL_FRONT = 2}. {@link Band#UNKNOWN} (and a null
     * band) return {@code -1} and are NOT ordering participants: only the three real bands define a
     * frame's draw sequence, so an UNKNOWN observation never contributes to order evidence.
     */
    public static int rank(Band band) {
        if (band == null) return -1;
        switch (band) {
            case EFFECT_LIST_BEHIND:
                return 0;
            case EFFECT_LIST_FRONT:
                return 1;
            case TOP_LEVEL_FRONT:
                return 2;
            default:
                return -1;
        }
    }

    /** The exact probe key of a band; null maps to {@link #NAME_UNKNOWN}. */
    public static String name(Band band) {
        if (band == null) return NAME_UNKNOWN;
        switch (band) {
            case EFFECT_LIST_BEHIND:
                return NAME_EFFECT_LIST_BEHIND;
            case EFFECT_LIST_FRONT:
                return NAME_EFFECT_LIST_FRONT;
            case TOP_LEVEL_FRONT:
                return NAME_TOP_LEVEL_FRONT;
            default:
                return NAME_UNKNOWN;
        }
    }
}
