package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
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
import artframework.sts1.assets.Sts1AssetMaterializer;
import artframework.sts1.assets.Sts1AtlasMaterializer;
import artframework.sts1.assets.Sts1HostAssets;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SelectDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.clearCache();
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
        assertEquals("a card without type/color maps to no cardui base-background region",
                "", items.get(0).frameResourceId);
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
        assertEquals("a disabled card without type/color maps to no cardui base-background region",
                "", item.frameResourceId);
    }

    /**
     * U01. A visible select card with no type/color maps to no cardui base-background region, and
     * its primary ({@code UI_SELECT_CARD} -> {@code cardui/frame}) is not file-backed, so the probe
     * must not count it as fully materialized: it is a gap. On current code {@code missingArt} /
     * {@code materializedResolved} are absent (null), which was the U01 Red.
     */
    @Test
    public void selectGapCountsUnresolvedCardFrameAsMissingNotSupplied() {
        Sts1HostAssets.install();
        // One visible card (selected, default playable) + the confirm button, both visible.
        publishSelectFrame();

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals("count stays the existing visible-item count",
                Integer.valueOf(2), probe.get("count"));
        assertNotNull("probe must expose missingArt", probe.get("missingArt"));
        assertTrue("the unresolved card frame must be reported as a gap: " + probe.get("missingArt"),
                ((Integer) probe.get("missingArt")).intValue() >= 1);
        assertEquals("exactly the unresolved card is a gap", Integer.valueOf(1), probe.get("missingArt"));
        assertNotNull("probe must expose materializedResolved", probe.get("materializedResolved"));
        assertEquals("only the file-backed confirm button is fully materialized",
                Integer.valueOf(1), probe.get("materializedResolved"));
        assertTrue("materializedResolved must be strictly below the supplied count",
                ((Integer) probe.get("materializedResolved")).intValue()
                        < ((Integer) probe.get("count")).intValue());

        SelectDrawPath.DrawItem card = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("a card without type/color maps to no cardui base-background region",
                "", card.frameResourceId);
        assertFalse("the card's primary cardui/frame must not be a file-backed drawable",
                Sts1AssetMaterializer.isFileBacked(
                        ArtFramework.assets().resolve(ResourceIds.UI_SELECT_CARD).source));
    }

    /**
     * U01/U02. The accounting is genuinely resolve-aware, not "always missing": a card whose primary
     * ({@code UI_SELECT_CARD_DISABLED} -> file-backed disabledButton.png) AND whose type/color-derived
     * cardui base-background frame descriptor resolve is fully materialized with no gap. The frame
     * descriptor here is {@code cardui#512/bg_attack_red}, resolved through the live atlas source.
     */
    @Test
    public void selectResolvableResourcesCountAsMaterializedNotGap() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(
                SelectView.grid(
                        Collections.singletonList(
                                CardView.builder(new CardRef("g1", "Strike_R"))
                                        .zone(CardZone.SELECT)
                                        .slot(0)
                                        .playable(false)
                                        .frame(ResourceIds.UI_SELECT_CARD_DISABLED)
                                        .type("ATTACK")
                                        .color("RED")
                                        .build()),
                        Collections.<String>emptyList(),
                        false,
                        false));

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertNotNull("probe must expose missingArt", probe.get("missingArt"));
        assertEquals("both the primary and the frame descriptor resolve",
                Integer.valueOf(0), probe.get("missingArt"));
        assertNotNull("probe must expose materializedResolved", probe.get("materializedResolved"));
        assertEquals("the single visible item is fully materialized",
                Integer.valueOf(1), probe.get("materializedResolved"));
    }

    /**
     * U01 premise (green by design): the real {@code UI_SELECT_CARD_FRAME} mapping is a known atlas
     * key that does NOT resolve to a file-backed drawable. This pins the defect the accounting must
     * surface rather than re-deriving or changing the mapping.
     */
    @Test
    public void selectCardFrameMappingIsKnownAtlasKeyNotFileBacked() {
        assertFalse("sts1:cardui/frame is an atlas key, not a loose PNG",
                Sts1AssetMaterializer.isFileBacked("sts1:cardui/frame"));
        Sts1HostAssets.install();

        AssetResolveResult frame = ArtFramework.assets().resolve(ResourceIds.UI_SELECT_CARD_FRAME);
        assertTrue("the catalog must still contain the frame key", frame.found);
        assertEquals("the frame resource normalizes to the non-file-backed cardui/frame atlas key",
                "cardui/frame", Sts1AssetMaterializer.normalize(frame.source));
    }

    /**
     * U02. The select card BASE-BACKGROUND frame is a region in the {@code cardui} atlas derived from
     * the card's type and color: {@code cardui#512/bg_<type>_<color>}. The type/color are
     * case-insensitive.
     */
    @Test
    public void selectCardFrameMapsTypeAndColorToCardUiBaseBackgroundRegion() {
        assertEquals("cardui#512/bg_attack_red",
                firstCardFrame("ATTACK", "RED"));
        assertEquals("lower-case type/color must map identically",
                "cardui#512/bg_attack_red",
                firstCardFrame("attack", "red"));
    }

    @Test
    public void selectCardFrameMappingMatrixUsesMappedColors() {
        assertEquals("cardui#512/bg_skill_green", firstCardFrame("SKILL", "GREEN"));
        assertEquals("cardui#512/bg_power_blue", firstCardFrame("POWER", "BLUE"));
        assertEquals("colorless maps to the gray base-background page",
                "cardui#512/bg_attack_gray", firstCardFrame("ATTACK", "colorless"));
        assertEquals("each family has its own colorless gray page",
                "cardui#512/bg_power_gray", firstCardFrame("POWER", "COLORLESS"));
        assertEquals("each family has its own colorless gray page",
                "cardui#512/bg_skill_gray", firstCardFrame("SKILL", "COLORLESS"));
    }

    /**
     * U02b (native {@code AbstractCard.renderCardBg}): CURSE and STATUS types switch to the SKILL
     * background family, so a CURSE-colored card draws {@code CARD_SKILL_BG_BLACK}
     * ({@code 512/bg_skill_black}) even though the type is curse/status; a COLORLESS status draws the
     * skill gray page.
     */
    @Test
    public void selectCardFrameCurseAndStatusTypesUseSkillFamilyBlackPage() {
        assertEquals("CURSE type renders the skill family, and a CURSE color is the black skill page",
                "cardui#512/bg_skill_black", firstCardFrame("CURSE", "CURSE"));
        assertEquals("STATUS type also renders the skill family",
                "cardui#512/bg_skill_black", firstCardFrame("STATUS", "CURSE"));
        assertEquals("a COLORLESS status draws the skill gray page, not the attack page",
                "cardui#512/bg_skill_gray", firstCardFrame("STATUS", "COLORLESS"));
    }

    /**
     * U02b (native {@code render*Bg} color switch default): an unmappable or absent color is the
     * native DEFAULT branch, which draws {@code CARD_SKILL_BG_BLACK} ({@code 512/bg_skill_black})
     * from every family - measured here on ATTACK/POWER/SKILL, all of which must land on the black
     * SKILL page rather than a family page or an empty descriptor.
     */
    @Test
    public void selectCardFrameUnmappedOrAbsentColorFallsToBlackSkillPage() {
        assertEquals("an unmappable ATTACK color is the native default -> black skill page",
                "cardui#512/bg_skill_black", firstCardFrame("ATTACK", "WAT"));
        assertEquals("an ABSENT ATTACK color is the native default -> black skill page",
                "cardui#512/bg_skill_black", firstCardFrame("ATTACK", ""));
        assertEquals("the black default is family-independent",
                "cardui#512/bg_skill_black", firstCardFrame("POWER", "WAT"));
        assertEquals("the black default is family-independent",
                "cardui#512/bg_skill_black", firstCardFrame("SKILL", "WAT"));
    }

    /**
     * U02b (native {@code renderCardBg} type switch with NO default case): an unknown or absent TYPE
     * draws NO background at all, so the frame descriptor stays empty (fail-open, no bogus region)
     * even when the color is otherwise mappable.
     */
    @Test
    public void selectCardFrameUnknownOrAbsentTypeHasNoFrameRegion() {
        assertEquals("an unknown type draws no background in native, so no descriptor",
                "", firstCardFrame("WAT", "RED"));
        assertEquals("a blank type draws no background in native, so no descriptor",
                "", firstCardFrame("", "RED"));
        assertEquals("an absent type draws no background in native, so no descriptor",
                "", firstCardFrame(null, "RED"));
    }

    /**
     * U02b. The native type switch renders CURSE as the SKILL family, so a CURSE card with a RED
     * color now maps to {@code cardui#512/bg_skill_red} (skill page + red), NOT the empty
     * descriptor. An absent TYPE still draws nothing (no native default case) -> {@code ""}. In both
     * cases the card's primary ({@code ui.select.card} -> non-file-backed {@code cardui/frame}) and
     * the atlas-only frame leave the item as a gap.
     */
    @Test
    public void selectCardFrameCurseTypeUsesSkillFamilyWhileAbsentTypeHasNoFrameRegionAndCountsAsGap() {
        Sts1HostAssets.install();

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("CURSE")
                        .color("RED")
                        .build()));
        SelectDrawPath.DrawItem curse = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("a CURSE type renders the skill family, so RED yields the red skill page",
                "cardui#512/bg_skill_red", curse.frameResourceId);
        assertEquals("the atlas-only skill frame is unresolved without a live atlas, so it is a gap",
                Integer.valueOf(1), SelectDrawPath.probeSlice().get("missingArt"));
        assertEquals("nothing is fully materialized while the frame is unresolved",
                Integer.valueOf(0), SelectDrawPath.probeSlice().get("materializedResolved"));

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g2", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .build()));
        SelectDrawPath.DrawItem noType = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("an absent type must produce no frame descriptor",
                "", noType.frameResourceId);
        assertTrue("the absent-type card must count as a gap: "
                        + SelectDrawPath.probeSlice().get("missingArt"),
                ((Integer) SelectDrawPath.probeSlice().get("missingArt")).intValue() >= 1);
    }

    /**
     * U02. With a live {@code cardui} atlas source containing {@code 512/bg_attack_red}, the
     * type/color-derived frame descriptor resolves through the production resolution path and the
     * item is fully materialized (its file-backed disabled primary resolves too).
     */
    @Test
    public void selectCardFrameDerivedRegionResolvesThroughLiveAtlas() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .playable(false)
                        .type("ATTACK")
                        .color("RED")
                        .build()));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("cardui#512/bg_attack_red", item.frameResourceId);
        assertNotNull("the derived region must resolve through the live atlas seam",
                Sts1AtlasMaterializer.regionFromAtlas("cardui", "512/bg_attack_red"));

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertEquals("the resolved frame descriptor leaves no gap",
                Integer.valueOf(0), probe.get("missingArt"));
        assertEquals("the single visible item is fully materialized",
                Integer.valueOf(1), probe.get("materializedResolved"));
    }

    /**
     * U02 fail-open. Without a live atlas (or without the region), the derived frame descriptor does
     * NOT resolve, so the item is reported as a gap rather than silently counted as materialized.
     */
    @Test
    public void selectCardFrameDerivedRegionUnresolvedCountsAsGap() {
        Sts1HostAssets.install();
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .playable(false)
                        .type("ATTACK")
                        .color("RED")
                        .build()));

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals("the derived frame descriptor is the cardui region",
                "cardui#512/bg_attack_red",
                SelectDrawPath.buildFromProjection().get(0).frameResourceId);
        assertEquals("an unresolved derived frame is a gap",
                Integer.valueOf(1), probe.get("missingArt"));
        assertEquals(Integer.valueOf(0), probe.get("materializedResolved"));
    }

    /**
     * U02b criterion (accounting honesty), native-default color branch: a KNOWN type with an
     * UNMAPPABLE color ({@code WAT}) or a blank color hits the native {@code render*Bg} DEFAULT
     * branch, which draws {@code CARD_SKILL_BG_BLACK} -> the descriptor is
     * {@code cardui#512/bg_skill_black} (a real {@code ImageMaster} black skill page), NOT empty.
     * The card's primary ({@code UI_SELECT_CARD_DISABLED} -> file-backed
     * {@code images/ui/event/disabledButton.png}) already resolves here, so the black skill page is
     * the ONLY unresolved required resource WITHOUT a live atlas: it remains a gap
     * ({@code missingArt == 1}) with nothing fully materialized ({@code materializedResolved == 0}).
     */
    @Test
    public void selectCardFrameUnknownColorUsesNativeDefaultBlackSkillPageAndCountsAsGap() {
        Sts1HostAssets.install();

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .playable(false)
                        .type("ATTACK")
                        .color("WAT")
                        .build()));
        SelectDrawPath.DrawItem unknown = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("an unmappable color is the native default -> the black skill page descriptor",
                "cardui#512/bg_skill_black", unknown.frameResourceId);
        Map<String, Object> unknownProbe = SelectDrawPath.probeSlice();
        assertEquals("the file-backed primary resolves, so the atlas-only frame is the only gap",
                Integer.valueOf(1), unknownProbe.get("missingArt"));
        assertEquals("the black skill page needs the live atlas, so nothing is materialized",
                Integer.valueOf(0), unknownProbe.get("materializedResolved"));

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g2", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .playable(false)
                        .type("ATTACK")
                        .color("")
                        .build()));
        SelectDrawPath.DrawItem blank = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("an absent color is also the native default -> the black skill page descriptor",
                "cardui#512/bg_skill_black", blank.frameResourceId);
        Map<String, Object> blankProbe = SelectDrawPath.probeSlice();
        assertEquals("the blank-color black page still leaves exactly the frame as the gap",
                Integer.valueOf(1), blankProbe.get("missingArt"));
        assertEquals(Integer.valueOf(0), blankProbe.get("materializedResolved"));
    }

    /**
     * U02 criterion 2 (accounting honesty), load-bearing half: with the card's primary already
     * file-backed ({@code playable(false)} -> {@code UI_SELECT_CARD_DISABLED}), the type/color-derived
     * frame descriptor is the ONLY remaining requirement. Without a live {@code cardui} atlas the
     * descriptor {@code cardui#512/bg_attack_red} does NOT resolve, so the item must still be a gap
     * ({@code missingArt == 1}, {@code materializedResolved == 0}); installing a live atlas carrying
     * {@code 512/bg_attack_red} must flip it to fully materialized ({@code missingArt == 0},
     * {@code materializedResolved == 1}). This pair proves the frame requirement - not the primary -
     * decides the account, so a regression that dropped it would fail the unresolved half (the gap
     * would silently become zero).
     */
    @Test
    public void selectCardFrameRequirementDecidesAccountingWhenPrimaryResolves() {
        Sts1HostAssets.install();

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .playable(false)
                        .type("ATTACK")
                        .color("RED")
                        .build()));
        SelectDrawPath.DrawItem unresolved = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("cardui#512/bg_attack_red", unresolved.frameResourceId);
        Map<String, Object> before = SelectDrawPath.probeSlice();
        assertEquals("the unresolved frame is the only gap even though the primary resolves",
                Integer.valueOf(1), before.get("missingArt"));
        assertEquals(Integer.valueOf(0), before.get("materializedResolved"));

        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        Map<String, Object> after = SelectDrawPath.probeSlice();
        assertEquals("the same item is fully materialized once the frame region resolves",
                Integer.valueOf(0), after.get("missingArt"));
        assertEquals(Integer.valueOf(1), after.get("materializedResolved"));
    }

    /**
     * U02c (accounting option (b)). A default (playable, non-disabled) select card's PRIMARY is
     * {@code ui.select.card -> cardui/frame}, a NON-file-backed logical handle rather than a
     * separately drawn texture: native {@code Sts1SurfaceRenderer.renderSelect} draws the
     * type/color-derived frame descriptor as the card's ONE base background, and the primary draw is
     * a no-op for {@code ui.select.card}/.selected. The accounting must therefore be decided by the
     * frame descriptor ALONE, so a live {@code cardui} atlas carrying the derived region leaves
     * {@code missingArt == 0} with every visible item fully materialized. On the pre-U02c code the
     * non-file-backed primary also fails {@code resolvesToDrawable}, so this card is counted as a gap
     * ({@code missingArt == 1}) even though its real base background resolves.
     */
    @Test
    public void selectResolvableBaseFrameAccountsWithoutRequiringNonFileBackedPrimary() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .build()));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("a default non-disabled card uses the primary select handle",
                ResourceIds.UI_SELECT_CARD, item.resourceId);
        assertFalse("the primary handle is a non-file-backed logical id, not a drawn texture",
                Sts1AssetMaterializer.isFileBacked(
                        ArtFramework.assets().resolve(item.resourceId).source));
        assertEquals("cardui#512/bg_attack_red", item.frameResourceId);

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertEquals("the resolved base frame is the only drawn texture, so there is no gap: "
                        + probe.get("missingArt"),
                Integer.valueOf(0), probe.get("missingArt"));
        assertEquals("the single visible item is fully materialized",
                Integer.valueOf(1), probe.get("materializedResolved"));
        // The accounting always partitions the visible draw-submission count.
        assertEquals("materializedResolved + missingArt must equal count",
                probe.get("count"),
                Integer.valueOf(
                        ((Integer) probe.get("materializedResolved")).intValue()
                                + ((Integer) probe.get("missingArt")).intValue()));
    }

    /**
     * U02e. Native {@code Sts1SurfaceRenderer.renderSelect} draws, for EVERY visible select item,
     * {@code drawResolvedTexture(sb, item.resourceId, b)} in addition to the base frame/banner/orb
     * regions. For a DISABLED card ({@code enabled == false}) that primary is
     * {@code ui.select.card.disabled}, which the vanilla catalog maps to a REAL file-backed
     * {@code images/.../disabledButton.png} overlay. So the overlay is a genuine SECOND draw that can
     * fail and MUST be required by the resolve-aware accounting. On the pre-U02e code
     * {@link SelectDrawPath#itemFullyResolves} inspects only the frame descriptor, so regressing the
     * overlay mapping to a non-file-backed logical id is silently accepted ({@code missingArt == 0});
     * this test pins the overlay as a required draw so that regression is reported as a gap.
     */
    @Test
    public void selectDisabledCardOverlayMustResolveOrCountAsGap() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        // Regress the disabled-card overlay mapping to a NON-file-backed logical id (no `images/`
        // path): the overlay is a REAL second draw in renderSelect, so this must be a gap.
        Map<String, String> catalog =
                new LinkedHashMap<String, String>(artframework.sts1.assets.Sts1VanillaCatalog.catalog());
        catalog.put(ResourceIds.UI_SELECT_CARD_DISABLED, "cardui/disabled");
        artframework.assets.HostAssetsHolder.get().registerVanillaCatalog(catalog);

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .playable(false)
                        .type("ATTACK")
                        .color("RED")
                        .build()));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertFalse("the card must be disabled so its overlay is drawn", item.enabled);
        assertEquals(ResourceIds.UI_SELECT_CARD_DISABLED, item.resourceId);
        assertFalse("the regressed overlay mapping must not be file-backed",
                Sts1AssetMaterializer.isFileBacked(
                        ArtFramework.assets().resolve(item.resourceId).source));
        assertEquals("the base frame still resolves", "cardui#512/bg_attack_red",
                item.frameResourceId);

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals("a disabled card whose drawn overlay does not resolve is a gap",
                Integer.valueOf(1), probe.get("missingArt"));
        assertEquals(Integer.valueOf(0), probe.get("materializedResolved"));
        assertEquals("materializedResolved + missingArt must equal count",
                probe.get("count"),
                Integer.valueOf(((Integer) probe.get("materializedResolved")).intValue()
                        + ((Integer) probe.get("missingArt")).intValue()));
    }

    /**
     * U02e guard. Pins the REAL vanilla catalog mapping for {@code ui.select.card.disabled} as a
     * file-backed {@code images/} draw ({@code disabledButton.png}), so a regression to a
     * non-drawable id fails the suite independently of the accounting change.
     */
    @Test
    public void selectDisabledCardOverlayMappingIsFileBacked() {
        Sts1HostAssets.install();
        AssetResolveResult overlay = ArtFramework.assets().resolve(ResourceIds.UI_SELECT_CARD_DISABLED);
        assertTrue("the disabled-card overlay key must be catalogued", overlay.found);
        assertTrue("ui.select.card.disabled must map to a file-backed images/ draw (disabledButton.png)",
                Sts1AssetMaterializer.isFileBacked(overlay.source));
    }

    /**
     * U02c guard (fail-open half of option (b)). An unknown TYPE ({@code WAT}) draws NO native
     * background, so the frame descriptor stays blank ({@code ""}) even with a live atlas; the
     * base-background requirement is therefore unmet and the item remains a gap
     * ({@code missingArt >= 1}) with nothing fully materialized. Dropping the redundant primary
     * requirement must not paper over a genuinely missing base background.
     */
    @Test
    public void selectUnknownTypeBlankFrameLeavesGapEvenWithLiveAtlas() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("WAT")
                        .color("RED")
                        .build()));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("an unknown type draws no native background, so no descriptor",
                "", item.frameResourceId);

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertTrue("a blank base-frame descriptor must remain a gap: " + probe.get("missingArt"),
                ((Integer) probe.get("missingArt")).intValue() >= 1);
        assertEquals("nothing is fully materialized without a base frame",
                Integer.valueOf(0), probe.get("materializedResolved"));
    }

    /**
     * U02d. The select card BANNER is a region in the SAME {@code cardui} atlas as the base frame,
     * derived from the native {@code AbstractCard.rarity} via {@code renderBannerImage}:
     * {@code 512/banner_common} for BASIC/COMMON/CURSE and the native default,
     * {@code 512/banner_uncommon} for UNCOMMON, {@code 512/banner_rare} for RARE. The mapping is
     * case-insensitive and a null/blank/unknown rarity hits the native default (common).
     */
    @Test
    public void cardBannerDescriptorMapsRarityToNativeCardUiBannerRegion() {
        assertEquals("cardui#512/banner_rare", SelectDrawPath.cardBannerDescriptor("RARE"));
        assertEquals("cardui#512/banner_uncommon", SelectDrawPath.cardBannerDescriptor("UNCOMMON"));
        assertEquals("lower-case rarity must map identically",
                "cardui#512/banner_rare", SelectDrawPath.cardBannerDescriptor("rare"));
        assertEquals("BASIC is the native common banner",
                "cardui#512/banner_common", SelectDrawPath.cardBannerDescriptor("BASIC"));
        assertEquals("COMMON is the native common banner",
                "cardui#512/banner_common", SelectDrawPath.cardBannerDescriptor("COMMON"));
        assertEquals("CURSE is the native common banner",
                "cardui#512/banner_common", SelectDrawPath.cardBannerDescriptor("CURSE"));
        assertEquals("an unknown rarity is the native default (common)",
                "cardui#512/banner_common", SelectDrawPath.cardBannerDescriptor("WAT"));
        assertEquals("a blank rarity is the native default (common)",
                "cardui#512/banner_common", SelectDrawPath.cardBannerDescriptor(""));
        assertEquals("an absent rarity is the native default (common)",
                "cardui#512/banner_common", SelectDrawPath.cardBannerDescriptor(null));
    }

    /**
     * U02d. {@code buildFromProjection()} threads the pool {@link CardView#rarity} into the built
     * non-confirm card item's {@code bannerResourceId} using the native rarity->banner table.
     */
    @Test
    public void buildFromProjectionThreadsCardRarityIntoBannerDescriptor() {
        assertEquals("cardui#512/banner_rare", firstCardBanner("RARE"));
        assertEquals("cardui#512/banner_uncommon", firstCardBanner("UNCOMMON"));
        assertEquals("cardui#512/banner_common", firstCardBanner("BASIC"));
        assertEquals("cardui#512/banner_common", firstCardBanner("COMMON"));
        assertEquals("cardui#512/banner_common", firstCardBanner("CURSE"));
        assertEquals("cardui#512/banner_common", firstCardBanner("WAT"));
        assertEquals("cardui#512/banner_common", firstCardBanner(""));
        assertEquals("cardui#512/banner_common", firstCardBanner(null));
    }

    /**
     * U02d. Adding the banner must NOT overwrite the card's base-background frame descriptor: a card
     * with type/color keeps {@code cardui#512/bg_attack_red} while its banner varies with rarity.
     */
    @Test
    public void selectCardBannerDoesNotDisturbBaseFrameDescriptor() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity("RARE")
                        .build()));
        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("the base-background frame is unchanged by the banner addition",
                "cardui#512/bg_attack_red", item.frameResourceId);
        assertEquals("the banner is a separate descriptor",
                "cardui#512/banner_rare", item.bannerResourceId);
    }

    /** U02d. The probe item rows expose the card banner descriptor under {@code bannerResourceId}. */
    @Test
    public void probeExposesCardBannerDescriptor() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity("UNCOMMON")
                        .build()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) SelectDrawPath.probeSlice().get("items");
        assertEquals("cardui#512/banner_uncommon", rows.get(0).get("bannerResourceId"));
    }

    /**
     * U02d + U02c freeze: the banner must NOT count toward the resolve-aware supply accounting. With
     * a live {@code cardui} atlas carrying ONLY {@code 512/bg_attack_red} (no banner region), a card
     * whose base frame resolves is still fully materialized ({@code missingArt == 0},
     * {@code materializedResolved == 1}) even though its banner region is unresolved.
     */
    @Test
    public void selectCardBannerDoesNotEnterU02cAccounting() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity("RARE")
                        .build()));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("cardui#512/bg_attack_red", item.frameResourceId);
        assertEquals("cardui#512/banner_rare", item.bannerResourceId);

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertEquals("the unresolved banner must not create a gap",
                Integer.valueOf(0), probe.get("missingArt"));
        assertEquals("the base frame alone decides the account",
                Integer.valueOf(1), probe.get("materializedResolved"));
        assertEquals("materializedResolved + missingArt must equal count",
                probe.get("count"),
                Integer.valueOf(
                        ((Integer) probe.get("materializedResolved")).intValue()
                                + ((Integer) probe.get("missingArt")).intValue()));
    }

    /**
     * U02d criterion 5. {@link CardView} {@code rarity} round-trips through the builder; a card built
     * without {@code .rarity(...)} defaults it to {@code ""} (the old constructor path).
     */
    @Test
    public void cardViewRarityRoundTripsAndDefaultsEmpty() {
        assertEquals("RARE",
                CardView.builder(new CardRef("g1", "Strike_R")).rarity("RARE").build().rarity);
        assertEquals("",
                CardView.builder(new CardRef("g2", "Defend_R")).build().rarity);
    }

    /**
     * U02d2. The select card ENERGY ORB is a region in the SAME {@code cardui} atlas as the base
     * frame and banner, drawn by native {@code AbstractCard.renderEnergy} from {@code this.color}:
     * RED/GREEN/BLUE/PURPLE map to {@code 512/card_<color>_orb} and the native DEFAULT (COLORLESS,
     * CURSE, any other/unknown/blank/absent color) maps to {@code 512/card_colorless_orb}. The
     * mapping is case-insensitive and fail-open (any failure yields the native colorless orb).
     */
    @Test
    public void cardOrbDescriptorMapsColorToNativeCardUiOrbRegion() {
        assertEquals("cardui#512/card_red_orb", SelectDrawPath.cardOrbDescriptor("RED"));
        assertEquals("cardui#512/card_green_orb", SelectDrawPath.cardOrbDescriptor("GREEN"));
        assertEquals("cardui#512/card_blue_orb", SelectDrawPath.cardOrbDescriptor("BLUE"));
        assertEquals("cardui#512/card_purple_orb", SelectDrawPath.cardOrbDescriptor("PURPLE"));
        assertEquals("lower-case color must map identically",
                "cardui#512/card_red_orb", SelectDrawPath.cardOrbDescriptor("red"));
        assertEquals("COLORLESS is the native colorless orb",
                "cardui#512/card_colorless_orb", SelectDrawPath.cardOrbDescriptor("COLORLESS"));
        assertEquals("CURSE is the native colorless orb",
                "cardui#512/card_colorless_orb", SelectDrawPath.cardOrbDescriptor("CURSE"));
        assertEquals("an unknown color is the native default (colorless)",
                "cardui#512/card_colorless_orb", SelectDrawPath.cardOrbDescriptor("WAT"));
        assertEquals("a blank color is the native default (colorless)",
                "cardui#512/card_colorless_orb", SelectDrawPath.cardOrbDescriptor(""));
        assertEquals("an absent color is the native default (colorless)",
                "cardui#512/card_colorless_orb", SelectDrawPath.cardOrbDescriptor(null));
    }

    /**
     * U02d2. {@code buildFromProjection()} threads the pool {@link CardView#color} into the built
     * non-confirm card item's {@code orbResourceId} using the native color->orb table, for a card
     * whose native cost permits the orb draw (the default fail-open {@code orbDrawable}).
     */
    @Test
    public void buildFromProjectionThreadsCardColorIntoOrbDescriptor() {
        assertEquals("cardui#512/card_red_orb", firstCardOrb("RED"));
        assertEquals("cardui#512/card_green_orb", firstCardOrb("GREEN"));
        assertEquals("cardui#512/card_blue_orb", firstCardOrb("BLUE"));
        assertEquals("cardui#512/card_purple_orb", firstCardOrb("PURPLE"));
        assertEquals("cardui#512/card_colorless_orb", firstCardOrb("COLORLESS"));
        assertEquals("cardui#512/card_colorless_orb", firstCardOrb("WAT"));
        assertEquals("cardui#512/card_colorless_orb", firstCardOrb(""));
        assertEquals("cardui#512/card_colorless_orb", firstCardOrb(null));
    }

    /**
     * U02d2. Adding the orb must NOT overwrite the card's base-background frame or banner
     * descriptors: a card with type/color/rarity keeps {@code cardui#512/bg_attack_red} and
     * {@code cardui#512/banner_rare} while gaining {@code cardui#512/card_red_orb}.
     */
    @Test
    public void selectCardOrbDoesNotDisturbBaseFrameOrBanner() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity("RARE")
                        .build()));
        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("the base-background frame is unchanged by the orb addition",
                "cardui#512/bg_attack_red", item.frameResourceId);
        assertEquals("the banner is unchanged by the orb addition",
                "cardui#512/banner_rare", item.bannerResourceId);
        assertEquals("the orb is a separate descriptor",
                "cardui#512/card_red_orb", item.orbResourceId);
    }

    /** U02d2. The probe item rows expose the card orb descriptor under {@code orbResourceId}. */
    @Test
    public void probeExposesCardOrbDescriptor() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .build()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) SelectDrawPath.probeSlice().get("items");
        assertEquals("cardui#512/card_red_orb", rows.get(0).get("orbResourceId"));
    }

    /**
     * U02d2 cost gate. Native {@code renderEnergy} early-returns when {@code cost <= -2} (or the
     * card is darkened/locked/unseen), so the projected {@code orbDrawable} gate suppresses the orb
     * draw. A card explicitly built with {@code .orbDrawable(false)} draws NO orb
     * ({@code orbResourceId == ""}) while its frame/banner descriptors stay unchanged; the default
     * (fail-open {@code true}) still draws the orb.
     */
    @Test
    public void selectCardOrbSkipsDrawWhenCostGateDisallows() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity("RARE")
                        .orbDrawable(false)
                        .build()));
        SelectDrawPath.DrawItem gated = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("native cost <= -2 skips the orb draw",
                "", gated.orbResourceId);
        assertEquals("the cost gate must not disturb the base frame",
                "cardui#512/bg_attack_red", gated.frameResourceId);
        assertEquals("the cost gate must not disturb the banner",
                "cardui#512/banner_rare", gated.bannerResourceId);

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g2", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .build()));
        assertEquals("the default orbDrawable draws the orb (fail-open)",
                "cardui#512/card_red_orb",
                SelectDrawPath.buildFromProjection().get(0).orbResourceId);
    }

    /**
     * U02d2 criterion 6. {@link CardView} {@code orbDrawable} round-trips through the builder; a card
     * built without {@code .orbDrawable(...)} defaults it to {@code true} (fail-open: an absent cost
     * projection still draws the orb).
     */
    @Test
    public void cardViewOrbDrawableRoundTripsAndDefaultsTrue() {
        assertTrue("orbDrawable must default to true (fail-open)",
                CardView.builder(new CardRef("g1", "Strike_R")).build().orbDrawable);
        assertFalse("an explicit .orbDrawable(false) must be honoured",
                CardView.builder(new CardRef("g2", "Defend_R"))
                        .orbDrawable(false).build().orbDrawable);
    }

    /**
     * U02d2 + U02c freeze: the orb must NOT count toward the resolve-aware supply accounting. With a
     * live {@code cardui} atlas carrying ONLY {@code 512/bg_attack_red} (no orb region), a card whose
     * base frame resolves is still fully materialized ({@code missingArt == 0},
     * {@code materializedResolved == 1}) even though its orb region is unresolved.
     */
    @Test
    public void selectCardOrbDoesNotEnterU02cAccounting() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity("RARE")
                        .build()));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("cardui#512/bg_attack_red", item.frameResourceId);
        assertEquals("cardui#512/card_red_orb", item.orbResourceId);

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertEquals("the unresolved orb must not create a gap",
                Integer.valueOf(0), probe.get("missingArt"));
        assertEquals("the base frame alone decides the account",
                Integer.valueOf(1), probe.get("materializedResolved"));
    }

    /**
     * U02d2 fail-open. A card with an absent or blank color has no native orb page of its own, so it
     * draws {@code cardui#512/card_colorless_orb} when the orb draw is permitted.
     */
    @Test
    public void selectCardOrbFailOpenToColorlessWhenColorAbsentOrBlank() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("")
                        .build()));
        assertEquals("a blank color draws the native colorless orb",
                "cardui#512/card_colorless_orb",
                SelectDrawPath.buildFromProjection().get(0).orbResourceId);

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g2", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .build()));
        assertEquals("an absent color draws the native colorless orb",
                "cardui#512/card_colorless_orb",
                SelectDrawPath.buildFromProjection().get(0).orbResourceId);
    }

    /**
     * U04. The SELECT card PORTRAIT comes from the native {@code cards/cards.atlas} page, whose
     * region names ARE the native card art urls ({@code AbstractCard.assetUrl}, the exact string
     * native {@code AbstractCard} passes to {@code cardAtlas.findRegion(...)}). The projected
     * {@link CardView#artResourceId} carries that art url ({@code red/attack/strike}), so the built
     * item exposes the region descriptor {@code cards#red/attack/strike} under
     * {@code portraitResourceId}.
     */
    @Test
    public void selectCardPortraitDescriptorUsesNativeCardsAtlasRegion() {
        publishSelectFrame(singleSelectCard(portraitCard("red/attack/strike")));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("the portrait descriptor is the native cards atlas region",
                "cards#red/attack/strike", firstProbePortrait());
        assertEquals("the portrait must not disturb the base frame",
                "cardui#512/bg_attack_red", item.frameResourceId);
        assertEquals("the portrait must not disturb the banner",
                "cardui#512/banner_rare", item.bannerResourceId);
    }

    /**
     * U04 fail-open. An art value that is already a {@code "<atlasKey>#<region>"} descriptor (the
     * backend sets {@code cards#<artUrl>}) is passed through unchanged instead of being prefixed
     * twice.
     */
    @Test
    public void selectCardPortraitDescriptorAcceptsQualifiedDescriptor() {
        publishSelectFrame(singleSelectCard(portraitCard("cards#red/attack/strike")));

        assertEquals("cards#red/attack/strike", firstProbePortrait());
    }

    /** U04 fail-open. A blank or absent art url draws NO portrait: the descriptor stays empty. */
    @Test
    public void selectCardBlankOrAbsentArtUrlYieldsEmptyPortraitDescriptor() {
        publishSelectFrame(singleSelectCard(portraitCard("")));
        assertEquals("a blank art url draws no portrait", "", firstProbePortrait());

        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity("RARE")
                        .build()));
        assertEquals("an absent art url draws no portrait", "", firstProbePortrait());
    }

    /**
     * U04. Adding the portrait must leave the other card descriptors untouched: a card with
     * type/color/rarity keeps its base frame, banner and orb while gaining the portrait.
     */
    @Test
    public void selectCardPortraitDoesNotDisturbFrameBannerOrOrb() {
        publishSelectFrame(singleSelectCard(portraitCard("red/attack/strike")));
        SelectDrawPath.DrawItem withPortrait = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("cardui#512/bg_attack_red", withPortrait.frameResourceId);
        assertEquals("cardui#512/banner_rare", withPortrait.bannerResourceId);
        assertEquals("cardui#512/card_red_orb", withPortrait.orbResourceId);
        assertEquals("the portrait is the one NEW descriptor",
                "cards#red/attack/strike", firstProbePortrait());

        publishSelectFrame(singleSelectCard(portraitCard("")));
        SelectDrawPath.DrawItem withoutPortrait = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("the base frame is identical without a portrait",
                withPortrait.frameResourceId, withoutPortrait.frameResourceId);
        assertEquals("the banner is identical without a portrait",
                withPortrait.bannerResourceId, withoutPortrait.bannerResourceId);
        assertEquals("the orb is identical without a portrait",
                withPortrait.orbResourceId, withoutPortrait.orbResourceId);
        assertEquals("the primary select handle is identical without a portrait",
                withPortrait.resourceId, withoutPortrait.resourceId);
        assertEquals("only the portrait descriptor differs", "", firstProbePortrait());
    }

    /**
     * U04 + U02c freeze: the portrait is an ADDITIONAL draw outside the resolve-aware supply
     * accounting. With a live {@code cardui} atlas carrying ONLY the base frame and a live
     * {@code cards} atlas carrying the portrait region, the card is fully materialized
     * ({@code missingArt == 0}, {@code materializedResolved == 1}) and
     * {@code materializedResolved + missingArt == count}.
     */
    @Test
    public void selectCardPortraitDoesNotEnterU02cAccountingWhenResolvable() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardsAndCardUiLiveSource());
        publishSelectFrame(singleSelectCard(portraitCard("red/attack/strike")));

        assertEquals("cards#red/attack/strike", firstProbePortrait());

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertEquals("a resolvable portrait must not create a gap",
                Integer.valueOf(0), probe.get("missingArt"));
        assertEquals("the base frame alone decides the account",
                Integer.valueOf(1), probe.get("materializedResolved"));
        assertEquals("materializedResolved + missingArt must equal count",
                probe.get("count"),
                Integer.valueOf(
                        ((Integer) probe.get("materializedResolved")).intValue()
                                + ((Integer) probe.get("missingArt")).intValue()));
    }

    /**
     * U04 + U02c freeze (fail-open half). A portrait region that does NOT resolve must not make the
     * card a {@code missingArt} gap: the portrait is never required by {@code itemFullyResolves}.
     */
    @Test
    public void selectCardPortraitDoesNotEnterU02cAccountingWhenUnresolvable() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(singleSelectCard(portraitCard("red/attack/strike")));

        assertEquals("the descriptor is still built even when the region is absent",
                "cards#red/attack/strike", firstProbePortrait());

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertEquals("an unresolved portrait must not create a gap",
                Integer.valueOf(0), probe.get("missingArt"));
        assertEquals(Integer.valueOf(1), probe.get("materializedResolved"));
    }

    /** U04 select card with the given art url, keeping every other descriptor deterministic. */
    private static CardView portraitCard(String artResourceId) {
        return CardView.builder(new CardRef("g1", "Strike_R"))
                .zone(CardZone.SELECT)
                .slot(0)
                .type("ATTACK")
                .color("RED")
                .rarity("RARE")
                .art(artResourceId)
                .build();
    }

    /**
     * The portrait descriptor of the currently published frame's {@code items[0]} row, failing with
     * an assertion (rather than an NPE) when the U04 key has not landed yet.
     */
    private String firstProbePortrait() {
        Map<String, Object> row = firstProbeItemRow();
        Object value = row.get("portraitResourceId");
        assertNotNull("the probe item row must expose portraitResourceId", value);
        return (String) value;
    }

    /**
     * Live atlas source exposing the native {@code cards} page (portrait regions named by art url)
     * alongside the {@code cardui} base-background page.
     */
    private static Sts1AtlasMaterializer.LiveAtlasSource cardsAndCardUiLiveSource() {
        StubLiveAtlasSource source = (StubLiveAtlasSource) cardUiLiveSource("512/bg_attack_red");
        Texture cardsPage = noGlTexture(2048, 2048);
        TextureAtlas cards = new TextureAtlas();
        cards.addRegion("red/attack/strike", cardsPage, 997, 375, 248, 186);
        source.atlases.put("cards", cards);
        return source;
    }

    /**
     * U02d3. Native {@code AbstractCard.renderEnergy} draws the ENERGY COST NUMBER after the orb at
     * {@code (-132*scale, 192*scale)} with the large energy font scaled by the card draw scale. ART
     * maps the uniform select-card scale as {@code factor = item.w / 512f} (the renderer's
     * {@code bounds.width / region.originalWidth} analogue; the select card box is 250x350 and every
     * cardui region has originalWidth 512). A normal ATTACK/RED card built with {@code cost("1")}
     * therefore draws the white "1" at {@code (-64.453125, 93.75)} scaled by {@code 0.48828125}, and
     * the probe item row exposes the same values.
     */
    @Test
    public void selectCardCostNumberDrawsForNormalCard() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .cost("1")
                        .build()));
        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        Map<String, Object> row = firstProbeItemRow();

        assertEquals("a normal card keeps the orb descriptor", "cardui#512/card_red_orb",
                item.orbResourceId);
        assertEquals("a normal select card draws the cost number", Boolean.TRUE,
                row.get("costDrawable"));
        assertEquals("the cost number is the projected CardView.cost", "1", row.get("costText"));
        assertEquals("an unmodified, unrestricted cost draws white", "white", row.get("costColor"));
        assertEquals("costScale is the uniform card scale item.w / 512f",
                0.48828125f, probeFloat(row, "costScale"), 0.000001f);
        assertEquals("costOffsetX = -132 * factor",
                -64.453125f, probeFloat(row, "costOffsetX"), 0.001f);
        assertEquals("costOffsetY = 192 * factor",
                93.75f, probeFloat(row, "costOffsetY"), 0.001f);
    }

    /**
     * U02d3 cost gate. Native {@code renderEnergy} early-returns before drawing BOTH the orb and the
     * number when {@code cost <= -2} (projected as {@code orbDrawable == false}): the item exposes no
     * orb resource, an empty {@code costText} and an empty {@code costColor}.
     */
    @Test
    public void selectCardCostNumberSkipsDrawWhenCostGateDisallows() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "AscendersBane"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("CURSE")
                        .color("CURSE")
                        .cost("-2")
                        .orbDrawable(false)
                        .build()));
        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        Map<String, Object> row = firstProbeItemRow();

        assertEquals("native cost <= -2 draws no orb either", "", item.orbResourceId);
        assertEquals("native cost <= -2 suppresses the cost number", Boolean.FALSE,
                row.get("costDrawable"));
        assertEquals("a suppressed number has no text", "", row.get("costText"));
        assertEquals("a suppressed number has no colour", "", row.get("costColor"));
    }

    /**
     * U02d3 text gate. Native {@code renderEnergy} suppresses only the NUMBER (the orb still draws)
     * for STATUS cards other than Slimed and CURSE cards other than Pride, matching native
     * {@code (type != STATUS || cardID == "Slimed") && (color != CURSE || cardID == "Pride")}.
     * Fail-open: a blank/unknown type or colour satisfies its clause, so the number draws.
     */
    @Test
    public void selectCardCostTextGateMirrorsNativeStatusCurseExceptions() {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Wound"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("STATUS")
                        .color("COLORLESS")
                        .cost("1")
                        .build()));
        SelectDrawPath.DrawItem wound = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("a plain STATUS card suppresses the cost number", Boolean.FALSE,
                firstProbeItemRow().get("costDrawable"));
        assertEquals("the number gate must not disturb the orb",
                "cardui#512/card_colorless_orb", wound.orbResourceId);

        assertEquals("a plain CURSE card suppresses the cost number", Boolean.FALSE,
                costRow("CURSE", "CURSE", "AscendersBane", "-2").get("costDrawable"));
        assertEquals("Slimed is the STATUS exception that still draws the number", Boolean.TRUE,
                costRow("STATUS", "COLORLESS", "Slimed", "1").get("costDrawable"));
        assertEquals("Pride is the CURSE exception that still draws the number", Boolean.TRUE,
                costRow("CURSE", "CURSE", "Pride", "1").get("costDrawable"));

        Map<String, Object> unknown = costRow("", "", "Strike_R", "1");
        assertEquals("a blank type/colour fails open and draws the number", Boolean.TRUE,
                unknown.get("costDrawable"));
        assertEquals("a blank type/colour draws white", "white", unknown.get("costColor"));
        assertEquals("a blank colour alone fails open and draws the number", Boolean.TRUE,
                costRow("ATTACK", "", "Strike_R", "1").get("costDrawable"));
    }

    /**
     * U02d3 + U02c freeze: the cost NUMBER (like the orb and banner) must NOT count toward the
     * resolve-aware supply accounting. With a live {@code cardui} atlas carrying ONLY
     * {@code 512/bg_attack_red}, a card whose base frame resolves is still fully materialized
     * ({@code missingArt == 0}, {@code materializedResolved == 1}) even though its orb region is
     * unresolved and its cost number is a font draw, not an atlas resource.
     */
    @Test
    public void selectCardCostNumberDoesNotEnterU02cAccounting() {
        Sts1HostAssets.install();
        Sts1AtlasMaterializer.setLiveAtlasSource(cardUiLiveSource("512/bg_attack_red"));
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .cost("1")
                        .build()));

        SelectDrawPath.DrawItem item = SelectDrawPath.buildFromProjection().get(0);
        assertEquals("cardui#512/bg_attack_red", item.frameResourceId);
        assertEquals("the cost number is drawable for this card", Boolean.TRUE,
                firstProbeItemRow().get("costDrawable"));

        Map<String, Object> probe = SelectDrawPath.probeSlice();
        assertEquals(Integer.valueOf(1), probe.get("count"));
        assertEquals("the cost number must not create a gap",
                Integer.valueOf(0), probe.get("missingArt"));
        assertEquals("the base frame alone decides the account",
                Integer.valueOf(1), probe.get("materializedResolved"));
    }

    /**
     * U02d3 colour flags. Native {@code renderEnergy} picks the number colour with the RESTRICTED
     * branch FIRST ({@code player != null && player.hand.contains(card) && !card.hasEnoughEnergy()}
     * -> {@code (1.0, 0.3, 0.3)}), else the MODIFIED branch ({@code isCostModified ||
     * isCostModifiedForTurn || freeToPlay()} -> {@code (0.4, 1.0, 0.4)}), else white. The projection
     * carries both as fail-open {@code false} flags on {@link CardView}; the item/probe expose the
     * chosen colour NAME and {@code costColorRgba} gives the native RGBA.
     */
    @Test
    public void selectCardCostColorFlagsPreferRestrictedThenModifiedElseWhite() {
        assertTrue("an unmodified, unrestricted card is drawable", costItem(false, false).costDrawable);
        assertEquals("neither flag draws white", "white", costItem(false, false).costColor);
        assertEquals("modified alone draws green", "green", costItem(true, false).costColor);
        assertEquals("restricted alone draws red", "red", costItem(false, true).costColor);
        assertEquals("restricted takes precedence over modified", "red",
                costItem(true, true).costColor);
        assertEquals("the probe row carries the same colour name", "green",
                costColorRow(true, false).get("costColor"));

        assertEquals("white", SelectDrawPath.costColorName(false, false));
        assertEquals("green", SelectDrawPath.costColorName(true, false));
        assertEquals("red", SelectDrawPath.costColorName(false, true));
        assertEquals("red", SelectDrawPath.costColorName(true, true));

        assertArrayEquals(new float[] {1f, 1f, 1f, 1f},
                SelectDrawPath.costColorRgba("white"), 0.000001f);
        assertArrayEquals(new float[] {0.4f, 1f, 0.4f, 1f},
                SelectDrawPath.costColorRgba("green"), 0.000001f);
        assertArrayEquals(new float[] {1f, 0.3f, 0.3f, 1f},
                SelectDrawPath.costColorRgba("red"), 0.000001f);
        assertArrayEquals("a blank colour name fails open to white",
                new float[] {1f, 1f, 1f, 1f}, SelectDrawPath.costColorRgba(""), 0.000001f);
        assertArrayEquals("an unknown colour name fails open to white",
                new float[] {1f, 1f, 1f, 1f}, SelectDrawPath.costColorRgba("chartreuse"), 0.000001f);

        assertEquals("the native cardui page is 512px",
                512f, SelectDrawPath.CARDUI_PAGE, 0.000001f);
        assertEquals("the native cost offset X is -132",
                -132f, SelectDrawPath.COST_OFFSET_X, 0.000001f);
        assertEquals("the native cost offset Y is 192",
                192f, SelectDrawPath.COST_OFFSET_Y, 0.000001f);

        assertFalse("costModified must default to false (fail-open)",
                CardView.builder(new CardRef("g1", "Strike_R")).build().costModified);
        assertFalse("costRestricted must default to false (fail-open)",
                CardView.builder(new CardRef("g2", "Strike_R")).build().costRestricted);
        assertTrue("an explicit .costModified(true) must round-trip",
                CardView.builder(new CardRef("g3", "Strike_R"))
                        .costModified(true).build().costModified);
        assertTrue("an explicit .costRestricted(true) must round-trip",
                CardView.builder(new CardRef("g4", "Strike_R"))
                        .costRestricted(true).build().costRestricted);
    }

    /** Publishes a single cost-1 select card with the given flags and returns its built item. */
    private SelectDrawPath.DrawItem costItem(boolean modified, boolean restricted) {
        publishSelectFrame(singleSelectCard(costCard(modified, restricted)));
        return SelectDrawPath.buildFromProjection().get(0);
    }

    /** Publishes the same flagged card and returns the probe's {@code items[0]} row. */
    private Map<String, Object> costColorRow(boolean modified, boolean restricted) {
        publishSelectFrame(singleSelectCard(costCard(modified, restricted)));
        return firstProbeItemRow();
    }

    private static CardView costCard(boolean modified, boolean restricted) {
        return CardView.builder(new CardRef("g1", "Strike_R"))
                .zone(CardZone.SELECT)
                .slot(0)
                .type("ATTACK")
                .color("RED")
                .cost("1")
                .costModified(modified)
                .costRestricted(restricted)
                .build();
    }

    /** Publishes a single-card select frame and returns the probe's {@code items[0]} row. */
    private Map<String, Object> costRow(String type, String color, String cardId, String cost) {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", cardId))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type(type)
                        .color(color)
                        .cost(cost)
                        .build()));
        return firstProbeItemRow();
    }

    /**
     * Reads a required numeric probe value as a {@code float}, failing with an assertion (rather
     * than an NPE) when the key is absent because the cost-number slice has not landed yet.
     */
    private static float probeFloat(Map<String, Object> row, String key) {
        Object value = row.get(key);
        assertNotNull("the probe row must expose " + key, value);
        assertTrue("the probe row " + key + " must be numeric", value instanceof Number);
        return ((Number) value).floatValue();
    }

    /** The probe's {@code items[0]} row of the currently published select frame. */
    private Map<String, Object> firstProbeItemRow() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) SelectDrawPath.probeSlice().get("items");
        assertNotNull("the probe must expose item rows", rows);
        return rows.get(0);
    }

    /** Builds a single ATTACK select card with the given color and returns its orb descriptor. */
    private String firstCardOrb(String color) {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color(color)
                        .build()));
        return SelectDrawPath.buildFromProjection().get(0).orbResourceId;
    }

    /** Builds a single ATTACK/RED select card with the given rarity and returns its banner. */
    private String firstCardBanner(String rarity) {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type("ATTACK")
                        .color("RED")
                        .rarity(rarity)
                        .build()));
        return SelectDrawPath.buildFromProjection().get(0).bannerResourceId;
    }

    /** Builds a single-card select frame and returns the built card's frame descriptor. */
    private SelectDrawPath.DrawItem firstDrawItem(String type, String color) {
        publishSelectFrame(singleSelectCard(
                CardView.builder(new CardRef("g1", "Strike_R"))
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .type(type)
                        .color(color)
                        .build()));
        return SelectDrawPath.buildFromProjection().get(0);
    }

    private String firstCardFrame(String type, String color) {
        return firstDrawItem(type, color).frameResourceId;
    }

    private static SelectView singleSelectCard(CardView card) {
        return SelectView.grid(
                Collections.singletonList(card),
                Collections.<String>emptyList(),
                false,
                false);
    }

    /** Live atlas source exposing a {@code cardui} page with the given base-background regions. */
    private static Sts1AtlasMaterializer.LiveAtlasSource cardUiLiveSource(String... regionNames) {
        Texture page = noGlTexture(512, 512);
        TextureAtlas atlas = new TextureAtlas();
        for (int i = 0; i < regionNames.length; i++) {
            atlas.addRegion(regionNames[i], page, 10 * (i + 1), 20, 64, 48);
        }
        StubLiveAtlasSource source = new StubLiveAtlasSource();
        source.atlases.put("cardui", atlas);
        return source;
    }

    private static Texture noGlTexture(final int width, final int height) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Texture texture =
                    (Texture) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(Texture.class);
            Field dataField = Texture.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(texture, new TextureData() {
                public TextureDataType getType() { return TextureDataType.Pixmap; }
                public boolean isPrepared() { return true; }
                public void prepare() {}
                public Pixmap consumePixmap() { return null; }
                public boolean disposePixmap() { return false; }
                public void consumeCustomData(int target) {}
                public int getWidth() { return width; }
                public int getHeight() { return height; }
                public Pixmap.Format getFormat() { return Pixmap.Format.RGBA8888; }
                public boolean useMipMaps() { return false; }
                public boolean isManaged() { return false; }
            });
            return texture;
        } catch (Exception failure) {
            throw new AssertionError("could not create no-GL texture test double", failure);
        }
    }

    private static final class StubLiveAtlasSource implements Sts1AtlasMaterializer.LiveAtlasSource {
        final Map<String, TextureAtlas> atlases = new LinkedHashMap<String, TextureAtlas>();

        public TextureAtlas atlas(String atlasKey) {
            return atlases.get(atlasKey);
        }
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
