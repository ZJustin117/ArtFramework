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
