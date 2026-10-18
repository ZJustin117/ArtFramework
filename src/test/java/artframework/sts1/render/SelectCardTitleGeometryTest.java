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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * U06 Red: select-screen cards must draw their TITLE like native
 * {@code AbstractCard.renderTitle} instead of the current centred
 * {@code FontHelper.buttonLabelFont} label over the middle of the card.
 *
 * <p>Accepted design: the title is anchored on the banner arch at
 * {@code centerY + 175f * drawScale}, drawn with {@code FontHelper.cardTitleFont} at
 * {@code drawScale} (times {@code 0.85f} for the native small-title flag), rotated by the card's
 * projected rotation, green when upgraded, and with the native {@code renderColor.a} alpha. It is
 * drawn AFTER the frame, portrait, banner, energy orb and cost number.
 *
 * <p><strong>Scale authority.</strong> {@code drawScale = item.w / 512f} - the uniform ART card
 * scale already used by the U02d3 cost geometry ({@code costScale}/{@code costOffsetY}), i.e. the
 * native {@code bounds.width / region.originalWidth} analogue where every {@code cardui} region has
 * {@code originalWidth 512}. Native {@code card.drawScale} is NOT the authority because it does not
 * carry ART's own card box: on the select screen the native {@code drawScale} is about 0.75, which
 * would put the title at {@code 175 * 0.75 = 131.25} px above the card centre - roughly 46 px above
 * the ART anchor of {@code 85.449} px for the real 250x350 select item, pushing the title off the
 * banner arch and toward/above the top edge of the ART card. ART's {@code w/512} scale already
 * absorbs the host {@code Settings.scale}, so the anchor needs no extra multiplier.
 *
 * <p>The U06 fields/builder setters do not exist yet, so every new member is read through a small
 * private reflection helper. That keeps this class compiling today and makes each test fail at
 * runtime with an {@link AssertionError} that names the missing U06 field or method.
 */
public class SelectCardTitleGeometryTest {

    /** Real select-card box published by {@link SelectDrawPath#buildFromProjection()}. */
    private static final float SELECT_W = 250f;
    private static final float SELECT_H = 350f;

    /** Native {@code card.drawScale} authority is ART's uniform {@code w / 512f}. */
    private static final float UNIFORM_SCALE = SELECT_W / 512f; // 0.48828125

    /** Native {@code renderTitle} anchor: {@code 175f * drawScale}. */
    private static final float TITLE_OFFSET_Y = 175f * UNIFORM_SCALE; // 85.44921875

    /** Native small-title multiplier ({@code Settings.language == EPOCH}?0.85:1). */
    private static final float SMALL_TITLE_SCALE = UNIFORM_SCALE * 0.85f; // ~0.41503906

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.clearCache();
    }

    // ------------------------------------------------------------------ behavior 1

    @Test
    public void titleAnchorAndScaleUseTheUniformCardScale() {
        SelectDrawPath.DrawItem item = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("打击")
                        .type("ATTACK")
                        .color("RED")
                        .build());

        assertEquals("U06: DrawItem.title must carry the localized card title verbatim",
                "打击", stringField(SelectDrawPath.DrawItem.class, item, "title"));
        assertEquals("U06: titleOffsetY = 175 * (w/512) = 85.44921875 for the 250x350 select item "
                        + "(NOT 175 * native card.drawScale)",
                TITLE_OFFSET_Y, floatField(SelectDrawPath.DrawItem.class, item, "titleOffsetY"),
                1e-4f);
        assertEquals("U06: titleScale = w/512 = 0.48828125 for the 250x350 select item",
                UNIFORM_SCALE, floatField(SelectDrawPath.DrawItem.class, item, "titleScale"), 1e-4f);
        assertEquals("U06: titleAlpha fail-open is 1f when the host did not project renderColor.a",
                1f, floatField(SelectDrawPath.DrawItem.class, item, "titleAlpha"), 1e-4f);
        assertTrue("U06: a non-blank localized title must be titleDrawable",
                boolField(SelectDrawPath.DrawItem.class, item, "titleDrawable"));
    }

    // ------------------------------------------------------------------ behavior 2

    @Test
    public void smallTitleFlagScalesTheFontButNotTheAnchor() {
        CardView.Builder builder = CardView.builder(new CardRef("g1", "Strike_R"))
                .zone(CardZone.SELECT)
                .slot(0)
                .title("打击")
                .type("ATTACK")
                .color("RED");
        callBuilder(builder, "smallTitleFont", boolean.class, Boolean.TRUE);
        SelectDrawPath.DrawItem item = firstItem(builder.build());

        assertEquals("U06: the native small-title flag scales the FONT by 0.85: "
                        + "titleScale = 0.48828125 * 0.85 = 0.4150390625",
                SMALL_TITLE_SCALE, floatField(SelectDrawPath.DrawItem.class, item, "titleScale"),
                1e-4f);
        assertEquals("U06: the small-title flag must NOT move the banner-arch anchor "
                        + "(titleOffsetY stays 175 * w/512 = 85.44921875)",
                TITLE_OFFSET_Y, floatField(SelectDrawPath.DrawItem.class, item, "titleOffsetY"),
                1e-4f);
    }

    // ------------------------------------------------------------------ behavior 3

    @Test
    public void upgradedTitleIsGreenAndAlphaComesFromRenderColor() {
        CardView.Builder upgraded = CardView.builder(new CardRef("g1", "Strike_R"))
                .zone(CardZone.SELECT)
                .slot(0)
                .title("打击")
                .type("ATTACK")
                .color("RED");
        callBuilder(upgraded, "upgraded", boolean.class, Boolean.TRUE);
        callBuilder(upgraded, "titleAlpha", float.class, Float.valueOf(0.5f));
        SelectDrawPath.DrawItem upgradedItem = firstItem(upgraded.build());

        assertTrue("U06: an upgraded card draws its title GREEN (native renderColor GREEN branch)",
                boolField(SelectDrawPath.DrawItem.class, upgradedItem, "titleGreen"));
        assertEquals("U06: titleAlpha must carry the native renderColor.a (0.5f here)",
                0.5f, floatField(SelectDrawPath.DrawItem.class, upgradedItem, "titleAlpha"), 1e-4f);

        SelectDrawPath.DrawItem plain = firstItem(
                CardView.builder(new CardRef("g2", "Defend_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("防御")
                        .type("SKILL")
                        .color("RED")
                        .build());

        assertFalse("U06: a plain (non-upgraded) card draws its title in the normal color",
                boolField(SelectDrawPath.DrawItem.class, plain, "titleGreen"));
        assertEquals("U06: titleAlpha fail-open is 1f for a plain card", 1f,
                floatField(SelectDrawPath.DrawItem.class, plain, "titleAlpha"), 1e-4f);
    }

    // ------------------------------------------------------------------ behavior 4

    @Test
    public void blankTitleDrawsNothingAndFabricatesNoText() {
        SelectDrawPath.DrawItem whitespace = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("   ")
                        .type("ATTACK")
                        .color("RED")
                        .build());

        assertEquals("U06: DrawItem.title must carry the projected title VERBATIM (no trim, "
                        + "no substitution)", "   ",
                stringField(SelectDrawPath.DrawItem.class, whitespace, "title"));
        assertFalse("U06: a whitespace-only title must not be titleDrawable",
                boolField(SelectDrawPath.DrawItem.class, whitespace, "titleDrawable"));
        assertFalse("U06: a blank title must never fabricate the old 'Generic' text",
                "Generic".equals(stringField(SelectDrawPath.DrawItem.class, whitespace, "title")));

        SelectDrawPath.DrawItem empty = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("")
                        .type("ATTACK")
                        .color("RED")
                        .build());

        assertFalse("U06: an empty title must not be titleDrawable",
                boolField(SelectDrawPath.DrawItem.class, empty, "titleDrawable"));
        assertEquals("U06: the pre-existing label fallback is untouched - a blank localized title "
                        + "still resolves item.label to the raw cardId", "Strike_R", empty.label);
    }

    // ------------------------------------------------------------------ behavior 5

    @Test
    public void titleRotationFollowsTheProjectedPose() {
        SelectDrawPath.DrawItem item = firstItem(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .title("打击")
                        .type("ATTACK")
                        .color("RED")
                        .pose(new CardPose(100f, 200f, 0.25f, 1f, 0f, true))
                        .build());

        assertEquals("U06: DrawItem.rotation must carry the projected CardPose.rotation (0.25f)",
                0.25f, floatField(SelectDrawPath.DrawItem.class, item, "rotation"), 1e-4f);
    }

    // ------------------------------------------------------------------ behavior 6

    @Test
    public void confirmItemHasNoTitle() {
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .title("打击")
                                        .type("ATTACK")
                                        .color("RED")
                                        .build()),
                        Collections.<String>emptyList(),
                        true,
                        true));

        SelectDrawPath.DrawItem confirm = SelectDrawPath.confirmItem();
        assertNotNull("U06: a visible confirm surface still yields a confirm DrawItem", confirm);
        assertFalse("U06: the confirm button is not a card and must not draw a card title",
                boolField(SelectDrawPath.DrawItem.class, confirm, "titleDrawable"));
        String title = stringField(SelectDrawPath.DrawItem.class, confirm, "title");
        assertTrue("U06: the confirm item carries no title text (blank/null)",
                title == null || title.trim().isEmpty());
    }

    // ------------------------------------------------------------------ behavior 7

    @Test
    public void legacyItemConstructorKeepsUnitScaleFailOpen() {
        SelectDrawPath.DrawItem item = new SelectDrawPath.DrawItem(
                "g", "Strike_R", "打击", 0, false, true, true, 0f, 0f, false,
                "ui.select.card", "", 0f, 0f);

        assertEquals("U06: the legacy 14-arg constructor carries no title geometry, so "
                        + "titleOffsetY fails open to 175f when w <= 0",
                175f, floatField(SelectDrawPath.DrawItem.class, item, "titleOffsetY"), 1e-4f);
        assertEquals("U06: the legacy 14-arg constructor's titleScale fails open to 1f when w <= 0",
                1f, floatField(SelectDrawPath.DrawItem.class, item, "titleScale"), 1e-4f);
        assertFalse("U06: the legacy 14-arg constructor carries no title, so nothing is drawable",
                boolField(SelectDrawPath.DrawItem.class, item, "titleDrawable"));
    }

    // ------------------------------------------------------------------ behavior 8

    @Test
    public void cardViewCarrierDefaultsAreFailOpen() {
        CardView builderDefault = CardView.builder(new CardRef("g1", "Strike_R"))
                .zone(CardZone.SELECT)
                .slot(0)
                .title("打击")
                .build();

        assertFalse("U06: CardView.upgraded fail-open default is false",
                boolField(CardView.class, builderDefault, "upgraded"));
        assertFalse("U06: CardView.smallTitleFont fail-open default is false",
                boolField(CardView.class, builderDefault, "smallTitleFont"));
        assertEquals("U06: CardView.titleAlpha fail-open default is 1f", 1f,
                floatField(CardView.class, builderDefault, "titleAlpha"), 1e-4f);

        CardView direct = new CardView(
                new CardRef("g2", "Defend_R"), CardZone.SELECT, 0, CardPose.at(0f, 0f),
                true, false, false, false, "", "");
        assertFalse("U06: an EXISTING CardView constructor keeps upgraded=false",
                boolField(CardView.class, direct, "upgraded"));
        assertFalse("U06: an EXISTING CardView constructor keeps smallTitleFont=false",
                boolField(CardView.class, direct, "smallTitleFont"));
        assertEquals("U06: an EXISTING CardView constructor keeps titleAlpha=1f", 1f,
                floatField(CardView.class, direct, "titleAlpha"), 1e-4f);

        CardView.Builder configured = CardView.builder(new CardRef("g3", "Bash"))
                .zone(CardZone.SELECT)
                .slot(1)
                .title("重击");
        callBuilder(configured, "upgraded", boolean.class, Boolean.TRUE);
        callBuilder(configured, "smallTitleFont", boolean.class, Boolean.TRUE);
        callBuilder(configured, "titleAlpha", float.class, Float.valueOf(0.25f));
        CardView carrier = configured.build();

        assertTrue("U06: CardView.Builder.upgraded(boolean) must round-trip to CardView.upgraded",
                boolField(CardView.class, carrier, "upgraded"));
        assertTrue("U06: CardView.Builder.smallTitleFont(boolean) must round-trip to "
                        + "CardView.smallTitleFont",
                boolField(CardView.class, carrier, "smallTitleFont"));
        assertEquals("U06: CardView.Builder.titleAlpha(float) must round-trip to CardView.titleAlpha",
                0.25f, floatField(CardView.class, carrier, "titleAlpha"), 1e-4f);
    }

    // ------------------------------------------------------------------ behavior 9

    @Test
    public void renderSelectDrawsTheNativeTitleAfterTheCardRegions() throws Exception {
        String source = new String(
                Files.readAllBytes(Paths.get(
                        "src/main/java/artframework/sts1/render/Sts1SurfaceRenderer.java")),
                StandardCharsets.UTF_8);

        String renderSelect = methodBody(source, "private static void renderSelect(");
        int portrait = renderSelect.indexOf("drawSelectPortrait(");
        int firstAtlas = renderSelect.indexOf("drawSelectAtlasRegion(");
        int lastAtlas = renderSelect.lastIndexOf("drawSelectAtlasRegion(");
        int cost = renderSelect.indexOf("drawSelectCostNumber(");
        int title = renderSelect.indexOf("drawSelectTitle(");

        assertTrue("U06: renderSelect must draw the native card title (drawSelectTitle(...) call); "
                + "today it only draws the centred buttonLabelFont item.label", title >= 0);
        assertTrue("U06: renderSelect must still draw the portrait region; no "
                + "drawSelectPortrait( call found", portrait >= 0);
        assertTrue("U06: renderSelect must still draw the cardui banner/orb regions; no "
                + "drawSelectAtlasRegion( call found", firstAtlas >= 0 && lastAtlas >= 0);
        assertTrue("U06: renderSelect must still draw the cost number; no "
                + "drawSelectCostNumber( call found", cost >= 0);
        assertTrue("U06: the base frame is drawn before the portrait", firstAtlas < portrait);
        assertTrue("U06: the portrait is drawn before the banner/orb", portrait < lastAtlas);
        assertTrue("U06: the cost number is drawn after the cardui regions", lastAtlas < cost);
        assertTrue("U06: drawSelectTitle must run AFTER frame/portrait/banner/orb/cost "
                        + "(native renderTitle runs after renderCardBg/renderPortrait/"
                        + "renderBannerImage/renderEnergy)",
                title > portrait && title > lastAtlas && title > cost);

        // U06-04: strip comments first. The surrounding style is heavily commented, so an inline
        // "// not 175f * native drawScale" style note would otherwise fail these structural checks
        // without any behaviour change.
        String helper = stripComments(methodBody(source, "private static void drawSelectTitle("));
        assertTrue("U06: drawSelectTitle must use the native card title font "
                        + "(FontHelper.cardTitleFont), not buttonLabelFont",
                helper.contains("FontHelper.cardTitleFont"));
        assertTrue("U06: drawSelectTitle must draw rotated text via renderRotatedText(",
                helper.contains("renderRotatedText("));
        assertTrue("U06: drawSelectTitle must rotate by the projected item.rotation",
                helper.contains("item.rotation"));
        assertTrue("U06: drawSelectTitle must scale the font by item.titleScale",
                helper.contains("item.titleScale"));
        // U06-03: the helper must CONSUME the projected geometry, not re-derive it from native
        // drawScale or a bare 175 constant (that would silently defeat Ruling 1).
        assertTrue("U06: drawSelectTitle must place the title with item.titleOffsetY (the projected "
                        + "ART-scale anchor), not a re-derived offset",
                helper.contains("item.titleOffsetY"));
        assertTrue("U06: drawSelectTitle must resolve the colour through titleColor(...)",
                helper.contains("titleColor("));
        assertFalse("U06: drawSelectTitle must not re-derive the anchor from a bare 175 or from "
                        + "native drawScale; the ART scale authority is the projected titleOffsetY",
                helper.contains("175f") || helper.contains("drawScale"));

        // U06-01: assert the REAL predicate, not just the identifier - a bare "titleDrawable"
        // could be satisfied by a comment above the guard.
        assertTrue("U06: drawSelectTitle must guard on !item.titleDrawable",
                helper.contains("!item.titleDrawable"));

        // U06-02: exactly ONE title may be painted. Re-adding the old centred item.label draw for
        // cards would reintroduce the U06 symptom (name across the portrait AND on the arch) while
        // every other assertion stayed green, so pin the label draw to the confirm branch only.
        assertEquals("U06: renderSelect must contain exactly one renderFontCentered( - the confirm "
                        + "button's centred label; the card title must not also be drawn centred",
                1, countOccurrences(renderSelect, "renderFontCentered("));
        int confirmBranch = indexOfIgnoringWhitespace(renderSelect, "}else{");
        int centredLabel = renderSelect.indexOf("renderFontCentered(");
        assertTrue("U06: the single centred label draw must live in the confirm (non-card) branch",
                confirmBranch >= 0 && centredLabel > confirmBranch);
        assertEquals("U06: renderSelect must contain exactly one drawSelectTitle( call",
                1, countOccurrences(renderSelect, "drawSelectTitle("));
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

    // ------------------------------------------------------------------ helpers

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
        assertEquals("U06 fixture: one projected pool card with no confirm yields one item",
                1, items.size());
        return items.get(0);
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
        assertTrue("U06: Sts1SurfaceRenderer must define '" + signature + "...'", start >= 0);
        int open = source.indexOf('{', start);
        assertTrue("U06: malformed source - no body brace after '" + signature + "'", open >= 0);
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
        throw new AssertionError("U06: malformed source - unbalanced braces in '" + signature + "'");
    }

    private static Field publicField(Class<?> type, String name) {
        try {
            return type.getField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("U06 requires the public field " + type.getSimpleName() + "."
                    + name + " (native select-card title geometry); it does not exist yet", e);
        }
    }

    private static float floatField(Class<?> type, Object target, String name) {
        try {
            return publicField(type, name).getFloat(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U06: could not read " + type.getSimpleName() + "." + name, e);
        }
    }

    private static boolean boolField(Class<?> type, Object target, String name) {
        try {
            return publicField(type, name).getBoolean(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U06: could not read " + type.getSimpleName() + "." + name, e);
        }
    }

    private static String stringField(Class<?> type, Object target, String name) {
        try {
            return (String) publicField(type, name).get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("U06: could not read " + type.getSimpleName() + "." + name, e);
        }
    }

    /** Calls a not-yet-existing {@code CardView.Builder} setter through reflection. */
    private static void callBuilder(Object builder, String name, Class<?> paramType, Object value) {
        try {
            Method method = builder.getClass().getMethod(name, paramType);
            method.invoke(builder, value);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("U06 requires the public CardView.Builder."
                    + name + "(" + paramType.getSimpleName() + ") setter; it does not exist yet", e);
        } catch (InvocationTargetException e) {
            throw new AssertionError("U06: CardView.Builder." + name + " threw", e.getCause());
        } catch (IllegalAccessException e) {
            throw new AssertionError("U06: could not call CardView.Builder." + name, e);
        }
    }
}
