package artframework.sts1.render;

import artframework.api.ArtFramework;
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
import artframework.sts1.FullPresentMode;
import artframework.sts1.assets.Sts1AtlasMaterializer;
import artframework.sts1.input.CombatInputRouter;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * U07 Red: select-screen cards must draw their DESCRIPTION text like native
 * {@code AbstractCard.renderDescription}, instead of the current empty lower half of the card
 * (see {@code debug-artifacts/art-verify/d1_u04_select_grid.png} and
 * {@code d1_u05c_hand_select.png}).
 *
 * <p><strong>Accepted contract (parent ruling).</strong> The text source is the RAW
 * {@link CardView#description} string (native {@code card.rawDescription}); ART has no native
 * pre-wrapped {@code List<DescriptionLine>}, so ART wraps the raw text itself with a deterministic
 * PURE function and tokenises {@code *keyword*} spans with TOGGLE semantics.
 *
 * <p><strong>Geometry authority.</strong> {@code descScale = item.w / 512f} - the same uniform ART
 * card scale U06 already uses for the title and U02d3 for the cost number - NOT the native
 * {@code card.drawScale}: every {@code cardui} region is authored on a 512-wide page, so the ART
 * scale absorbs the host {@code Settings.scale}. Native references
 * ({@code /home/justinz/stsReference/decompiled/sts1/cfad868ac8d65a88/com/megacrit/cardcrawl/cards/AbstractCard.java}):
 * <ul>
 *   <li>1561: {@code draw_y = current_y - IMG_HEIGHT * drawScale / 2.0f + DESC_OFFSET_Y * drawScale;}</li>
 *   <li>1562: {@code draw_y += description.size() * font.getCapHeight() * 0.775f
 *           - font.getCapHeight() * 0.375f;}</li>
 *   <li>1563: {@code spacing = 1.45f * -font.getCapHeight() / Settings.scale / drawScale;}</li>
 *   <li>1565: {@code start_x = current_x - description.get(i).width * drawScale / 2.0f;}</li>
 *   <li>1567/1576 and 1648: leading {@code '*'} draws in {@code goldColor} with the star stripped,
 *           the rest in {@code textColor}; 152/154 define those colours.</li>
 *   <li>1835-1839 {@code getDescFont()} predicate {@code angle == 0.0f && drawScale == 1.0f}
 *           selects {@code cardDescFont_N} else {@code cardDescFont_L}, then
 *           {@code font.getData().setScale(drawScale)}; 1652 restores {@code setScale(1.0f)}.</li>
 *   <li>2626 {@code IMG_HEIGHT = 420.0f * Settings.scale}; 2636
 *           {@code DESC_OFFSET_Y = BIG_TEXT_MODE ? IMG_HEIGHT*0.24f : IMG_HEIGHT*0.255f}.</li>
 *   <li>1066-1075 {@code renderCard} order: {@code renderImage} -> {@code renderTitle} ->
 *           {@code renderType} -> description -> {@code renderTint} -> {@code renderEnergy}; the
 *           accepted slice only requires "description after title".</li>
 * </ul>
 *
 * <p><strong>Why reflection?</strong> Every U07 member (constants, pure functions, nested
 * {@code DescriptionChunk}, new {@link SelectDrawPath.DrawItem} fields, new {@code toMap()} keys)
 * does not exist yet. Referring to them directly would be a COMPILE error - an infrastructure
 * failure rather than Red. Each new member is therefore resolved reflectively, so this class
 * compiles today and every U07 test fails at RUNTIME with an {@link AssertionError} that names the
 * missing member. The one exception is {@link #renderSelectDrawsDescriptionAfterTitleInsideCardBranch()},
 * a source-shape guard that needs no reflection and is a real Red on its own.
 */
public class SelectCardDescriptionTest {

    /** Real select-card box width published by {@link SelectDrawPath#buildFromProjection()}. */
    private static final float SELECT_W = 250f;

    /** Native {@code card.drawScale} authority for ART is the uniform card scale {@code w / 512f}. */
    private static final float UNIFORM_SCALE = SELECT_W / 512f; // 0.48828125

    /**
     * Native anchor {@code -(IMG_HEIGHT/2 - DESC_OFFSET_Y) * drawScale}
     * = {@code -(210 - 107.1) * 0.48828125} = {@code -50.244140625}.
     */
    private static final float DESC_ANCHOR_Y = -102.9f * UNIFORM_SCALE;

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.clearCache();
    }

    // ============================================================ 1. PURE wrap

    /**
     * Non-CJK greedy WORD wrap on single spaces (native wraps on spaces against
     * {@code DESC_BOX_WIDTH}, AbstractCard 465; ART pins a deterministic equivalent).
     *
     * <p>{@code "Deal 5 damage. Apply 1 Weak."} at 15 chars: {@code "Deal 5 damage."} is 14 chars,
     * so adding {@code " Apply"} (20 total) exceeds 15 and the second line starts at
     * {@code "Apply"}; {@code "Apply 1 Weak."} is 13. The limit 15 is deliberately NOT equal to
     * either line length, so this test pins the greedy rule without also pinning a
     * {@code >}/&gt;= boundary convention the contract does not state. Words are rejoined with
     * exactly one space.
     */
    @Test
    public void wrapDescriptionWordWrapsEnglishOnSpaces() {
        assertEquals("U07: non-CJK wrap is greedy on single spaces, rejoined with ONE space: "
                        + "\"Deal 5 damage.\" (14) + \"Apply 1 Weak.\" (13) at 15 chars",
                Arrays.asList("Deal 5 damage.", "Apply 1 Weak."),
                wrapDescription("Deal 5 damage. Apply 1 Weak.", 15, 5));

        assertEquals("U07: a word LONGER than maxCharsPerLine stays whole on its own line "
                        + "(never split mid-word)",
                Arrays.asList("Extraordinarily", "long"),
                wrapDescription("Extraordinarily long", 5, 5));

        assertEquals("U07: a short description that fits stays on ONE line",
                Arrays.asList("Deal 5 damage."),
                wrapDescription("Deal 5 damage.", 24, 5));
    }

    /**
     * CJK mode is detected by code-point script (HAN / HIRAGANA / KATAKANA / HANGUL) and wraps by
     * CHARACTER, because native {@code initializeDescriptionCN} does not wrap CJK on spaces.
     * Whitespace is collapsed to nothing first.
     */
    @Test
    public void wrapDescriptionWrapsCjkByCharacter() {
        assertTrue("U07: isCjkText must detect HAN (\"一二三\")",
                isCjkText("一二三"));
        assertTrue("U07: isCjkText must detect HIRAGANA (\"ひらがな\")",
                isCjkText("ひらがな"));
        assertTrue("U07: isCjkText must detect HANGUL (\"한글\")",
                isCjkText("한글"));
        assertFalse("U07: isCjkText must be false for plain English (\"Deal 5\")",
                isCjkText("Deal 5"));

        assertEquals("U07: CJK wraps by CHARACTER into chunks of at most maxCharsPerLine: "
                        + "\"一二三四五六七八九十\" (10 chars) at 5 chars = 5 + 5",
                Arrays.asList("一二三四五", "六七八九十"),
                wrapDescription("一二三四五六七八九十", 5, 5));

        assertEquals("U07: CJK by-character wrap also applies when the length is NOT a multiple of "
                        + "maxCharsPerLine: \"一二三四五六七八九十\" at 4 chars = 4 + 4 + 2",
                Arrays.asList("一二三四", "五六七八", "九十"),
                wrapDescription("一二三四五六七八九十", 4, 5));

        assertEquals("U07: CJK mode collapses whitespace to nothing before cutting "
                        + "(\"一二 三\" -> \"一二三\")",
                Arrays.asList("一二三"),
                wrapDescription("一二 三", 5, 5));
    }

    /**
     * {@code maxLines} is a documented TRUNCATION: lines beyond the cap are dropped, NOT replaced by
     * an ellipsis. Native instead shrinks the font over 5 lines; ART's accepted contract caps at
     * {@code DESC_MAX_LINES}.
     */
    @Test
    public void wrapDescriptionDropsLinesBeyondTheCap() {
        List<String> lines = wrapDescription("aaa bbb ccc ddd eee", 5, 2);

        assertEquals("U07: maxLines=2 must yield exactly 2 lines (\"aaa\", \"bbb\"); the tail "
                        + "\"ccc ddd eee\" is DROPPED, not ellipsised",
                2, lines.size());
        assertEquals("U07: truncation keeps the FIRST lines in order (line 0 = \"aaa\")",
                "aaa", lines.get(0));
        assertEquals("U07: truncation keeps the FIRST lines in order (line 1 = \"bbb\")",
                "bbb", lines.get(1));
        assertFalse("U07: truncation is not an ellipsis - no line may end with \"...\"/\"…\"",
                lines.get(1).endsWith("...") || lines.get(1).endsWith("…"));
        assertFalse("U07: truncation must not leak the dropped tail back into the list",
                lines.contains("ccc") || lines.contains("ddd") || lines.contains("eee"));

        assertEquals("U07: a 5-line description at maxLines=5 is NOT truncated",
                5, wrapDescription("aaaa bbbb cccc dddd eeee", 4, 5).size());
    }

    /**
     * U07b: the native manual line-break token {@code NL} is REAL transmitted data, not literal text:
     * {@code debug-artifacts/art-verify/d1_verify_background.json} carries e.g.
     * {@code "造成 !D! 点伤害。 NL 给予 !M! 层 易伤 。"}, and U07 as landed renders the two literal
     * characters "NL" on the card face.
     *
     * <p>Native authority {@code AbstractCard.initializeDescription()}:
     * L380 {@code for (String word : this.rawDescription.split(" "))} splits on the literal SPACE,
     * and L454-460 {@code } else if (word.equals("NL"))} flushes the accumulated builder into a
     * {@code DescriptionLine} and resets it. ART therefore turns a whole {@code NL} token into a
     * FORCED line break instead of drawing it.
     */
    @Test
    public void wrapDescriptionHonoursNativeNlAsForcedLineBreak() {
        List<String> broken = wrapDescription("Deal 5 damage NL Exhaust.", 24, 5);
        assertEquals("U07b: the native NL token is a FORCED line break - \"Deal 5 damage NL "
                        + "Exhaust.\" must draw as exactly 2 lines, the pre-break text on line 0 "
                        + "and the post-break text on a NEW line 1; actual " + broken,
                Arrays.asList("Deal 5 damage", "Exhaust."), broken);
        for (String line : broken) {
            assertFalse("U07b: acceptance criterion - the two literal characters \"NL\" must no "
                            + "longer be emitted on the card face; actual line \"" + line + "\"",
                    line.contains("NL"));
        }

        assertEquals("U07b: text BEFORE a break is still subject to the normal U07 greedy wrap - "
                        + "\"aaa bbb ccc\" at 5 chars is already 3 lines, and the break then "
                        + "starts \"ddd\" on line 3",
                Arrays.asList("aaa", "bbb", "ccc", "ddd"),
                wrapDescription("aaa bbb ccc NL ddd", 5, 5));

        assertEquals("U07b: the break consumes the WHOLE token and never splits a word - "
                        + "\"Exhaust\" survives intact on both sides of the break",
                Arrays.asList("Exhaust", "Exhaust"),
                wrapDescription("Exhaust NL Exhaust", 24, 5));
    }

    /**
     * U07b: the native match rule is EXACT, WHOLE-TOKEN and CASE-SENSITIVE -
     * {@code } else if (word.equals("NL"))} (AbstractCard L454) is an equality test on ONE
     * space-split token, NOT an {@code indexOf("NL")} substring scan. So a word that merely
     * CONTAINS "NL" ({@code "PLANL"}, {@code "NLNL"}), and any other casing ({@code "nl"},
     * {@code "Nl"}), stays literal text. (ART tokenises on {@code \s+}, a strict superset of the
     * native literal space that cannot mis-handle the real space-delimited device data.)
     */
    @Test
    public void wrapDescriptionNlBoundaryIsWholeTokenAndCaseSensitive() {
        assertEquals("U07b: native is word.equals(\"NL\") (AbstractCard L454), not indexOf - a word "
                        + "merely CONTAINING the letters (\"PLANL\") is NOT a break and stays literal",
                Arrays.asList("PLANL"), wrapDescription("PLANL", 24, 5));

        assertEquals("U07b: the match is CASE-SENSITIVE - lowercase \"nl\" is NOT the native token "
                        + "and stays literal text",
                Arrays.asList("Deal nl damage"), wrapDescription("Deal nl damage", 24, 5));

        assertEquals("U07b: mixed case \"Nl\" is NOT the native token and stays literal text",
                Arrays.asList("Deal Nl damage"), wrapDescription("Deal Nl damage", 24, 5));

        assertEquals("U07b: ONE token \"NLNL\" is NOT two breaks - the token must EQUAL \"NL\" "
                        + "exactly, so it stays literal",
                Arrays.asList("NLNL"), wrapDescription("NLNL", 24, 5));

        assertEquals("U07b: ... while a whole-token NL NEXT TO such a word still breaks, so the "
                        + "literal-vs-break distinction is token equality, not substring or position",
                Arrays.asList("PLANL", "damage"),
                wrapDescription("PLANL NL damage", 24, 5));
    }

    /**
     * U07b follow-up (review F1): ART tokenises on {@code \\s+} while native splits on the literal
     * space ({@code rawDescription.split(" ")}, AbstractCard L380). ART is therefore a strict SUPERSET:
     * a TAB/newline-delimited {@code NL} breaks in ART but NOT natively. That difference is WIDER than
     * native but harmless (real data is space-delimited), so it is pinned as the documented ART
     * behaviour rather than claimed as native parity - and pinning it means a future narrowing to
     * {@code split(" ")} is a deliberate, visible change rather than a silent one.
     */
    @Test
    public void wrapDescriptionNlBreaksOnAnyWhitespaceARTIsWiderThanNative() {
        assertEquals("U07b: ART splits on \\s+, so a TAB-delimited NL breaks here (native splits on a "
                        + "literal space only, so this is ART-WIDER, not native parity)",
                Arrays.asList("Deal 5 damage", "Exhaust."),
                wrapDescription("Deal 5 damage\tNL\tExhaust.", 24, 5));
        assertEquals("U07b: likewise for a newline-delimited NL",
                Arrays.asList("Deal 5 damage", "Exhaust."),
                wrapDescription("Deal 5 damage\nNL\nExhaust.", 24, 5));
        assertEquals("U07b: and the space-delimited form (the real-data form) breaks identically",
                Arrays.asList("Deal 5 damage", "Exhaust."),
                wrapDescription("Deal 5 damage NL Exhaust.", 24, 5));
    }

    /**
     * U07b follow-up (review F2): the CJK branch must drop leading/trailing/doubled {@code NL} the
     * same way the word branch does - it is a separate code path, so the empty-line policy has to be
     * pinned there independently.
     *
     * <p>The {@code NL} must be its OWN space-delimited token, exactly as native requires:
     * {@code initializeDescriptionCN} splits on the literal space (AbstractCard L490) and then tests
     * {@code word.equals("NL")} (L494/L598). {@code "NL不能被打出。"} is therefore ONE token that merely
     * CONTAINS those letters, not a break - native does not break there either, so ART keeping it
     * literal is the faithful behaviour (a non-space-adjacent NL is NOT a break in the CN path).
     */
    @Test
    public void wrapDescriptionDropsCjkNlEdgesAndDoubles() {
        assertEquals("U07b: a leading token NL in CJK text must not produce a leading blank line",
                Arrays.asList("\u4e0d\u80fd\u88ab\u6253\u51fa\u3002"),
                wrapDescription("NL \u4e0d\u80fd\u88ab\u6253\u51fa\u3002", 24, 5));
        assertEquals("U07b: a trailing token NL in CJK text must not produce a trailing blank line",
                Arrays.asList("\u4e0d\u80fd\u88ab\u6253\u51fa\u3002"),
                wrapDescription("\u4e0d\u80fd\u88ab\u6253\u51fa\u3002 NL", 24, 5));
        assertEquals("U07b: a doubled token NL in CJK text must not produce an empty line",
                Arrays.asList("\u4e0d\u80fd\u88ab\u6253\u51fa\u3002", "\u865a\u65e0\u3002"),
                wrapDescription(
                        "\u4e0d\u80fd\u88ab\u6253\u51fa\u3002 NL NL \u865a\u65e0\u3002", 24, 5));
        assertEquals("U07b: NL glued to CJK text is NOT a native break (one space-split token that "
                        + "merely contains the letters), so it stays literal",
                Arrays.asList("NL\u4e0d\u80fd\u88ab\u6253\u51fa\u3002"),
                wrapDescription("NL\u4e0d\u80fd\u88ab\u6253\u51fa\u3002", 24, 5));
    }

    /**
     * U07b: EMPTY LINES ARE DROPPED, never emitted as {@code ""}. A blank entry would burn one of
     * the 5 {@code DESC_MAX_LINES} slots and shift every later line down - an invisible line on the
     * card face.
     *
     * <p>Policy authority is the CJK path, the one that actually routes the observed D1 Chinese data
     * ({@code Settings.setLanguage} sets {@code lineBreakViaCharacter=true} for ZHS/ZHT/JPN):
     * {@code initializeDescriptionCN()} skips {@code NL} while the builder is empty - AbstractCard
     * L494 {@code if (word.equals("NL") && sbuilder.length() == 0 || word.isEmpty())} - only breaks
     * when the builder already holds text (L598
     * {@code if ((Settings.manualLineBreak || Settings.manualAndAutoLineBreak) && word.equals("NL") && sbuilder.length() != 0)}),
     * and never materialises a trailing empty line (L636 {@code if (sbuilder.length() != 0)}).
     * The non-CJK path's unconditional {@code add(...)} at L457 CAN emit an empty
     * {@code DescriptionLine}; ART deliberately follows the CN guard, because a dropped break costs
     * nothing while a kept blank line costs a cap slot.
     */
    @Test
    public void wrapDescriptionDropsEmptyLinesCreatedByNl() {
        List<String> leading = wrapDescription("NL Deal 5 damage", 24, 5);
        assertEquals("U07b: a LEADING break must not produce an empty first line - AbstractCard "
                        + "L494 skips NL while the builder is empty; actual " + leading,
                Arrays.asList("Deal 5 damage"), leading);

        List<String> trailing = wrapDescription("Deal 5 damage NL", 24, 5);
        assertEquals("U07b: a TRAILING break must not produce an empty last line - AbstractCard "
                        + "L636 only flushes a non-empty builder; actual " + trailing,
                Arrays.asList("Deal 5 damage"), trailing);

        List<String> doubled = wrapDescription("Deal NL NL damage", 24, 5);
        assertEquals("U07b: the real two-break form is the space-separated \"NL NL\"; the empty "
                        + "middle line is DROPPED (policy is DROP, not KEEP), so only \"Deal\" and "
                        + "\"damage\" remain; actual " + doubled,
                Arrays.asList("Deal", "damage"), doubled);

        List<String> midLine = wrapDescription("Deal NL damage", 24, 5);
        assertEquals("U07b: a break between two words yields exactly those two lines and nothing "
                        + "else; actual " + midLine,
                Arrays.asList("Deal", "damage"), midLine);

        List<String> allBreaks = wrapDescription("NL NL NL", 24, 5);
        assertTrue("U07b: an all-break description yields an EMPTY list, never a list of empty "
                        + "strings (a blank line would consume a DESC_MAX_LINES slot); actual "
                        + allBreaks,
                allBreaks.isEmpty());

        for (List<String> lines : Arrays.asList(leading, trailing, doubled, midLine)) {
            for (String line : lines) {
                assertFalse("U07b: a blank-string line is forbidden in the result; actual " + lines,
                        line.isEmpty());
            }
        }
    }

    /**
     * U07b: the break MUST also work in the CJK path, not only the word-wrap path - that is the
     * whole point of the slice.
     *
     * <p>Native routes CJK text through {@code initializeDescriptionCN()} rather than
     * {@code initializeDescription()}, because {@code Settings.setLanguage} sets
     * {@code lineBreakViaCharacter=true} for ZHS/ZHT/JPN; that is also why the CN {@code NL} guard
     * (AbstractCard L494 {@code sbuilder.length() == 0} skip, L598 break on a non-empty builder,
     * L636 non-empty final flush) is the authority for the empty-line DROP policy. The real D1
     * Chinese device data is exactly this shape:
     * {@code "不能被打出 。 NL 虚无 。 NL 不能从牌组中移除。"} and
     * {@code "造成 !D! 点伤害。 NL 给予 !M! 层 易伤 。"}. An implementation that special-cases
     * {@code NL} only in the non-CJK word wrap would leave the actual device defect unfixed, so
     * these assertions are non-negotiable.
     *
     * <p>Whitespace collapse stays WITHIN each break segment (native CN collapses per segment too),
     * and the by-code-point cut runs per segment at {@code maxCharsPerLine}. This does NOT alter
     * NL-free CJK behaviour: with no break there is exactly ONE segment, so "collapse/cut per
     * segment" is observationally identical to today's "collapse/cut globally" - which is why the
     * existing {@code "一二 三" -> ["一二三"]} assertion still holds.
     */
    @Test
    public void wrapDescriptionHonoursNlInCjkDescriptions() {
        List<String> segment = wrapDescription("不能被打出 。 NL 虚无 。", 24, 5);
        assertEquals("U07b: CJK mode must honour the NL break and still collapse whitespace WITHIN "
                        + "each segment (the space before 。 disappears); actual " + segment,
                Arrays.asList("不能被打出。", "虚无。"), segment);
        for (String line : segment) {
            assertFalse("U07b: in CJK mode the two literal characters \"NL\" must not be emitted "
                            + "either; actual line \"" + line + "\"",
                    line.contains("NL"));
        }

        assertEquals("U07b: a break must not disable the by-code-point cut INSIDE a segment - "
                        + "segment 1 (6 chars at width 3) = 2 lines, segment 2 (4 chars at width 3) "
                        + "= 2 lines, total 4 <= maxLines 5; actual "
                        + wrapDescription("一二三四五六 NL 七八九十", 3, 5),
                Arrays.asList("一二三", "四五六", "七八九", "十"),
                wrapDescription("一二三四五六 NL 七八九十", 3, 5));

        List<String> device = wrapDescription("不能被打出 。 NL 虚无 。 NL 不能从牌组中移除。", 24, 5);
        assertEquals("U07b: the REAL two-break device form keeps BOTH breaks, drops nothing "
                        + "between them, and each segment collapses to well under 24 chars, so "
                        + "exactly 3 lines; actual " + device,
                Arrays.asList("不能被打出。", "虚无。", "不能从牌组中移除。"), device);
        for (String line : device) {
            assertFalse("U07b: no literal \"NL\" may survive in the real device description; "
                            + "actual line \"" + line + "\"",
                    line.contains("NL"));
        }

        assertEquals("U07b: the line cap applies to CJK break segments exactly as to English ones "
                        + "(maxLines=2 keeps 甲, 乙 and drops 丙); actual "
                        + wrapDescription("甲 NL 乙 NL 丙", 24, 2),
                Arrays.asList("甲", "乙"),
                wrapDescription("甲 NL 乙 NL 丙", 24, 2));
    }

    /**
     * U07b: a forced break must NOT bypass {@code maxLines}. Truncation stays the rule - lines past
     * the cap are DROPPED, never ellipsised - so a description made only of breaks cannot grow the
     * card text past {@code DESC_MAX_LINES}.
     */
    @Test
    public void wrapDescriptionNlRespectsTheLineCap() {
        List<String> capped = wrapDescription("a NL b NL c", 24, 2);

        assertEquals("U07b: maxLines=2 with two forced breaks must still yield exactly 2 lines "
                        + "(forced breaks do not bypass the cap); actual " + capped,
                2, capped.size());
        assertEquals("U07b: truncation keeps the FIRST lines in order (line 0 = \"a\")",
                "a", capped.get(0));
        assertEquals("U07b: truncation keeps the FIRST lines in order (line 1 = \"b\")",
                "b", capped.get(1));
        for (String line : capped) {
            assertFalse("U07b: truncation is not an ellipsis - no line may end with \"...\"/\"…\"",
                    line.endsWith("...") || line.endsWith("…"));
        }

        assertEquals("U07b: maxLines=1 keeps only the first break segment; actual "
                        + wrapDescription("a NL b NL c", 24, 1),
                Arrays.asList("a"), wrapDescription("a NL b NL c", 24, 1));
    }

    /**
     * U07b regression guard: descriptions WITHOUT an {@code NL} token must wrap EXACTLY as U07 did.
     * The last three cases matter most: they pin the existing null/blank/whitespace fail-open AND
     * the CJK whitespace collapse, so an implementation cannot "fix" NL by splitting the raw text
     * into segments first and wrapping each segment independently (that would change NL-free CJK
     * collapse and the blank-input contract).
     */
    @Test
    public void wrapDescriptionWithoutNlIsUnchangedByU07b() {
        assertEquals("U07b: English word wrap stays greedy on spaces at 15 chars",
                Arrays.asList("Deal 5 damage.", "Apply 1 Weak."),
                wrapDescription("Deal 5 damage. Apply 1 Weak.", 15, 5));

        assertEquals("U07b: CJK by-character wrap is unchanged (10 chars at 4 = 4 + 4 + 2)",
                Arrays.asList("一二三四", "五六七八", "九十"),
                wrapDescription("一二三四五六七八九十", 4, 5));

        assertEquals("U07b: CJK whitespace collapse stays GLOBAL across the whole description - "
                        + "\"一二 三\" is \"一二三\", not per-segment output; actual "
                        + wrapDescription("一二 三", 5, 5),
                Arrays.asList("一二三"), wrapDescription("一二 三", 5, 5));

        assertEquals("U07b: the truncation cap still drops the tail",
                Arrays.asList("aaa", "bbb"),
                wrapDescription("aaa bbb ccc ddd eee", 5, 2));

        assertTrue("U07b: null description still yields an EMPTY list",
                wrapDescription(null, 24, 5).isEmpty());
        assertTrue("U07b: empty description still yields an EMPTY list",
                wrapDescription("", 24, 5).isEmpty());
        assertTrue("U07b: whitespace-only description still yields an EMPTY list",
                wrapDescription("   \n\t ", 24, 5).isEmpty());
        assertTrue("U07b: a break-only/whitespace description yields an EMPTY list, not [\"\"]",
                wrapDescription("   NL   ", 24, 5).isEmpty());
    }

    /**
     * Fail-open of the PURE wrapper: blank/null/whitespace-only raw text yields NO lines, and a
     * non-positive {@code maxCharsPerLine} or {@code maxLines} must not throw (the accepted contract
     * only requires fail-open, so the observed degradation is deliberately not pinned beyond
     * "returns a list").
     */
    @Test
    public void wrapDescriptionFailsOpenOnBlankInputAndNonPositiveArguments() {
        assertTrue("U07: null description -> EMPTY list (no draw, no fabricated text)",
                wrapDescription(null, 24, 5).isEmpty());
        assertTrue("U07: empty description -> EMPTY list",
                wrapDescription("", 24, 5).isEmpty());
        assertTrue("U07: whitespace-only description -> EMPTY list",
                wrapDescription("   \n\t ", 24, 5).isEmpty());

        for (int[] bad : new int[][] {{0, 5}, {-1, 5}, {24, 0}, {24, -1}, {-1, -1}}) {
            List<String> lines = wrapDescription("Deal 5 damage", bad[0], bad[1]);
            assertNotNull("U07: wrapDescription(raw, maxCharsPerLine=" + bad[0] + ", maxLines="
                    + bad[1] + ") must not throw and must return a list (fail-open)", lines);
        }
        assertTrue("U07: the same raw text still wraps normally for positive limits - so the "
                        + "non-positive cases above really exercised the fail-open path",
                !wrapDescription("Deal 5 damage", 24, 5).isEmpty());
    }

    // ============================================================ 1b. U07c dynamic markers

    /**
     * U07c: the REAL D1 device data carries the native dynamic-value tokens {@code !D!}, {@code !B!}
     * and {@code !M!} ({@code debug-artifacts/art-verify/d1_verify_background.json}:
     * {@code "造成 !D! 点伤害。"}, {@code "获得 !B! 点 格挡 。"},
     * {@code "造成 !D! 点伤害。 NL 给予 !M! 层 易伤 。"}). Native {@code initializeDescriptionCN}
     * substitutes those tokens at wrap/prep time - AbstractCard L559 {@code if (word.equals("!D!"))},
     * L572 {@code if (word.equals("!B!"))}, L585 {@code if (word.equals("!M!"))} - so they must never
     * be painted literally on the card face.
     *
     * <p>U07c deliberately does NOT invent a damage/block/magic number: the fixed value-free
     * placeholder {@link SelectDrawPath#DYNAMIC_VALUE_PLACEHOLDER} stands in for the native
     * single-character value SLOT; the LIVE number is the separate U07d slice.
     */
    @Test
    public void wrapDescriptionSubstitutesRealDynamicMarkersInDeviceStrings() {
        List<String> attack = wrapDescription("\u9020\u6210 !D! \u70b9\u4f24\u5bb3\u3002", 24, 5);
        assertEquals("U07c: the REAL D1 Bash description must not paint the raw native dynamic "
                        + "token !D! (AbstractCard 559 word.equals(\"!D!\")) as literal text; the "
                        + "CJK path collapses whitespace, so the whole line is "
                        + "\u9020\u6210?\u70b9\u4f24\u5bb3\u3002; actual " + attack,
                Arrays.asList("\u9020\u6210?\u70b9\u4f24\u5bb3\u3002"), attack);
        assertNoRawDynamicMarker(attack);

        List<String> defend = wrapDescription(
                "\u83b7\u5f97 !B! \u70b9 \u683c\u6321 \u3002", 24, 5);
        assertEquals("U07c: the REAL D1 Defend_R description must not paint the raw native dynamic "
                        + "token !B! (AbstractCard 572 word.equals(\"!B!\")); CJK whitespace "
                        + "collapses to \u83b7\u5f97?\u70b9\u683c\u6321\u3002; actual " + defend,
                Arrays.asList("\u83b7\u5f97?\u70b9\u683c\u6321\u3002"), defend);
        assertNoRawDynamicMarker(defend);

        List<String> bash = wrapDescription(
                "\u9020\u6210 !D! \u70b9\u4f24\u5bb3\u3002 NL \u7ed9\u4e88 !M! \u5c42 "
                        + "\u6613\u4f24 \u3002", 24, 5);
        assertEquals("U07c: the REAL two-token Bash description must keep the U07b NL forced break "
                        + "(AbstractCard 494/598) while substituting !D! (L559) and !M! (L585); "
                        + "actual " + bash,
                Arrays.asList("\u9020\u6210?\u70b9\u4f24\u5bb3\u3002",
                        "\u7ed9\u4e88?\u5c42\u6613\u4f24\u3002"),
                bash);
        assertNoRawDynamicMarker(bash);

        List<String> english = wrapDescription(
                "Deal !D! damage NL Gain !B! Block.", 24, 5);
        assertEquals("U07c: substitution also applies to non-CJK text and must not disturb the "
                        + "U07b NL break (\"Deal ? damage\" is 13 chars, well under 24, so it stays "
                        + "one line); actual " + english,
                Arrays.asList("Deal ? damage", "Gain ? Block."), english);
        assertNoRawDynamicMarker(english);
    }

    /**
     * U07c: native substitution is EXACT WHOLE-TOKEN EQUALITY on one space-split token
     * (AbstractCard 559/572/585), not an {@code indexOf}/{@code replace} substring pass. A token
     * that merely CONTAINS a marker ({@code !DX!}, {@code !!D!}, {@code x!D!}), a different casing
     * ({@code !d!}), or one token that is neither ({@code !D!!M!}) stays literal. Surrounding
     * whitespace of a real token is preserved by the pure substitution step.
     */
    @Test
    public void wrapDescriptionSubstitutesOnlyWholeDynamicMarkerTokens() {
        assertEquals("U07c: a real whole token IS substituted during the wrap, and its neighbours "
                        + "survive intact - so the near-miss checks below really pin token EQUALITY "
                        + "rather than a blanket no-op",
                Arrays.asList("x ? y"), wrapDescription("x !D! y", 24, 5));

        List<String> nearMisses = wrapDescription("!DX! !!D! x!D! !d!", 24, 5);
        assertEquals("U07c: substring-looking tokens are NOT native dynamic slots - only a token "
                        + "EQUAL to !D!/!B!/!M! (AbstractCard 559/572/585) is substituted; actual "
                        + nearMisses,
                Arrays.asList("!DX! !!D! x!D! !d!"), nearMisses);

        assertEquals("U07c: \"!DX!\" is not the native !D! token and stays literal",
                "!DX!", substituteDynamicMarkers("!DX!"));
        assertEquals("U07c: \"!!D!\" is one token that is not equal to !D! and stays literal",
                "!!D!", substituteDynamicMarkers("!!D!"));
        assertEquals("U07c: \"x!D!\" is one token that is not equal to !D! and stays literal",
                "x!D!", substituteDynamicMarkers("x!D!"));
        assertEquals("U07c: the native match is CASE-SENSITIVE - \"!d!\" stays literal",
                "!d!", substituteDynamicMarkers("!d!"));
        assertEquals("U07c: only the TOKEN is replaced - its surrounding whitespace is preserved",
                " ? ", substituteDynamicMarkers(" !D! "));
        assertEquals("U07c: all three native tokens are substituted (D damage, B block, M magic)",
                "? ? ?", substituteDynamicMarkers("!D! !B! !M!"));
        assertEquals("U07c: \"!D!!M!\" is ONE token that equals neither marker and stays literal",
                "!D!!M!", substituteDynamicMarkers("!D!!M!"));
    }

    /**
     * U07c: the placeholder is a fixed, ASCII, value-free single character slot: the SUBSTITUTED
     * VALUE it produces contains NO digit (never an invented damage/block/magic number), while
     * legitimate keyword digits such as {@code *5*} are card CONTENT and must survive. The existing
     * U07 keyword toggle ({@code '*'} toggles gold and is stripped) and U07b {@code NL} forced break
     * both keep working in the SAME string as a substituted marker.
     *
     * <p><b>Strict whole-token equality (parent ruling).</b> Native tests
     * {@code word.equals("!D!")} on ONE space-split token (AbstractCard 559; likewise 572 {@code
     * !B!} and 585 {@code !M!}), so a star-wrapped {@code *!D!*} is ONE token that does NOT equal
     * {@code !D!} and stays LITERAL - substitution must never degrade to
     * {@code replace("!D!", "?")}. A BARE whole {@code !D!} next to a keyword span IS substituted,
     * while that span keeps its stars for the tokeniser.
     */
    @Test
    public void wrapDescriptionKeepsKeywordToggleAndNlBreakAroundSubstitutedMarkers() {
        List<String> starWrapped = wrapDescription("Deal *!D!* damage NL Exhaust.", 24, 5);
        assertEquals("U07c: a star-wrapped marker is ONE token that does NOT equal !D! (native "
                        + "word.equals(\"!D!\"), AbstractCard 559/572/585), so it stays LITERAL "
                        + "exactly like !DX!/!!D!/x!D! - substitution must not become a substring "
                        + "replace; the U07b NL break still fires; actual " + starWrapped,
                Arrays.asList("Deal *!D!* damage", "Exhaust."), starWrapped);

        List<String> substituted = wrapDescription("Deal *5* damage !D! NL Exhaust.", 24, 5);
        assertEquals("U07c: a BARE whole !D! token IS substituted, the U07 '*' keyword span keeps "
                        + "its stars for the tokeniser, and the U07b NL forced break still fires; "
                        + "actual " + substituted,
                Arrays.asList("Deal *5* damage ?", "Exhaust."), substituted);

        assertEquals("U07c: DYNAMIC_VALUE_PLACEHOLDER is the fixed single ASCII '?' slot (not a "
                        + "number, not a '!'-delimited token)",
                "?", dynamicValuePlaceholder());
        assertFalse("U07c: the placeholder must not be a digit - it carries NO invented "
                        + "damage/block/magic value; actual \"" + dynamicValuePlaceholder() + "\"",
                Character.isDigit(dynamicValuePlaceholder().charAt(0)));

        for (String token : new String[] {"!D!", "!B!", "!M!"}) {
            String value = substituteDynamicMarkers(token);
            assertEquals("U07c: the whole native dynamic token " + token + " (AbstractCard "
                            + "559/572/585 word.equals) is replaced by the value-free placeholder; "
                            + "actual \"" + value + "\"",
                    dynamicValuePlaceholder(), value);
            for (int i = 0; i < value.length(); i++) {
                assertFalse("U07c: the SUBSTITUTED value slot carries NO invented number - "
                                + "\"" + token + "\" becomes \"" + value + "\", which must contain "
                                + "no digit",
                        Character.isDigit(value.charAt(i)));
            }
        }

        assertFalse("U07c: the substituted line must not carry a raw native dynamic marker; actual "
                        + substituted, containsRawDynamicMarker(substituted.get(0)));
        assertChunks("Deal *5* damage ?", new String[] {"Deal ", "5", " damage ?"},
                new boolean[] {false, true, false});
        assertTrue("U07c: keyword digits are CARD CONTENT, not dynamic-value substitutions - the "
                        + "U07 '*' span keeps its \"5\" (only a native !D!/!B!/!M! slot becomes the "
                        + "value-free placeholder)",
                "Deal *5* damage ?".contains("5"));
    }

    /**
     * U07c: substitution runs BEFORE wrapping, so the ONE-character placeholder participates in the
     * wrap width. At {@code maxCharsPerLine = 8} the RAW token would fit ({@code "Deal !D!"} is
     * exactly 8 characters); the substituted form is {@code "Deal ?"} (6) and pushes
     * {@code "damage"} to line 1. The pure step stays fail-open: null/empty/blank are unchanged and
     * never throw, and the existing null-description wrap contract is untouched.
     */
    @Test
    public void wrapDescriptionPlaceholderParticipatesInWrapWidthAndFailsOpen() {
        List<String> narrow = wrapDescription("Deal !D! damage", 8, 5);
        assertEquals("U07c: the placeholder must participate in the wrap width BEFORE wrapping - at "
                        + "8 chars the raw \"Deal !D!\" (8) would fit, but \"Deal ?\" (6) + "
                        + "\"damage\" does not (13), so the result is 2 lines; actual " + narrow,
                Arrays.asList("Deal ?", "damage"), narrow);

        // U07c-R02 (review): the width-8 case above cannot distinguish PRE-wrap from POST-wrap
        // substitution (both give ["Deal ?","damage"]). At width 14 the two differ: pre-wrap sees the
        // 1-char placeholder -> "Deal ? damage" is 13 chars and FITS one line; post-wrap would first
        // wrap the 4-char raw token and "... !D! ..." is 15 chars, so it would split. One line is
        // therefore proof the substitution happens BEFORE wrapping.
        List<String> pair = wrapDescription("Deal !D! damage", 14, 5);
        assertEquals("U07c: substitution must run BEFORE wrapping - at 14 chars \"Deal ? damage\" "
                        + "(13) fits one line, whereas wrapping the raw 4-char token (15) would split; "
                        + "actual " + pair,
                Arrays.asList("Deal ? damage"), pair);

        assertNull("U07c: fail-open - null stays null", substituteDynamicMarkers(null));
        assertEquals("U07c: fail-open - the empty string stays empty", "",
                substituteDynamicMarkers(""));
        assertEquals("U07c: fail-open - whitespace-only text is unchanged (no token, no trim)",
                "   ", substituteDynamicMarkers("   "));
        assertTrue("U07c: a null description still yields an EMPTY line list",
                wrapDescription(null, 24, 5).isEmpty());
    }

    // ============================================================ 2. PURE tokenise

    /**
     * Native keyword colouring (AbstractCard 1567/1576 gold, 1648 text): a chunk beginning with
     * {@code '*'} is drawn in {@code goldColor} with the star stripped. ART's accepted contract uses
     * TOGGLE semantics - each {@code '*'}' toggles keyword mode and is not emitted, so
     * {@code "Deal *5* damage"} = normal {@code "Deal "}, keyword {@code "5"}, normal {@code " "}.
     */
    @Test
    public void tokenizeDescriptionLineTogglesKeywordModeOnAsterisks() {
        assertChunks("Deal *5* damage",
                new String[] {"Deal ", "5", " damage"},
                new boolean[] {false, true, false});

        assertChunks("Deal 5 damage",
                new String[] {"Deal 5 damage"},
                new boolean[] {false});

        assertChunks("Deal *5 damage",
                new String[] {"Deal ", "5 damage"},
                new boolean[] {false, true});
    }

    // ============================================================ 3. DrawItem geometry

    /**
     * The projected select card must carry the RAW description plus the native description geometry
     * converted through the uniform ART scale. Numbers for the real 250-wide select item:
     * <pre>
     *   descScale      = 250 / 512                        = 0.48828125
     *   anchor (page)  = -(420/2 - 107.1)                 = -102.9
     *   descAnchorY    = -102.9 * 0.48828125              = -50.244140625
     *   page -> px     = 420 * 0.48828125 = 205.078125 ; 107.1 * 0.48828125 = 52.294921875
     * </pre>
     * {@code capHeight} below is a TEST INPUT (10f), never a claimed font metric: the live metric is
     * {@code font.getCapHeight()} read after {@code setScale(descScale)}, and must not be hardcoded.
     */
    @Test
    public void drawItemCarriesDescriptionAndNativeDescriptionGeometry() {
        SelectDrawPath.DrawItem item = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("打击")
                        .type("ATTACK")
                        .color("RED")
                        .description("Deal *5* damage.")
                        .build());

        assertEquals("U07: DESC_IMG_HEIGHT must stay the native 420f page-unit constant",
                420f, floatConstant("DESC_IMG_HEIGHT"), 1e-4f);
        assertEquals("U07: DESC_OFFSET_Y_RATIO must stay 0.255f (BIG_TEXT_MODE == false, "
                        + "AbstractCard 2636); DESC_OFFSET_Y = 420 * 0.255 = 107.1 page units",
                0.255f, floatConstant("DESC_OFFSET_Y_RATIO"), 1e-4f);
        assertEquals("U07: DESC_LINE_ADVANCE_FACTOR mirrors AbstractCard 1563's 1.45f",
                1.45f, floatConstant("DESC_LINE_ADVANCE_FACTOR"), 1e-4f);
        assertEquals("U07: DESC_LINE_COUNT_FACTOR mirrors AbstractCard 1562's 0.775f",
                0.775f, floatConstant("DESC_LINE_COUNT_FACTOR"), 1e-4f);
        assertEquals("U07: DESC_LINE_COUNT_OFFSET mirrors AbstractCard 1562's 0.375f",
                0.375f, floatConstant("DESC_LINE_COUNT_OFFSET"), 1e-4f);
        assertEquals("U07: DESC_CHUNK_NUDGE_Y mirrors the native per-chunk -6f screen-space nudge "
                        + "(AbstractCard 1576/1648)",
                -6f, floatConstant("DESC_CHUNK_NUDGE_Y"), 1e-4f);
        assertEquals("U07: DESC_MAX_LINES is the documented truncation cap (5)",
                5, intConstant("DESC_MAX_LINES"));
        assertEquals("U07: DESC_MAX_CHARS_PER_LINE is the deterministic ART wrap width (24)",
                24, intConstant("DESC_MAX_CHARS_PER_LINE"));

        assertEquals("U07: DrawItem.description must carry the RAW projected description VERBATIM "
                        + "(no trim, no keyword stripping, no substitution)",
                "Deal *5* damage.",
                stringField(SelectDrawPath.DrawItem.class, item, "description"));
        assertTrue("U07: a non-blank description must be descriptionDrawable",
                boolField(SelectDrawPath.DrawItem.class, item, "descriptionDrawable"));

        assertEquals("U07: descScale = item.w / 512 = 250/512 = 0.48828125 for the 250x350 "
                        + "select item (the SAME uniform scale as costScale)",
                UNIFORM_SCALE,
                floatField(SelectDrawPath.DrawItem.class, item, "descScale"), 1e-4f);
        assertEquals("U07: descAnchorY = -(420/2 - 107.1) * (250/512) = -102.9 * 0.48828125 "
                        + "= -50.244140625 px from the card centre (page 420/205.078125 px and "
                        + "offset 107.1/52.294921875 px both come from the SAME scale)",
                DESC_ANCHOR_Y,
                floatField(SelectDrawPath.DrawItem.class, item, "descAnchorY"), 1e-4f);

        assertEquals("U07: descriptionAnchorY(centerY=500, descScale=0.48828125) = "
                        + "500 - 102.9*0.48828125 = 500 - 50.244140625 = 449.755859375",
                449.755859375f,
                descriptionAnchorY(500f, UNIFORM_SCALE), 1e-4f);
        assertEquals("U07: descriptionFirstLineY(centerY=500, descScale=0.48828125, lineCount=1, "
                        + "TEST-INPUT capHeight=10) = 500 - 50.244140625 + (0.775*1 - 0.375)*10 "
                        + "= 500 - 50.244140625 + 4 = 453.755859375 "
                        + "(capHeight is an INPUT here, not a claimed font metric)",
                453.755859375f,
                descriptionFirstLineY(500f, UNIFORM_SCALE, 1, 10f), 1e-4f);
        assertEquals("U07: descriptionLineAdvance(10) = 1.45 * 10 = 14.5 (AbstractCard 1563), so "
                        + "line index 1 sits at 453.755859375 - 14.5 = 439.255859375",
                14.5f, descriptionLineAdvance(10f), 1e-4f);
        assertEquals("U07: line index 1 y = firstLineY - 1 * advance = 439.255859375",
                439.255859375f,
                descriptionFirstLineY(500f, UNIFORM_SCALE, 1, 10f) - descriptionLineAdvance(10f),
                1e-4f);
    }

    /**
     * Native {@code getDescFont} (AbstractCard 1835-1839) selects {@code cardDescFont_N} only when
     * {@code angle == 0.0f && drawScale == 1.0f}; ART mirrors it as
     * {@code rotation == 0f && poseScale == 1f}. Both fonts are {@code prepFont(24.0f, ...)}
     * (FontHelper 205-206) and differ only in texture filtering, so this test pins the PREDICATE,
     * never a visual difference.
     */
    @Test
    public void descriptionFontChoiceMirrorsTheNativePredicate() {
        SelectDrawPath.DrawItem upright = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("打击")
                        .description("Deal 5 damage.")
                        .build());

        assertEquals("U07: poseScale defaults to 1f when the projection has no CardPose",
                1f, floatField(SelectDrawPath.DrawItem.class, upright, "poseScale"), 1e-4f);
        assertTrue("U07: descFontNormal = (rotation == 0 && poseScale == 1) -> "
                        + "cardDescFont_N for an upright, unposed card",
                boolField(SelectDrawPath.DrawItem.class, upright, "descFontNormal"));

        SelectDrawPath.DrawItem tilted = firstItem(
                CardView.builder(new CardRef("g2", "Bash"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("重击")
                        .description("Deal 5 damage.")
                        .pose(new CardPose(100f, 200f, 0.25f, 1f, 0f, true))
                        .build());
        assertEquals("U07: poseScale must carry the projected CardPose.scale (1f)",
                1f, floatField(SelectDrawPath.DrawItem.class, tilted, "poseScale"), 1e-4f);
        assertFalse("U07: a rotated card (rotation 0.25) must use cardDescFont_L - "
                        + "descFontNormal = false",
                boolField(SelectDrawPath.DrawItem.class, tilted, "descFontNormal"));

        SelectDrawPath.DrawItem scaled = firstItem(
                CardView.builder(new CardRef("g3", "Defend_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("防御")
                        .description("Deal 5 damage.")
                        .pose(new CardPose(100f, 200f, 0f, 0.8f, 0f, true))
                        .build());
        assertEquals("U07: poseScale must carry the projected CardPose.scale (0.8f)",
                0.8f, floatField(SelectDrawPath.DrawItem.class, scaled, "poseScale"), 1e-4f);
        assertFalse("U07: a scaled pose (poseScale 0.8 != 1) must use cardDescFont_L even at "
                        + "rotation 0 - descFontNormal = false",
                boolField(SelectDrawPath.DrawItem.class, scaled, "descFontNormal"));
    }

    /**
     * Fail-open of the carrier and the confirm pseudo-item: a blank description draws nothing, and
     * the confirm button (not a card) never carries a drawable description.
     */
    @Test
    public void blankDescriptionDrawsNothingAndConfirmItemHasNone() {
        SelectDrawPath.DrawItem whitespace = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("打击")
                        .description("   ")
                        .build());

        assertEquals("U07: DrawItem.description must carry the projected value VERBATIM "
                        + "(whitespace kept, no trim)",
                "   ", stringField(SelectDrawPath.DrawItem.class, whitespace, "description"));
        assertFalse("U07: a whitespace-only description must not be descriptionDrawable "
                        + "(fail-open: no draw)",
                boolField(SelectDrawPath.DrawItem.class, whitespace, "descriptionDrawable"));

        SelectDrawPath.DrawItem absent = firstItem(
                CardView.builder(new CardRef("g2", "Defend_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("防御")
                        .build());
        assertEquals("U07: an absent description fails open to the empty string",
                "", stringField(SelectDrawPath.DrawItem.class, absent, "description"));
        assertFalse("U07: an absent description must not be descriptionDrawable",
                boolField(SelectDrawPath.DrawItem.class, absent, "descriptionDrawable"));

        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .title("打击")
                                        .description("Deal 5 damage.")
                                        .build()),
                        Collections.<String>emptyList(),
                        true,
                        true));

        SelectDrawPath.DrawItem confirm = SelectDrawPath.confirmItem();
        assertNotNull("U07: a visible confirm surface still yields a confirm DrawItem", confirm);
        assertFalse("U07: the confirm button is not a card and must not draw a card description",
                boolField(SelectDrawPath.DrawItem.class, confirm, "descriptionDrawable"));
        assertEquals("U07: the confirm item carries no description text",
                "", stringField(SelectDrawPath.DrawItem.class, confirm, "description"));
    }

    /**
     * {@code toMap()} evidence must be ADDITIVE: the six new keys appear and no pre-existing key is
     * renamed or dropped (U06 title keys and the U02d3 cost keys stay present).
     */
    @Test
    public void toMapAddsDescriptionKeysWithoutRenamingExistingOnes() {
        SelectDrawPath.DrawItem item = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("打击")
                        .description(" Deal *5* damage. ")
                        .build());
        Map<String, Object> map = item.toMap();

        for (String key : Arrays.asList("instanceId", "cardId", "label", "slot", "w", "h",
                "costScale", "costOffsetX", "costOffsetY", "title", "titleScale", "titleOffsetY",
                "titleDrawable", "rotation")) {
            assertTrue("U07: toMap() must keep the pre-existing key \"" + key
                    + "\" - the description keys are ADDITIVE only", map.containsKey(key));
        }

        for (String key : Arrays.asList("description", "descriptionDrawable", "descScale",
                "descAnchorY", "descFontNormal", "descMaxLines")) {
            assertTrue("U07: toMap() must add the \"" + key + "\" evidence key",
                    map.containsKey(key));
        }

        assertEquals("U07: toMap().description must be the RAW text verbatim (no trim)",
                " Deal *5* damage. ", map.get("description"));
        assertEquals("U07: toMap().descriptionDrawable reflects the non-blank description",
                Boolean.TRUE, map.get("descriptionDrawable"));
        assertEquals("U07: toMap().descScale = 250/512 = 0.48828125",
                UNIFORM_SCALE, ((Number) map.get("descScale")).floatValue(), 1e-4f);
        assertEquals("U07: toMap().descAnchorY = -102.9 * 0.48828125 = -50.244140625",
                DESC_ANCHOR_Y, ((Number) map.get("descAnchorY")).floatValue(), 1e-4f);
        assertEquals("U07: toMap().descFontNormal is the native font predicate for an unposed card",
                Boolean.TRUE, map.get("descFontNormal"));
        assertTrue("U07: toMap().descMaxLines must be numeric", map.get("descMaxLines")
                instanceof Number);
        assertEquals("U07: toMap().descMaxLines = DESC_MAX_LINES = 5",
                5, ((Number) map.get("descMaxLines")).intValue());
    }

    // ============================================================ 4. structural / order

    /**
     * Source-shape guard mirroring the U06 technique in {@code SelectCardTitleGeometryTest} (item 4
     * of the U07 handoff). Native {@code AbstractCard.renderCard} (1066-1075) runs
     * {@code renderTitle} BEFORE the description, and the accepted slice says "description AFTER the
     * title, INSIDE the card branch". This test needs no reflection: it is a real Red today because
     * {@code renderSelect} has no {@code drawSelectDescription(} call and no such helper exists.
     */
    @Test
    public void renderSelectDrawsDescriptionAfterTitleInsideCardBranch() throws Exception {
        String source = new String(
                Files.readAllBytes(Paths.get(
                        "src/main/java/artframework/sts1/render/Sts1SurfaceRenderer.java")),
                StandardCharsets.UTF_8);

        // Comments are stripped first: the surrounding style is heavily commented, so a doc note
        // mentioning "drawSelectDescription(" would otherwise satisfy the structural checks.
        String renderSelect = stripComments(methodBody(source, "private static void renderSelect("));
        int title = renderSelect.indexOf("drawSelectTitle(");
        int description = renderSelect.indexOf("drawSelectDescription(");

        assertTrue("U07: renderSelect must still draw the native card title "
                + "(drawSelectTitle(...) call); no call found", title >= 0);
        assertTrue("U07: renderSelect must draw the card DESCRIPTION "
                + "(drawSelectDescription(...) call) - without it every select card keeps an empty "
                + "lower half, the exact U07 defect", description >= 0);
        assertTrue("U07: the description must be drawn AFTER the title, mirroring native "
                        + "renderCard order renderTitle -> ... -> description (AbstractCard 1066-1075)",
                description > title);

        assertEquals("U07: renderSelect must contain exactly ONE drawSelectDescription( call",
                1, countOccurrences(renderSelect, "drawSelectDescription("));

        int confirmBranch = indexOfIgnoringWhitespace(renderSelect, "}else{");
        assertTrue("U07: the description draw must live INSIDE the card branch - after "
                        + "drawSelectTitle( and before the confirm '} else {'",
                confirmBranch >= 0 && description < confirmBranch);

        String helper = stripComments(
                methodBody(source, "private static void drawSelectDescription("));

        assertTrue("U07: drawSelectDescription must resolve the native normal desc font "
                        + "(FontHelper.cardDescFont_N)",
                helper.contains("FontHelper.cardDescFont_N"));
        assertTrue("U07: drawSelectDescription must resolve the native large desc font "
                        + "(FontHelper.cardDescFont_L)",
                helper.contains("FontHelper.cardDescFont_L"));
        assertTrue("U07: drawSelectDescription must consume the projected raw text item.description",
                helper.contains("item.description"));
        assertTrue("U07: drawSelectDescription must scale the font by item.descScale",
                helper.contains("item.descScale"));
        assertTrue("U07: drawSelectDescription must place the block with item.descAnchorY "
                        + "(the projected ART-scale anchor), not a re-derived offset",
                helper.contains("item.descAnchorY"));
        assertTrue("U07: drawSelectDescription must tokenise keyword spans with "
                        + "tokenizeDescriptionLine(",
                helper.contains("tokenizeDescriptionLine("));
        assertTrue("U07: drawSelectDescription must wrap the raw text with wrapDescription(",
                helper.contains("wrapDescription("));

        assertFalse("U07: drawSelectDescription must not use FontHelper.buttonLabelFont - that is "
                        + "the old generic UI label font, not the native card description font",
                helper.contains("buttonLabelFont"));
        assertFalse("U07: drawSelectDescription must not re-derive the 420f page height; it must "
                        + "use the DESC_* constants", helper.contains("420f"));
        assertFalse("U07: drawSelectDescription must not re-derive the 420f page height; it must "
                        + "use the DESC_* constants", helper.contains("420.0f"));
    }

    // ============================================================ helpers: projections

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

    /** Publishes a one-card grid select frame (no confirm) and returns its first draw item. */
    private SelectDrawPath.DrawItem firstItem(CardView card) {
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(card),
                        Collections.<String>emptyList(),
                        false,
                        false));
        List<SelectDrawPath.DrawItem> items = SelectDrawPath.buildFromProjection();
        assertEquals("U07 fixture: one projected pool card with no confirm yields one item",
                1, items.size());
        return items.get(0);
    }

    // ============================================================ helpers: U07 API via reflection

    @SuppressWarnings("unchecked")
    private static List<String> wrapDescription(String raw, int maxCharsPerLine, int maxLines) {
        Method m = staticMethod("wrapDescription", String.class, int.class, int.class);
        Object result = invokeStatic(m, raw, Integer.valueOf(maxCharsPerLine),
                Integer.valueOf(maxLines));
        assertNotNull("U07: SelectDrawPath.wrapDescription must never return null", result);
        return (List<String>) result;
    }

    /** U07c: the pure native dynamic-marker substitution step (must run BEFORE wrapping). */
    private static String substituteDynamicMarkers(String raw) {
        Method m = staticMethod("substituteDynamicMarkers", String.class);
        Object result = invokeStatic(m, raw);
        if (raw == null) {
            assertNull("U07c: substituteDynamicMarkers(null) must fail open to null, never throw",
                    result);
        } else if (result == null) {
            throw new AssertionError("U07c: substituteDynamicMarkers(\"" + raw
                    + "\") must never return null (fail-open string identity)");
        }
        return (String) result;
    }

    /**
     * U07c: {@code DYNAMIC_VALUE_PLACEHOLDER} is a String constant, so it cannot go through
     * {@link #constant(String)} (which asserts {@link Number}).
     */
    private static String dynamicValuePlaceholder() {
        Field field;
        try {
            try {
                field = SelectDrawPath.class.getDeclaredField("DYNAMIC_VALUE_PLACEHOLDER");
            } catch (NoSuchFieldException ignored) {
                field = SelectDrawPath.class.getField("DYNAMIC_VALUE_PLACEHOLDER");
            }
            field.setAccessible(true);
            Object value = field.get(null);
            assertTrue("U07c: SelectDrawPath.DYNAMIC_VALUE_PLACEHOLDER must be a String, got "
                    + (value == null ? "null" : value.getClass().getName()), value instanceof String);
            return (String) value;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("U07c requires the SelectDrawPath.DYNAMIC_VALUE_PLACEHOLDER "
                    + "constant (the value-free stand-in for a native !D!/!B!/!M! slot); it does "
                    + "not exist yet", e);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U07c: could not read "
                    + "SelectDrawPath.DYNAMIC_VALUE_PLACEHOLDER", e);
        }
    }

    private static boolean containsRawDynamicMarker(String line) {
        for (String marker : new String[] {"!D!", "!B!", "!M!"}) {
            if (line.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static void assertNoRawDynamicMarker(List<String> lines) {
        for (String line : lines) {
            assertFalse("U07c: acceptance criterion - the raw native dynamic tokens !D!/!B!/!M! "
                            + "(AbstractCard 559/572/585) must no longer be painted on the card "
                            + "face; actual line \"" + line + "\"",
                    containsRawDynamicMarker(line));
        }
    }

    private static void assertChunks(String line, String[] texts, boolean[] keywords) {
        Method m = staticMethod("tokenizeDescriptionLine", String.class);
        Object result = invokeStatic(m, line);
        assertNotNull("U07: tokenizeDescriptionLine(\"" + line + "\") must never return null",
                result);
        List<?> chunks = (List<?>) result;

        StringBuilder actual = new StringBuilder("[");
        for (Object chunk : chunks) {
            if (actual.length() > 1) {
                actual.append(", ");
            }
            actual.append('"').append(stringField(chunk.getClass(), chunk, "text")).append('"');
            if (boolField(chunk.getClass(), chunk, "keyword")) {
                actual.append("(keyword)");
            }
        }
        actual.append(']');

        assertEquals("U07: tokenizeDescriptionLine(\"" + line + "\") must yield " + texts.length
                + " chunk(s) but yielded " + actual, texts.length, chunks.size());
        for (int i = 0; i < texts.length; i++) {
            Object chunk = chunks.get(i);
            assertEquals("U07: tokenizeDescriptionLine(\"" + line + "\") chunk " + i
                            + " text; actual " + actual, texts[i],
                    stringField(chunk.getClass(), chunk, "text"));
            assertEquals("U07: tokenizeDescriptionLine(\"" + line + "\") chunk " + i
                            + " keyword flag (TOGGLE on '*'); actual " + actual,
                    keywords[i], boolField(chunk.getClass(), chunk, "keyword"));
        }
    }

    private static boolean isCjkText(String raw) {
        Object result = invokeStatic(staticMethod("isCjkText", String.class), raw);
        assertTrue("U07: isCjkText must return a boolean", result instanceof Boolean);
        return ((Boolean) result).booleanValue();
    }

    private static float descriptionAnchorY(float centerY, float descScale) {
        Object result = invokeStatic(staticMethod("descriptionAnchorY", float.class, float.class),
                Float.valueOf(centerY), Float.valueOf(descScale));
        assertTrue("U07: descriptionAnchorY must return a float", result instanceof Number);
        return ((Number) result).floatValue();
    }

    private static float descriptionFirstLineY(
            float centerY, float descScale, int lineCount, float capHeight) {
        Object result = invokeStatic(
                staticMethod("descriptionFirstLineY", float.class, float.class, int.class,
                        float.class),
                Float.valueOf(centerY), Float.valueOf(descScale), Integer.valueOf(lineCount),
                Float.valueOf(capHeight));
        assertTrue("U07: descriptionFirstLineY must return a float", result instanceof Number);
        return ((Number) result).floatValue();
    }

    private static float descriptionLineAdvance(float capHeight) {
        Object result = invokeStatic(staticMethod("descriptionLineAdvance", float.class),
                Float.valueOf(capHeight));
        assertTrue("U07: descriptionLineAdvance must return a float", result instanceof Number);
        return ((Number) result).floatValue();
    }

    private static Method staticMethod(String name, Class<?>... params) {
        StringBuilder signature = new StringBuilder(name).append('(');
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                signature.append(',');
            }
            signature.append(params[i].getSimpleName());
        }
        signature.append(')');
        try {
            Method m = SelectDrawPath.class.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new AssertionError("U07 requires SelectDrawPath." + signature
                    + " (native card description wrap/tokenise/geometry); it does not exist yet",
                    e);
        }
    }

    private static Object invokeStatic(Method method, Object... args) {
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw new AssertionError("U07: SelectDrawPath." + method.getName()
                    + " threw " + e.getCause(), e.getCause());
        } catch (IllegalAccessException e) {
            throw new AssertionError("U07: could not call SelectDrawPath." + method.getName(), e);
        }
    }

    private static float floatConstant(String name) {
        return constant(name).floatValue();
    }

    private static int intConstant(String name) {
        return constant(name).intValue();
    }

    /** Reads a (possibly package-private) {@code SelectDrawPath} constant by name. */
    private static Number constant(String name) {
        Field field;
        try {
            try {
                field = SelectDrawPath.class.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                field = SelectDrawPath.class.getField(name);
            }
            field.setAccessible(true);
            Object value = field.get(null);
            assertTrue("U07: SelectDrawPath." + name + " must be numeric, got "
                    + (value == null ? "null" : value.getClass().getName()), value instanceof Number);
            return (Number) value;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("U07 requires the SelectDrawPath." + name
                    + " constant (native card description geometry); it does not exist yet", e);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U07: could not read SelectDrawPath." + name, e);
        }
    }

    // ============================================================ helpers: reflection fields

    private static Field publicField(Class<?> type, String name) {
        try {
            return type.getField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("U07 requires the public field " + type.getSimpleName() + "."
                    + name + " (native select-card description); it does not exist yet", e);
        }
    }

    private static float floatField(Class<?> type, Object target, String name) {
        try {
            return publicField(type, name).getFloat(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U07: could not read " + type.getSimpleName() + "." + name, e);
        }
    }

    private static boolean boolField(Class<?> type, Object target, String name) {
        try {
            return publicField(type, name).getBoolean(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U07: could not read " + type.getSimpleName() + "." + name, e);
        }
    }

    private static String stringField(Class<?> type, Object target, String name) {
        try {
            return (String) publicField(type, name).get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U07: could not read " + type.getSimpleName() + "." + name, e);
        }
    }

    // ============================================================ helpers: source shape

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

    /** {@code indexOf} that tolerates any whitespace layout, e.g. {@code "} else {"} vs {@code "}\n else {"}. */
    private static int indexOfIgnoringWhitespace(String haystack, String compactNeedle) {
        for (int start = 0; start < haystack.length(); start++) {
            int needle = 0;
            int cursor = start;
            while (needle < compactNeedle.length() && cursor < haystack.length()) {
                char c = haystack.charAt(cursor);
                if (Character.isWhitespace(c)) {
                    cursor++;
                    continue;
                }
                if (c != compactNeedle.charAt(needle)) {
                    break;
                }
                needle++;
                cursor++;
            }
            if (needle == compactNeedle.length()) {
                return start;
            }
        }
        return -1;
    }

    /** Isolates one method body by brace matching, from its signature to the matching close. */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue("U07: Sts1SurfaceRenderer must define '" + signature + "...'", start >= 0);
        int open = source.indexOf('{', start);
        assertTrue("U07: malformed source - no body brace after '" + signature + "'", open >= 0);
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
        throw new AssertionError("U07: malformed source - unbalanced braces in '" + signature + "'");
    }
}
