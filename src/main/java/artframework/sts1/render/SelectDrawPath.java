package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardView;
import artframework.context.SelectView;
import artframework.context.SurfaceIds;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.sts1.FullPresentMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid/hand select full-present draw description (22.3). Host paints pool chrome from SelectView.
 */
public final class SelectDrawPath {

    /**
     * Native cardui bitmap page width in pixels. Every select-card region (base background, banner,
     * energy orb) is packed in the {@code 512}-wide {@code cardui} page, so the uniform native card
     * scale {@code drawScale} maps to ART's {@code item.w / CARDUI_PAGE}.
     */
    static final float CARDUI_PAGE = 512f;

    /**
     * Native {@code AbstractCard.renderEnergy} horizontal offset of the cost number relative to the
     * card centre: {@code -132f * drawScale * Settings.scale}.
     */
    static final float COST_OFFSET_X = -132f;

    /**
     * Native {@code AbstractCard.renderEnergy} vertical offset of the cost number relative to the
     * card centre: {@code 192f * drawScale * Settings.scale}.
     */
    static final float COST_OFFSET_Y = 192f;

    /**
     * U06: native {@code AbstractCard.renderTitle} banner-arch anchor relative to the card centre:
     * {@code 175f * drawScale} unscaled. ART scales it by the SAME uniform card scale
     * {@code w / 512f} used by {@link #COST_OFFSET_Y} (which already absorbs the host
     * {@code Settings.scale}), never by the native {@code card.drawScale}.
     */
    static final float TITLE_OFFSET_Y = 175f;

    /** U06: native small-title font multiplier ({@code useSmallTitleFont}) applied to the font only. */
    static final float SMALL_TITLE_SCALE = 0.85f;

    /**
     * U07: native {@code AbstractCard.IMG_HEIGHT} in cardui PAGE units (2626:
     * {@code IMG_HEIGHT = 420.0f * Settings.scale}; the ART scale absorbs the host
     * {@code Settings.scale}, so only the page-unit 420 remains).
     */
    static final float DESC_IMG_HEIGHT = 420f;

    /**
     * U07: native {@code DESC_OFFSET_Y / IMG_HEIGHT} with {@code Settings.BIG_TEXT_MODE == false}
     * (2636: {@code DESC_OFFSET_Y = IMG_HEIGHT * 0.255f}), i.e. {@code 420 * 0.255 = 107.1} page
     * units below the card centre for the description block's top edge.
     */
    static final float DESC_OFFSET_Y_RATIO = 0.255f;

    /** U07: native per-line advance factor (1563: {@code spacing = 1.45f * -capHeight / ...}). */
    static final float DESC_LINE_ADVANCE_FACTOR = 1.45f;

    /** U07: native first-baseline shift, {@code 0.775f} term of AbstractCard 1562. */
    static final float DESC_LINE_COUNT_FACTOR = 0.775f;

    /** U07: native first-baseline shift, {@code 0.375f} term of AbstractCard 1562. */
    static final float DESC_LINE_COUNT_OFFSET = 0.375f;

    /**
     * U07: native per-chunk screen-space vertical nudge (1567-1582 / 1648 pass
     * {@code ... - current_y + -6.0f}) applied to every description chunk.
     */
    static final float DESC_CHUNK_NUDGE_Y = -6.0f;

    /**
     * U07: documented truncation cap for the ART description block. ART wraps its own raw string and
     * DROPS the tail (truncation, never an ellipsis) so a long card description cannot overrun the
     * card's lower half. VANILLA native does NOT shrink: it only logs a dev warning
     * ({@code Settings.isDev && numLines > 5}, AbstractCard 481-482) and lets the text overflow. The
     * five-line shrink is BaseMod's {@code ShrinkLongDescription} patch, deliberately not mirrored
     * because ART does not depend on BaseMod.
     */
    static final int DESC_MAX_LINES = 5;

    /**
     * U07: deterministic ART wrap width in characters, NOT a pixel width. Native wraps the raw text
     * against {@code DESC_BOX_WIDTH} in pixels; ART pins a character count so the wrap is pure,
     * host-independent and testable on the 512-wide cardui page scale.
     */
    static final int DESC_MAX_CHARS_PER_LINE = 24;

    /**
     * U07c: the fixed, VALUE-FREE placeholder that stands in for a native dynamic-value slot in a
     * description line.
     *
     * <p>Native does not paint {@code !D!}/{@code !B!}/{@code !M!}: at wrap/prep time
     * {@code AbstractCard.initializeDescriptionCN} tests {@code word.equals("!D!")} (L559),
     * {@code word.equals("!B!")} (L572) and {@code word.equals("!M!")} (L585) and appends a single
     * character SLOT for the native {@code MAGIC_NUM_W} value; at draw time
     * {@code renderDescriptionCN} (L1707-1725) / {@code getDynamicValue} (L1655-1687) fills that slot
     * with the LIVE damage/block/magic NUMBER. Rendering that live number faithfully is the separate
     * U07d slice and is deliberately NOT done here.
     *
     * <p>This slice's narrower honest contract is therefore: no raw marker token is ever painted, and
     * no number is invented. {@code "?"} is chosen because native reserves exactly ONE character per
     * dynamic value and {@code ?} is exactly one ASCII character, so it occupies one slot and
     * participates in the wrap width; it is font-safe in both description fonts; it CANNOT be read as
     * a number (so nothing is fabricated); and it cannot be confused with a {@code !}-delimited token,
     * so a second substitution pass can never treat it as a marker. It carries NO damage, block or
     * magic number.
     */
    public static final String DYNAMIC_VALUE_PLACEHOLDER = "?";

    /**
     * U07: one drawable piece of a description line. Native splits a line into "cached tokenized
     * text" fragments and colours each fragment by its leading {@code '*'} (gold) or not (cream);
     * this carrier is the ART equivalent: the fragment TEXT (already stripped of its stars) plus the
     * single {@link #keyword} flag the renderer maps to gold vs cream.
     */
    public static final class DescriptionChunk {
        /** The fragment's drawable text, never containing a {@code '*'}. */
        public final String text;
        /** True when the fragment came from a {@code *...*} keyword span (native {@code goldColor}). */
        public final boolean keyword;

        public DescriptionChunk(String text, boolean keyword) {
            this.text = text != null ? text : "";
            this.keyword = keyword;
        }
    }

    public static final class DrawItem {
        public final String instanceId;
        /** Raw card id (e.g. {@code Strike_R}); identity/resource selection only. */
        public final String cardId;
        /**
         * Localized card name (the projection's {@link CardView#title}) used for the drawn text,
         * resolved to the raw {@link #cardId} when the title is null/empty. Never used for
         * identity or resource selection.
         */
        public final String label;
        public final int slot;
        public final boolean selected;
        public final boolean visible;
        public final float x;
        public final float y;
        public final boolean confirm;
        public final boolean enabled;
        public final float w;
        public final float h;
        public final String resourceId;
        public final String frameResourceId;
        /**
         * U02d: select card BANNER descriptor ({@code cardui#512/banner_<rarity>}) derived from the
         * native {@code AbstractCard.rarity} via {@code renderBannerImage}; empty when unavailable.
         * Parallel to {@link #frameResourceId}; an ADDITIONAL drawn region, not part of accounting.
         */
        public final String bannerResourceId;
        /**
         * U02d2: select card ENERGY ORB descriptor ({@code cardui#512/card_<color>_orb}) derived from
         * the native {@code AbstractCard.renderEnergy} {@code this.color} switch; empty when the
         * native cost gate suppresses the orb draw. Parallel to {@link #frameResourceId}; an
         * ADDITIONAL drawn region, never part of accounting.
         */
        public final String orbResourceId;
        /**
         * U04: select card PORTRAIT descriptor ({@code cards#<artUrl>}) over the native
         * {@code cards/cards.atlas} page, whose region names ARE the native card art urls
         * ({@code AbstractCard.assetUrl}, the exact string native passes to
         * {@code cardAtlas.findRegion(...)}). Derived from {@link CardView#artResourceId} by
         * {@link #portraitDescriptor}; empty when the card has no art url or the value is not a
         * native art url.
         *
         * <p>Parallel to {@link #frameResourceId} and drawn as an ADDITIONAL region between the
         * base background and the banner. It is deliberately OUTSIDE the U02c resolve-aware
         * accounting: {@link #itemFullyResolves} never consults it, so a missing portrait page
         * never turns a card into a {@code missingArt} gap.
         */
        public final String portraitResourceId;
        /**
         * U02d3: true when the native {@code AbstractCard.renderEnergy} cost NUMBER draw is
         * permitted: the orb draw gate ({@link CardView#orbDrawable}) AND the native text gate
         * {@link #costTextGate} both hold. False suppresses the number; the orb is unaffected.
         */
        public final boolean costDrawable;
        /** U02d3: the projected cost text (native string form); {@code ""} when not drawn. */
        public final String costText;
        /**
         * U02d3: the chosen native cost colour NAME ({@code "white"}/{@code "green"}/{@code "red"});
         * {@code ""} when not drawn. See {@link #costColorName}.
         */
        public final String costColor;
        /** U02d3: native {@code -132f * scale} offset applied to the number draw. */
        public final float costOffsetX;
        /** U02d3: native {@code 192f * scale} offset applied to the number draw. */
        public final float costOffsetY;
        /**
         * U02d3: native font scale = the uniform card scale {@code w / 512f} (the
         * {@code bounds.width / region.originalWidth} analogue; fail-open {@code 1f} when {@code w}
         * is non-positive).
         */
        public final float costScale;
        /**
         * U06: the native title text carried VERBATIM from {@link CardView#title}, which itself
         * substitutes {@code ref.cardId} when the projection has no title (so an absent native card
         * name lands on the arch as the raw id). Drawability is decided by {@link #titleDrawable}
         * (whitespace/blank suppression only). Never used for identity or resource selection -
         * {@link #cardId} owns those. {@code ""} when the item was built without a title (confirm
         * pseudo-item, legacy construction).
         */
        public final String title;
        /**
         * U06: native {@code AbstractCard.renderTitle} banner-arch anchor:
         * {@code 175f * (item.w / 512f)} - the SAME uniform ART card scale as {@link #costScale},
         * because native {@code drawScale} does not carry ART's own 250x350 card box (see
         * {@code Sts1SurfaceRenderer.drawSelectTitle}). Fail-open {@code 175f} when {@code w <= 0}.
         */
        public final float titleOffsetY;
        /**
         * U06: native title font scale = the uniform card scale {@code w / 512f}, multiplied by
         * {@code 0.85f} when the native small-title flag is set. Fail-open {@code 1f} when
         * {@code w <= 0}.
         */
        public final float titleScale;
        /**
         * U06: native {@code renderTitle} GREEN branch ({@code card.upgraded}); when false the title
         * uses the item's existing render colour.
         */
        public final boolean titleGreen;
        /** U06: native {@code renderColor.a} alpha applied to the title. Fail-open {@code 1f}. */
        public final float titleAlpha;
        /** U06: true when {@link #title} is non-null and not whitespace-only. */
        public final boolean titleDrawable;
        /** U06: the projected {@link artframework.context.CardPose#rotation} used to rotate the title. */
        public final float rotation;
        /**
         * U07: the RAW description carried VERBATIM from {@link CardView#description} (native
         * {@code card.rawDescription}); no trim, no keyword stripping, no substitution. Empty for the
         * confirm pseudo-item and for legacy construction.
         */
        public final String description;
        /**
         * U07: true when {@link #description} is non-null and not whitespace-only. False suppresses
         * the whole description block (fail-open: nothing is drawn instead of a bogus line).
         */
        public final boolean descriptionDrawable;
        /**
         * U07: native description font scale = the uniform ART card scale {@code w / 512f} (the SAME
         * value as {@link #costScale}); fail-open {@code 1f} when {@code w} is non-positive.
         */
        public final float descScale;
        /**
         * U07: native description block anchor relative to the card centre, already converted to ART
         * pixels: {@code -(DESC_IMG_HEIGHT / 2f - DESC_IMG_HEIGHT * DESC_OFFSET_Y_RATIO) * descScale}
         * (AbstractCard 1561 minus 2636), i.e. {@code -102.9f * descScale}.
         */
        public final float descAnchorY;
        /** U07: the projected {@link artframework.context.CardPose#scale}; fail-open {@code 1f}. */
        public final float poseScale;
        /**
         * U07: native {@code getDescFont} predicate (AbstractCard 1835-1839:
         * {@code angle == 0.0f && drawScale == 1.0f -> cardDescFont_N else cardDescFont_L}) mirroring
         * {@code rotation == 0f && poseScale == 1f}.
         */
        public final boolean descFontNormal;

        public DrawItem(
                String instanceId,
                String cardId,
                int slot,
                boolean selected,
                boolean visible,
                float x,
                float y,
                boolean confirm) {
            this(instanceId, cardId, cardId, slot, selected, visible, true, x, y,
                    confirm, confirm ? ResourceIds.UI_SELECT_CONFIRM : ResourceIds.UI_SELECT_CARD,
                    confirm ? "" : ResourceIds.UI_SELECT_CARD_FRAME, 250f, 350f);
        }

        public DrawItem(String instanceId, String cardId, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h) {
            this(instanceId, cardId, cardId, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h);
        }

        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h) {
            this(instanceId, cardId, label, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h, "");
        }

        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h,
                String bannerResourceId) {
            this(instanceId, cardId, label, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h, bannerResourceId, "");
        }

        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h,
                String bannerResourceId, String orbResourceId) {
            this(instanceId, cardId, label, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h, bannerResourceId, orbResourceId, false,
                    "", "");
        }

        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h,
                String bannerResourceId, String orbResourceId, boolean costDrawable,
                String costText, String costColor) {
            this(instanceId, cardId, label, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h, bannerResourceId, orbResourceId, costDrawable,
                    costText, costColor, "");
        }

        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h,
                String bannerResourceId, String orbResourceId, boolean costDrawable,
                String costText, String costColor, String portraitResourceId) {
            this(instanceId, cardId, label, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h, bannerResourceId, orbResourceId, costDrawable,
                    costText, costColor, portraitResourceId, null, false, false, 1f, 0f);
        }

        /**
         * U06 full carrier: the title text plus the native {@code renderTitle} inputs are appended
         * AFTER {@code portraitResourceId} so every pre-existing argument order is untouched. All
         * shorter constructors delegate here; the legacy shape carries no title, hence
         * {@code titleDrawable == false} but a fail-open {@code titleOffsetY == 175f} /
         * {@code titleScale == 1f} when {@code w <= 0}.
         *
         * <p>ONE scale authority: the uniform ART card scale {@code w / CARDUI_PAGE}, exactly like
         * {@link #costScale}. The anchor is {@code 175f * thatScale}; ART's scale already absorbs the
         * host {@code Settings.scale}, so no extra multiplier is applied. The small-title flag
         * scales the FONT only ({@code * 0.85f}), never the anchor.
         */
        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h,
                String bannerResourceId, String orbResourceId, boolean costDrawable,
                String costText, String costColor, String portraitResourceId,
                String title, boolean smallTitleFont, boolean titleGreen, float titleAlpha,
                float rotation) {
            this(instanceId, cardId, label, slot, selected, visible, enabled, x, y, confirm,
                    resourceId, frameResourceId, w, h, bannerResourceId, orbResourceId, costDrawable,
                    costText, costColor, portraitResourceId, title, smallTitleFont, titleGreen,
                    titleAlpha, rotation, "", 1f);
        }

        /**
         * U07 full carrier: the RAW description plus the projected pose scale are appended AFTER
         * {@code rotation} so every pre-existing signature and argument order is untouched. All
         * shorter constructors delegate here with {@code description = ""} and {@code poseScale = 1f},
         * which keeps the legacy fail-open shape ({@code descriptionDrawable == false},
         * {@code descScale == 1f}, {@code descAnchorY == -102.9f}).
         *
         * <p>ONE scale authority: {@code descScale} is the existing {@link #costScale} value
         * ({@code w / CARDUI_PAGE}), never the native {@code card.drawScale}; the anchor is the native
         * {@code -(IMG_HEIGHT/2 - DESC_OFFSET_Y)} page-unit offset times that same scale.
         */
        public DrawItem(String instanceId, String cardId, String label, int slot, boolean selected,
                boolean visible, boolean enabled, float x, float y, boolean confirm,
                String resourceId, String frameResourceId, float w, float h,
                String bannerResourceId, String orbResourceId, boolean costDrawable,
                String costText, String costColor, String portraitResourceId,
                String title, boolean smallTitleFont, boolean titleGreen, float titleAlpha,
                float rotation, String description, float poseScale) {
            this.instanceId = instanceId != null ? instanceId : "";
            this.cardId = cardId != null ? cardId : "";
            this.label = label != null && !label.isEmpty() ? label : this.cardId;
            this.slot = slot;
            this.selected = selected;
            this.visible = visible;
            this.x = x;
            this.y = y;
            this.confirm = confirm;
            this.enabled = enabled;
            this.w = w;
            this.h = h;
            this.resourceId = resourceId != null ? resourceId : "";
            this.frameResourceId = frameResourceId != null ? frameResourceId : "";
            this.bannerResourceId = bannerResourceId != null ? bannerResourceId : "";
            this.orbResourceId = orbResourceId != null ? orbResourceId : "";
            this.portraitResourceId = portraitResourceId != null ? portraitResourceId : "";
            this.costDrawable = costDrawable;
            this.costText = costText != null ? costText : "";
            this.costColor = costColor != null ? costColor : "";
            // ONE authority for the native cost-number geometry: the same uniform card scale the
            // region placement uses (bounds.width / region.originalWidth), where every cardui region
            // has originalWidth 512. Fail-open 1f when the box has no width.
            this.costScale = w > 0f ? w / CARDUI_PAGE : 1f;
            this.costOffsetX = COST_OFFSET_X * costScale;
            this.costOffsetY = COST_OFFSET_Y * costScale;
            // U06: the native renderTitle anchor uses the SAME uniform card scale (NOT native
            // card.drawScale - see Sts1SurfaceRenderer.drawSelectTitle for the derivation). The
            // small-title flag multiplies the FONT scale only; the anchor stays put. Both values
            // share the ONE fail-open above: with no width costScale is already 1f, hence
            // titleScale 1f and titleOffsetY 175f.
            this.titleScale = smallTitleFont ? costScale * SMALL_TITLE_SCALE : costScale;
            this.titleOffsetY = TITLE_OFFSET_Y * costScale;
            this.title = title != null ? title : "";
            this.titleDrawable = title != null && !title.trim().isEmpty();
            this.titleGreen = titleGreen;
            this.titleAlpha = titleAlpha;
            this.rotation = rotation;
            // U07: the description is carried raw; drawability is whitespace-only suppression. The
            // anchor applies native AbstractCard 1561/2636's -102.9 page units at the SAME uniform
            // card scale as the cost/title geometry.
            this.description = description != null ? description : "";
            this.descriptionDrawable = description != null && !description.trim().isEmpty();
            this.descScale = costScale;
            this.descAnchorY = descAnchorYUnits() * this.descScale;
            this.poseScale = poseScale;
            // Native getDescFont (1835-1839): the NORMAL font only for an unrotated, unit-scale card.
            this.descFontNormal = rotation == 0f && poseScale == 1f;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("instanceId", instanceId);
            m.put("cardId", cardId);
            m.put("label", label);
            m.put("slot", Integer.valueOf(slot));
            m.put("selected", Boolean.valueOf(selected));
            m.put("visible", Boolean.valueOf(visible));
            m.put("x", Float.valueOf(x));
            m.put("y", Float.valueOf(y));
            m.put("confirm", Boolean.valueOf(confirm));
            m.put("enabled", Boolean.valueOf(enabled));
            m.put("w", Float.valueOf(w));
            m.put("h", Float.valueOf(h));
            m.put("resourceId", resourceId);
            m.put("frameResourceId", frameResourceId);
            m.put("bannerResourceId", bannerResourceId);
            m.put("orbResourceId", orbResourceId);
            m.put("portraitResourceId", portraitResourceId);
            m.put("costDrawable", Boolean.valueOf(costDrawable));
            m.put("costText", costText);
            m.put("costColor", costColor);
            m.put("costOffsetX", Float.valueOf(costOffsetX));
            m.put("costOffsetY", Float.valueOf(costOffsetY));
            m.put("costScale", Float.valueOf(costScale));
            // U06: additive evidence keys for the native title geometry; no existing key renamed.
            m.put("title", title);
            m.put("titleOffsetY", Float.valueOf(titleOffsetY));
            m.put("titleScale", Float.valueOf(titleScale));
            m.put("titleGreen", Boolean.valueOf(titleGreen));
            m.put("titleAlpha", Float.valueOf(titleAlpha));
            m.put("titleDrawable", Boolean.valueOf(titleDrawable));
            m.put("rotation", Float.valueOf(rotation));
            // U07: additive evidence keys for the native description geometry; no existing key
            // renamed or dropped.
            m.put("description", description);
            m.put("descriptionDrawable", Boolean.valueOf(descriptionDrawable));
            m.put("descScale", Float.valueOf(descScale));
            m.put("descAnchorY", Float.valueOf(descAnchorY));
            m.put("descFontNormal", Boolean.valueOf(descFontNormal));
            m.put("descMaxLines", Integer.valueOf(DESC_MAX_LINES));
            return m;
        }
    }

    private SelectDrawPath() {}

    public static boolean shouldSuppressNativeSelect() {
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        return plan.shouldSuppressNative(SurfaceIds.SELECT_GRID)
                || plan.shouldSuppressNative(SurfaceIds.SELECT_HAND);
    }

    public static boolean shouldSuppressNativeGrid() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.SELECT_GRID);
    }

    public static boolean shouldSuppressNativeHandSelect() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.SELECT_HAND);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        SelectView sv = ArtFramework.projection().select();
        int i = 0;
        for (CardView c : sv.pool) {
            boolean selected = c.selected || sv.isSelected(c.ref.instanceId);
            float x = c.pose != null ? c.pose.x : defaultCardX(i, sv.poolCount());
            float y = c.pose != null ? c.pose.y : defaultCardY();
            boolean costDrawable = c.orbDrawable && costTextGate(c.type, c.color, c.ref.cardId);
            out.add(
                    new DrawItem(
                            c.ref.instanceId,
                            c.ref.cardId,
                            c.title,
                            c.slotIndex,
                            selected,
                            c.pose == null || c.pose.visible,
                            c.playable,
                            x,
                            y,
                            false,
                            cardResource(selected, c.playable),
                            cardFrameDescriptor(c.type, c.color),
                            CARD_WIDTH,
                            CARD_HEIGHT,
                            cardBannerDescriptor(c.rarity),
                            c.orbDrawable ? cardOrbDescriptor(c.color) : "",
                            costDrawable,
                            costDrawable ? c.cost : "",
                            costDrawable ? costColorName(c.costModified, c.costRestricted) : "",
                            portraitDescriptor(c.artResourceId),
                            // U06: the native renderTitle inputs. The title is passed VERBATIM (the
                            // item decides drawability); the anchor/scale authority is item.w, and
                            // the rotation comes from the projected pose so a tilted select card
                            // keeps its native title angle.
                            c.title,
                            c.smallTitleFont,
                            c.upgraded,
                            c.titleAlpha,
                            c.pose != null ? c.pose.rotation : 0f,
                            // U07: the RAW description (native card.rawDescription) and the
                            // projected pose scale that mirrors the native getDescFont predicate.
                            c.description,
                            c.pose != null ? c.pose.scale : 1f));
            i++;
        }
        if (sv.confirmVisible || sv.confirmEnabled) {
            out.add(confirmItem());
        }
        return out;
    }

    /**
     * Derives the select card BASE-BACKGROUND frame descriptor from the card's native type/color,
     * mirroring native {@code AbstractCard.renderCardBg} and its {@code render*Bg} color switch:
     * <ul>
     *   <li>type {@code attack} -> the {@code attack} family, {@code skill} -> {@code skill},
     *       {@code power} -> {@code power}, and {@code curse}/{@code status} -> the {@code skill}
     *       family. Native {@code renderCardBg} has NO default case, so an unknown/absent/blank TYPE
     *       draws NO background and yields {@code ""}.</li>
     *   <li>color {@code red|green|blue|purple} -> {@code cardui#512/bg_<family>_<color>}.</li>
     *   <li>color {@code colorless} -> {@code cardui#512/bg_<family>_gray}.</li>
     *   <li>color {@code curse} or any other/absent/blank color -> the native {@code render*Bg}
     *       DEFAULT branch, {@code cardui#512/bg_skill_black} (family-independent).</li>
     * </ul>
     * Type/color are matched case-insensitively. Fail-open: any failure yields {@code ""} (no frame
     * draw instead of a bogus one).
     */
    static String cardFrameDescriptor(String type, String color) {
        try {
            String t = type != null ? type.trim().toLowerCase(java.util.Locale.ROOT) : "";
            // curse/status draw on the skill page; attack/skill/power keep their own family name.
            String family = "curse".equals(t) || "status".equals(t) ? "skill" : t;
            if (!"attack".equals(family) && !"skill".equals(family) && !"power".equals(family)) {
                // No native default case: an unknown/absent type draws NO background.
                return "";
            }
            String mapped = mappedColor(color);
            if (mapped.isEmpty()) {
                // Native render*Bg DEFAULT branch: the black skill page, family-independent.
                return "cardui#512/bg_skill_black";
            }
            return "cardui#512/bg_" + family + "_" + mapped;
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * U02d: derives the select card BANNER descriptor from the native card rarity, mirroring native
     * {@code AbstractCard.renderBannerImage}: {@code RARE} -> {@code 512/banner_rare},
     * {@code UNCOMMON} -> {@code 512/banner_uncommon}, and BASIC/COMMON/CURSE/unknown/absent -> the
     * native default {@code 512/banner_common}. Rarity is matched case-insensitively. The result is
     * a {@code "<atlasKey>#<region>"} descriptor over the SAME {@code cardui} atlas as the base
     * background, drawn as an ADDITIONAL region (never part of the resolve-aware accounting).
     * Fail-open: any failure yields the native common banner.
     */
    static String cardBannerDescriptor(String rarity) {
        try {
            String r = rarity != null ? rarity.trim().toUpperCase(java.util.Locale.ROOT) : "";
            if ("RARE".equals(r)) {
                return "cardui#512/banner_rare";
            }
            if ("UNCOMMON".equals(r)) {
                return "cardui#512/banner_uncommon";
            }
            return "cardui#512/banner_common";
        } catch (Throwable ignored) {
            return "cardui#512/banner_common";
        }
    }

    /**
     * U04: derives the select card PORTRAIT descriptor from the projected art value
     * ({@link CardView#artResourceId}, which for GRID select cards carries the native
     * {@code AbstractCard.assetUrl}). The native portrait page is {@code cards/cards.atlas}, whose
     * region names ARE the native card art urls ({@code red/attack/strike}) - the exact string
     * native {@code AbstractCard} hands to {@code cardAtlas.findRegion(...)} - so a plain art url
     * maps to {@code cards#<artUrl>}.
     *
     * <ul>
     *   <li>a plain art url {@code red/attack/strike} -&gt; {@code cards#red/attack/strike};</li>
     *   <li>an already-qualified {@code cards#red/attack/strike} -&gt; passed through UNCHANGED
     *       (never double-prefixed);</li>
     *   <li>blank/absent, a non-art-url value (a logical {@code card.art.<id>} id, a
     *       {@code ui.*} id, the literal {@code "null"}), or any failure -&gt; {@code ""} so no
     *       portrait is drawn rather than a bogus one.</li>
     * </ul>
     *
     * <p>The admitted shape is the one {@link #isNativeCardArtUrl} documents; that check is what
     * separates a native art url from an arbitrary resource id.
     *
     * <p>This is an ADDITIONAL drawn region outside the U02c accounting; the caller never requires
     * it to resolve. Fail-open: this method never throws.
     */
    static String portraitDescriptor(String artResourceId) {
        try {
            if (artResourceId == null) {
                return "";
            }
            String value = artResourceId.trim();
            if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                return "";
            }
            int hash = value.indexOf('#');
            if (hash > 0) {
                // Already "<atlasKey>#<region>": pass through, but only when the key is our page
                // and a region name is actually present (a bare "cards#" is degenerate).
                return "cards".equals(value.substring(0, hash))
                                && !value.substring(hash + 1).trim().isEmpty()
                        ? value
                        : "";
            }
            if (hash == 0 || !isNativeCardArtUrl(value)) {
                return "";
            }
            return "cards#" + value;
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * True when {@code value} has the native {@code cards/cards.atlas} region-name shape: two or
     * three {@code [A-Za-z0-9_]} segments separated by {@code /} - {@code <color>/<type>/<card>} for
     * most cards (for example {@code red/attack/strike}) and {@code <type>/<card>} for the status and
     * curse pages (for example {@code status/burn}, {@code curse/pride}). Every region on the
     * shipped page matches this (vanilla names are lowercase; a mod may use mixed case, which is
     * kept verbatim because {@code findRegion} is case-sensitive), and no ART logical id
     * ({@code card.art.Strike_R}, {@code ui.select.card} - both dot-separated) does. Null-safe;
     * never throws.
     */
    private static boolean isNativeCardArtUrl(String value) {
        if (value == null) {
            return false;
        }
        String[] parts = value.split("/", -1);
        if (parts.length < 2 || parts.length > 3) {
            return false;
        }
        for (int i = 0; i < parts.length; i++) {
            if (!isArtUrlSegment(parts[i])) {
                return false;
            }
        }
        return true;
    }

    /** True when {@code token} is a non-empty {@code [A-Za-z0-9_]} run (no dots, spaces or colons). */
    private static boolean isArtUrlSegment(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || ch == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * U02d2: derives the select card ENERGY ORB descriptor from the native card color, mirroring
     * native {@code AbstractCard.renderEnergy}: RED/GREEN/BLUE/PURPLE map to
     * {@code 512/card_<color>_orb}, and the native DEFAULT (COLORLESS, CURSE, any other/unknown/
     * blank/absent color) maps to {@code 512/card_colorless_orb}. Color is matched
     * case-insensitively. The result is a {@code "<atlasKey>#<region>"} descriptor over the SAME
     * {@code cardui} atlas as the base background and banner, drawn as an ADDITIONAL region (never
     * part of the resolve-aware accounting). No cost gating here; the cost gate lives on
     * {@link CardView#orbDrawable}. Fail-open: any failure yields the native colorless orb.
     */
    static String cardOrbDescriptor(String color) {
        try {
            String c = color != null ? color.trim().toUpperCase(java.util.Locale.ROOT) : "";
            if ("RED".equals(c)) {
                return "cardui#512/card_red_orb";
            }
            if ("GREEN".equals(c)) {
                return "cardui#512/card_green_orb";
            }
            if ("BLUE".equals(c)) {
                return "cardui#512/card_blue_orb";
            }
            if ("PURPLE".equals(c)) {
                return "cardui#512/card_purple_orb";
            }
            return "cardui#512/card_colorless_orb";
        } catch (Throwable ignored) {
            return "cardui#512/card_colorless_orb";
        }
    }

    /**
     * U02d3: native {@code AbstractCard.renderEnergy} text gate for the cost NUMBER (the orb is
     * unaffected): {@code (type != STATUS || cardID == "Slimed") && (color != CURSE || cardID ==
     * "Pride")}. A blank/unknown type or colour satisfies its clause (fail-open: the number draws).
     * Matching is case-insensitive and null-safe.
     */
    static boolean costTextGate(String type, String color, String cardId) {
        boolean typeOk = !is(type, "STATUS") || "Slimed".equals(cardId);
        boolean colorOk = !is(color, "CURSE") || "Pride".equals(cardId);
        return typeOk && colorOk;
    }

    /**
     * U02d3: the native cost-number colour NAME. Native {@code renderEnergy} checks the RESTRICTED
     * branch FIRST ({@code player != null && player.hand.contains(card) && !hasEnoughEnergy()} ->
     * {@code ENERGY_COST_RESTRICTED_COLOR} {@code (1, 0.3, 0.3)}), else the MODIFIED branch
     * ({@code isCostModified || isCostModifiedForTurn || freeToPlay()} ->
     * {@code ENERGY_COST_MODIFIED_COLOR} {@code (0.4, 1, 0.4)}), else white.
     */
    static String costColorName(boolean modified, boolean restricted) {
        if (restricted) {
            return "red";
        }
        if (modified) {
            return "green";
        }
        return "white";
    }

    /**
     * U02d3: the native cost-number RGBA. {@code "green"} is {@code ENERGY_COST_MODIFIED_COLOR}
     * {@code (0.4, 1, 0.4, 1)}, {@code "red"} is {@code ENERGY_COST_RESTRICTED_COLOR}
     * {@code (1, 0.3, 0.3, 1)}, and any other (null/blank/unknown) name fails open to white
     * {@code (1, 1, 1, 1)}.
     */
    static float[] costColorRgba(String colorName) {
        if ("green".equals(colorName)) {
            return new float[] {0.4f, 1f, 0.4f, 1f};
        }
        if ("red".equals(colorName)) {
            return new float[] {1f, 0.3f, 0.3f, 1f};
        }
        return new float[] {1f, 1f, 1f, 1f};
    }

    /**
     * Case-insensitive, null-safe native enum-name comparison used by {@link #costTextGate}; a
     * null/blank {@code value} never equals a non-blank {@code expected}.
     */
    private static boolean is(String value, String expected) {
        return value != null && value.trim().equalsIgnoreCase(expected);
    }

    /**
     * Maps a native card color name to the cardui base-background color token, or {@code ""} for the
     * native {@code render*Bg} DEFAULT branch (caller then draws {@code 512/bg_skill_black}).
     */
    private static String mappedColor(String color) {
        if (color == null) {
            return "";
        }
        String c = color.trim().toLowerCase(java.util.Locale.ROOT);
        if ("red".equals(c)) return "red";
        if ("green".equals(c)) return "green";
        if ("blue".equals(c)) return "blue";
        if ("purple".equals(c)) return "purple";
        if ("colorless".equals(c)) return "gray";
        return "";
    }

    /**
     * U07: the native description block ANCHOR in ART pixels, i.e. native AbstractCard 1561
     * {@code draw_y = current_y - IMG_HEIGHT * drawScale / 2.0f + DESC_OFFSET_Y * drawScale} with the
     * ART uniform scale {@code descScale = w / 512f} standing in for {@code drawScale}:
     * {@code (DESC_IMG_HEIGHT / 2f - DESC_IMG_HEIGHT * DESC_OFFSET_Y_RATIO) = 210 - 107.1 = 102.9}
     * page units below the card centre, times {@code descScale}. For the real 250-wide select item
     * that is {@code -102.9 * 0.48828125 = -50.244140625}. Fail-open: never throws.
     */
    public static float descriptionAnchorY(float centerY, float descScale) {
        return centerY + descAnchorYUnits() * descScale;
    }

    /** U07: {@code -(DESC_IMG_HEIGHT / 2f - DESC_IMG_HEIGHT * DESC_OFFSET_Y_RATIO)} = {@code -102.9f}. */
    private static float descAnchorYUnits() {
        return -(DESC_IMG_HEIGHT / 2f - DESC_IMG_HEIGHT * DESC_OFFSET_Y_RATIO);
    }

    /**
     * U07: the FIRST line's y, adding the native AbstractCard 1562 first-baseline shift
     * {@code description.size() * capHeight * 0.775f - capHeight * 0.375f} to
     * {@link #descriptionAnchorY}. {@code capHeight} is the LIVE scaled font metric read by the
     * renderer after {@code setScale(descScale)} - never a hardcoded font metric. For
     * {@code (500f, 0.48828125f, 1, 10f)} this is {@code 500 - 50.244140625 + 4 = 453.755859375}.
     * Fail-open: never throws.
     */
    public static float descriptionFirstLineY(
            float centerY, float descScale, int lineCount, float capHeight) {
        return descriptionAnchorY(centerY, descScale)
                + (DESC_LINE_COUNT_FACTOR * lineCount - DESC_LINE_COUNT_OFFSET) * capHeight;
    }

    /**
     * U07: the per-line vertical advance for line index {@code i}: native AbstractCard 1563
     * {@code 1.45f * -capHeight} (the sign is dropped because ART walks DOWN the lines itself). For
     * {@code capHeight = 10} this is {@code 14.5}.
     */
    public static float descriptionLineAdvance(float capHeight) {
        return DESC_LINE_ADVANCE_FACTOR * capHeight;
    }

    /**
     * U07: true when {@code raw} contains at least one HAN, HIRAGANA, KATAKANA or HANGUL code point,
     * which selects the CJK by-CHARACTER wrap (native {@code initializeDescriptionCN} does not wrap
     * CJK on spaces). Null/blank is false; never throws.
     */
    public static boolean isCjkText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            Character.UnicodeScript script;
            try {
                script = Character.UnicodeScript.of(cp);
            } catch (Throwable ignored) {
                return false;
            }
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    /**
     * U07c: replaces each WHITESPACE-DELIMITED WHOLE token that is exactly equal to {@code !D!},
     * {@code !B!} or {@code !M!} with {@link #DYNAMIC_VALUE_PLACEHOLDER}, mirroring the native
     * whole-token equality tests in {@code AbstractCard.initializeDescriptionCN} (L559
     * {@code word.equals("!D!")}, L572 {@code !B!}, L585 {@code !M!}) and its single-character
     * value slot. This is deliberately NOT a substring {@code replace("!D!", "?")}: a token that
     * merely CONTAINS a marker ({@code !DX!}, {@code !!D!}, {@code x!D!}, {@code !D!!M!}), a
     * different casing ({@code !d!}), or a star-wrapped {@code *!D!*} is ONE token that never equals
     * a marker and therefore stays LITERAL.
     *
     * <p>Every other character is preserved VERBATIM, including the whitespace around a substituted
     * token ({@code " !D! "} -&gt; {@code " ? "}), so this pure step can run BEFORE wrapping without
     * disturbing the {@code NL} forced break (U07b) or the {@code '*'} keyword toggle (U07).
     *
     * <p>Fail-open, never throws: {@code null} yields {@code null}; an empty or token-free input is
     * returned unchanged.
     */
    public static String substituteDynamicMarkers(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.isEmpty()) {
            return raw;
        }
        try {
            StringBuilder out = new StringBuilder(raw.length());
            StringBuilder token = new StringBuilder();
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (!isDescriptionWhitespace(c)) {
                    token.append(c);
                    continue;
                }
                appendDynamicSubstituted(out, token);
                out.append(c);
            }
            appendDynamicSubstituted(out, token);
            return out.toString();
        } catch (Throwable ignored) {
            return raw;
        }
    }

    /**
     * U07c: flushes one completed whitespace-delimited {@code token} into {@code out}, writing the
     * value-free {@link #DYNAMIC_VALUE_PLACEHOLDER} when the token is exactly one of the three native
     * dynamic markers and the token text itself otherwise. Clears {@code token} either way.
     */
    private static void appendDynamicSubstituted(StringBuilder out, StringBuilder token) {
        if (token.length() == 0) {
            return;
        }
        String word = token.toString();
        token.setLength(0);
        if ("!D!".equals(word) || "!B!".equals(word) || "!M!".equals(word)) {
            out.append(DYNAMIC_VALUE_PLACEHOLDER);
            return;
        }
        out.append(word);
    }

    /**
     * U07c: true for exactly the characters Java's regex {@code \s} matches (space, {@code \t},
     * {@code \n}, {@code \x0B}, {@code \f}, {@code \r}) - the same whitespace set
     * {@link #wrapDescription} splits on with {@code "\\s+"}, so the token boundaries used by
     * {@link #substituteDynamicMarkers} and by the wrap agree.
     */
    private static boolean isDescriptionWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    /**
     * U07: deterministic wrap of the RAW description into drawable lines.
     *
     * <p><b>Dynamic values</b> (U07c): the raw text first passes through
     * {@link #substituteDynamicMarkers}, so the native markers {@code !D!}/{@code !B!}/{@code !M!}
     * become the single-character {@link #DYNAMIC_VALUE_PLACEHOLDER} BEFORE any wrapping. The
     * placeholder therefore participates in the wrap width, and no raw marker is ever painted.
     *
     * <p><b>CJK mode</b> ({@link #isCjkText}): ALL whitespace is collapsed to nothing and the text is
     * cut greedily by CODE POINT into chunks of at most {@code maxCharsPerLine}.
     *
     * <p><b>Otherwise</b>: greedy WORD wrap on single spaces, rejoined with exactly one space. A word
     * longer than {@code maxCharsPerLine} is kept WHOLE on its own line (never split mid-word).
     *
     * <p><b>Manual line break</b>: a whole token equal to the exact, case-sensitive native token
     * {@code NL} is a FORCED break, not text (native {@code initializeDescription} AbstractCard 454
     * {@code else if (word.equals("NL"))} and {@code initializeDescriptionCN} 494/598). Text before
     * the token keeps the wrap above; text after it starts a NEW line. Breaks while the pending line
     * is empty are no-ops, so no empty line is ever emitted.
     *
     * <p>Finally lines beyond {@code maxLines} are DROPPED: this is TRUNCATION, not an ellipsis
     * (vanilla native does not shrink - it only logs a dev warning, AbstractCard 481-482 - and
     * overflows; the five-line shrink is BaseMod's {@code ShrinkLongDescription} patch, deliberately
     * not mirrored because ART does not depend on BaseMod). A forced break never bypasses the cap.
     *
     * <p>Fail-open: a null/blank/whitespace-only {@code raw} yields an EMPTY list, and a
     * non-positive {@code maxCharsPerLine}/{@code maxLines} yields an EMPTY list rather than throwing.
     * The result is never null; this method never throws.
     */
    public static List<String> wrapDescription(String raw, int maxCharsPerLine, int maxLines) {
        List<String> out = new ArrayList<String>();
        if (raw == null || raw.trim().isEmpty() || maxCharsPerLine <= 0 || maxLines <= 0) {
            return out;
        }
        try {
            // U07c: substitute the native dynamic-value markers BEFORE wrapping so the one-character
            // placeholder takes part in the wrap width. NL (U07b) and '*' (U07) are untouched.
            String text = substituteDynamicMarkers(raw);
            if (text == null) {
                return out;
            }
            String[] words = text.trim().split("\\s+");
            if (isCjkText(text)) {
                StringBuilder segment = new StringBuilder();
                for (String word : words) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    if ("NL".equals(word)) {
                        appendCjkSegment(out, segment, maxCharsPerLine, maxLines);
                        segment.setLength(0);
                        if (out.size() >= maxLines) {
                            return out;
                        }
                        continue;
                    }
                    segment.append(word);
                }
                appendCjkSegment(out, segment, maxCharsPerLine, maxLines);
                return out;
            }
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
                if ("NL".equals(word)) {
                    if (current.length() > 0) {
                        out.add(current.toString());
                        current.setLength(0);
                        if (out.size() >= maxLines) {
                            return out;
                        }
                    }
                    continue;
                }
                if (current.length() == 0) {
                    current.append(word);
                } else if (current.length() + 1 + word.length() <= maxCharsPerLine) {
                    current.append(' ').append(word);
                } else {
                    out.add(current.toString());
                    current.setLength(0);
                    if (out.size() >= maxLines) {
                        return out;
                    }
                    current.append(word);
                }
            }
            if (current.length() > 0 && out.size() < maxLines) {
                out.add(current.toString());
            }
            return out;
        } catch (Throwable ignored) {
            return new ArrayList<String>();
        }
    }

    /**
     * U07: cuts ONE whitespace-collapsed CJK break segment by CODE POINT into chunks of at most
     * {@code maxCharsPerLine}, dropping everything past {@code maxLines}. With no {@code NL} token
     * there is exactly one segment, so this is observationally identical to a global collapse/cut.
     */
    private static void appendCjkSegment(
            List<String> out, CharSequence segment, int maxCharsPerLine, int maxLines) {
        int i = 0;
        int n = segment.length();
        while (i < n && out.size() < maxLines) {
            int end = i;
            int count = 0;
            while (end < n && count < maxCharsPerLine) {
                end += Character.charCount(Character.codePointAt(segment, end));
                count++;
            }
            out.add(segment.subSequence(i, end).toString());
            i = end;
        }
    }

    /**
     * U07: splits ONE already-wrapped description line into drawable chunks with native keyword
     * colouring. Native AbstractCard 1567/1576 draws a {@code '*'} fragment in {@code goldColor}
     * with the star stripped and everything else in {@code textColor} (1648); ART's accepted contract
     * uses TOGGLE semantics: each {@code '*'}' toggles keyword mode and is never emitted, so
     * {@code "Deal *5* damage"} = normal {@code "Deal "}, keyword {@code "5"}, normal {@code " "}.
     * An unterminated toggle ({@code "Deal *5 damage"}) simply stays in keyword mode - no throw.
     * Empty fragments are dropped; {@code null}/empty yields an EMPTY list; never returns null.
     */
    public static List<DescriptionChunk> tokenizeDescriptionLine(String line) {
        List<DescriptionChunk> out = new ArrayList<DescriptionChunk>();
        if (line == null || line.isEmpty()) {
            return out;
        }
        try {
            boolean keyword = false;
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '*') {
                    if (buf.length() > 0) {
                        out.add(new DescriptionChunk(buf.toString(), keyword));
                        buf.setLength(0);
                    }
                    keyword = !keyword;
                } else {
                    buf.append(c);
                }
            }
            if (buf.length() > 0) {
                out.add(new DescriptionChunk(buf.toString(), keyword));
            }
            return out;
        } catch (Throwable ignored) {
            return new ArrayList<DescriptionChunk>();
        }
    }

    /**
     * The native select-screen confirm button item, or {@code null} when the confirm control is
     * neither visible nor enabled in the current projection.
     *
     * <p><b>Verified native geometry</b> ({@code CardSelectConfirmButton.renderButton} bytecode):
     * {@code TAKE_Y = 475f * Settings.scale} and the enabled/disabled draw is
     * {@code sb.draw(texture, Settings.WIDTH/2f - 256f, TAKE_Y - 128f, 256f, 128f, 512f, 256f,
     * Settings.scale, Settings.scale, 0f, 0, 0, 512, 256, false, false)}. With libGDX origin
     * scaling both axes scale around the texture centre, so the rendered button CENTRE is
     * {@code (WIDTH/2f, 475f*scale)} and its SIZE is {@code (512f*scale, 256f*scale)};
     * {@code DrawItem.x/y} use the SAME CENTER convention as the card items, so the native
     * top-left {@code (WIDTH/2 - 256*scale, 475*scale - 128*scale)} falls out of the renderer's
     * {@code x - w/2, y - h/2}. At {@code scale=1, WIDTH=1920} that is centre {@code (960, 475)},
     * size {@code 512x256}. All {@code Settings} reads fail-open (defaults {@code WIDTH=1920},
     * {@code scale=1}) so an uninitialized host still yields native-at-unit-scale geometry.
     */
    public static DrawItem confirmItem() {
        SelectView sv = ArtFramework.projection().select();
        if (!sv.confirmVisible && !sv.confirmEnabled) {
            return null;
        }
        float[] cs = confirmSettings();
        float width = cs[0];
        float scale = cs[1];
        return new DrawItem(
                "confirm",
                "Confirm",
                resolvedConfirmLabel(sv),
                -1,
                false,
                sv.confirmVisible,
                sv.confirmEnabled,
                width * 0.5f,
                CONFIRM_TAKE_Y * scale,
                true,
                sv.confirmEnabled ? ResourceIds.UI_SELECT_CONFIRM
                        : ResourceIds.UI_SELECT_CONFIRM_DISABLED,
                "",
                CONFIRM_TEXTURE_W * scale,
                CONFIRM_TEXTURE_H * scale);
    }

    /**
     * Resolved confirm-button label: the projection's LIVE localized {@link SelectView#confirmLabel}
     * when non-blank, else the neutral English {@code "Confirm"} fallback. The fallback (rather than
     * an empty string) keeps the confirm item's drawn text non-degenerate on an English host and
     * mirrors the previous hardcoded literal exactly when no localized label is projected.
     */
    public static String resolvedConfirmLabel(SelectView sv) {
        if (sv != null && sv.confirmLabel != null && !sv.confirmLabel.trim().isEmpty()) {
            return sv.confirmLabel;
        }
        return "Confirm";
    }

    /** Native {@code TAKE_Y} base (unscaled): {@code 475f * Settings.scale}. */
    private static final float CONFIRM_TAKE_Y = 475f;
    /** Native confirm texture draw width in pixels (unscaled): {@code 512f}. */
    private static final float CONFIRM_TEXTURE_W = 512f;
    /** Native confirm texture draw height in pixels (unscaled): {@code 256f}. */
    private static final float CONFIRM_TEXTURE_H = 256f;

    /**
     * Live {@code Settings} values {@code {WIDTH, scale}} used by the native confirm geometry,
     * fail-open to {@code {1920, 1}} when the host type is unavailable or not yet initialized
     * (any value {@code <= 0}). Mirrors {@code RewardDrawPath.sheetSettings} fail-open, but defaults
     * rather than declining so the confirm button is never a zero-size degenerate draw.
     */
    private static float[] confirmSettings() {
        float width = 1920f;
        float scale = 1f;
        try {
            float w = com.megacrit.cardcrawl.core.Settings.WIDTH;
            float s = com.megacrit.cardcrawl.core.Settings.scale;
            if (w > 0f) {
                width = w;
            }
            if (s > 0f) {
                scale = s;
            }
        } catch (Throwable ignored) {
        }
        return new float[] {width, scale};
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        SelectView sv = ArtFramework.projection().select();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", Integer.valueOf(visibleCount(items)));
        // RESOLVE-AWARE supply accounting (mirrors MapDrawPath / HandDrawPath.missingArt): a visible
        // item counts as materializedResolved only when every resource the renderer would actually
        // draw resolves to a file-backed (`images/`) path; otherwise it is a missingArt gap. This is
        // prefix-level, not file-existence/texture-load (see resolvesToDrawable). `count` above
        // remains the visible-item draw-SUBMISSION count and is intentionally not resolve-aware.
        int missingArt = 0;
        int materializedResolved = 0;
        for (DrawItem item : items) {
            if (!item.visible) continue;
            if (itemFullyResolves(item)) materializedResolved++; else missingArt++;
        }
        m.put("missingArt", Integer.valueOf(missingArt));
        m.put("materializedResolved", Integer.valueOf(materializedResolved));
        m.put("kind", sv.kind);
        m.put("available", Boolean.valueOf(sv.available));
        m.put("poolCount", Integer.valueOf(sv.poolCount()));
        m.put("selectedCount", Integer.valueOf(sv.selectedInstanceIds.size()));
        m.put("confirmEnabled", Boolean.valueOf(sv.confirmEnabled));
        m.put("confirmVisible", Boolean.valueOf(sv.confirmVisible));
        m.put("suppressNativeSelect", Boolean.valueOf(shouldSuppressNativeSelect()));
        m.put("presentLevel", FullPresentMode.selectLevel().name());
        // NRO-04 D09: expose the native confirm-button geometry as its own sub-map (kept out of
        // items[] so the public item-list semantics are unchanged). x/y use the SAME CENTER
        // convention as items[]; the renderer's x - w/2, y - h/2 yields the native top-left.
        DrawItem confirm = confirmItem();
        if (confirm != null) {
            Map<String, Object> c = new LinkedHashMap<String, Object>();
            c.put("resourceId", confirm.resourceId);
            c.put("x", Float.valueOf(confirm.x));
            c.put("y", Float.valueOf(confirm.y));
            c.put("w", Float.valueOf(confirm.w));
            c.put("h", Float.valueOf(confirm.h));
            c.put("enabled", Boolean.valueOf(confirm.enabled));
            c.put("visible", Boolean.valueOf(confirm.visible));
            c.put("label", confirm.label);
            m.put("confirm", c);
        } else {
            m.put("confirm", null);
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        return m;
    }

    /**
     * The draw-SUBMISSION count recorded against the select surface (visible projected items). It is
     * intentionally NOT resolve-aware: {@code probeSlice().missingArt} is the honest resolve-aware
     * supply gap for the same frame.
     */
    public static int materializedDrawCount() { return visibleCount(buildFromProjection()); }

    /**
     * A non-confirm card item is decided by the type/color-derived base-background frame descriptor
     * ALONE ({@link #frameDescriptorResolves}). For an ENABLED or SELECTED card the item's primary
     * {@code resourceId} is a logical select handle ({@code ui.select.card}/{@code _SELECTED})
     * mapping to the non-file-backed {@code cardui/frame} region, NOT a distinct drawn texture.
     * Native {@code Sts1SurfaceRenderer.renderSelect} draws exactly one base bg per card - the
     * derived descriptor region - and its {@code drawResolvedTexture(sb, item.resourceId, b)} call is
     * a no-op for those ids, so also requiring the primary would double-count the same native draw.
     *
     * <p>A DISABLED card's primary is different: {@code ui.select.card.disabled} is a file-backed
     * {@code images/ui/event/disabledButton.png} overlay that {@code renderSelect} DOES draw as a
     * second real texture alongside the frame. It is therefore a required draw in its own right:
     * this method also requires {@link #resolvesToDrawable} for it, so a regression of the overlay
     * mapping to a non-file-backed id is reported as a {@code missingArt} gap rather than silently
     * accepted.
     *
     * <p>A blank frame descriptor is NOT materialized (the card is missing its base background) and
     * stays a gap. The confirm pseudo-item keeps the existing {@link #resolvesToDrawable} primary
     * semantics.
     */
    private static boolean itemFullyResolves(DrawItem item) {
        if (!item.confirm && !item.cardId.isEmpty()) {
            if (!frameDescriptorResolves(item.frameResourceId)) return false;
            // U02e: a DISABLED card's primary is a REAL second draw - the file-backed disabled
            // overlay that renderSelect draws via drawResolvedTexture(item.resourceId). Require it
            // so the probe contract "every drawn resource resolves" holds for the overlay too.
            if (!item.enabled && !resolvesToDrawable(item.resourceId)) return false;
            return true;
        }
        return resolvesToDrawable(item.resourceId);
    }

    /**
     * True when the frame descriptor resolves. A {@code "<atlasKey>#<region>"} descriptor resolves
     * through {@link artframework.sts1.assets.Sts1AtlasMaterializer#regionFromAtlas}; any other
     * (non-{@code #}) id uses {@link #resolvesToDrawable}. A null/blank descriptor never resolves.
     * Fail-open: any resolution failure yields {@code false} rather than throwing.
     */
    static boolean frameDescriptorResolves(String frameResourceId) {
        if (frameResourceId == null || frameResourceId.isEmpty()) return false;
        int hash = frameResourceId.indexOf('#');
        if (hash < 0) {
            return resolvesToDrawable(frameResourceId);
        }
        String atlasKey = frameResourceId.substring(0, hash);
        String regionName = frameResourceId.substring(hash + 1);
        try {
            return artframework.sts1.assets.Sts1AtlasMaterializer
                    .regionFromAtlas(atlasKey, regionName) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True when the resolver yields a source that maps to a file-backed ({@code images/}) path, i.e.
     * the prefix-level shape {@link artframework.sts1.assets.Sts1AssetMaterializer#isFileBacked} accepts.
     * This is deliberately NOT a file-existence or texture-load check: a mapping to an absent
     * {@code images/} file still yields {@code true} here, so {@code materializedResolved} can
     * over-count such an item. It proves only that the source is not a non-file-backed atlas/logical
     * key (which is the gap this accounting targets), so missing {@code images/} files are a known
     * under-counted gap.
     *
     * <p>This is the non-{@code #} frame path only. The select card base-background frame is now a
     * real {@code cardui#512/bg_<type>_<color>} atlas-region descriptor, accounted for by
     * {@link #frameDescriptorResolves} through the live atlas seam; a card without a mappable
     * type/color yields a blank descriptor and is reported as a gap. Fail-open: any resolver/host
     * failure yields {@code false} rather than throwing.
     */
    static boolean resolvesToDrawable(String resourceId) {
        if (resourceId == null || resourceId.isEmpty()) return false;
        try {
            AssetResolveResult r = ArtFramework.assets().resolve(resourceId);
            return r != null && r.found
                    && artframework.sts1.assets.Sts1AssetMaterializer.isFileBacked(r.source);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int visibleCount(List<DrawItem> items) {
        int count = 0;
        for (DrawItem item : items) if (item.visible) count++;
        return count;
    }

    private static String cardResource(boolean selected, boolean enabled) {
        if (!enabled) return ResourceIds.UI_SELECT_CARD_DISABLED;
        return selected ? ResourceIds.UI_SELECT_CARD_SELECTED : ResourceIds.UI_SELECT_CARD;
    }

    /**
     * Fixed width of the select item box, in screen pixels.
     *
     * <p>This constant IS the box width: {@link #buildFromProjection()} passes it straight to
     * {@link DrawItem}. The fallback row below uses it to guarantee that neighbouring fallback cards
     * do not overlap, so widening the box automatically widens the pitch requirement. (The legacy
     * 8-arg convenience constructor of {@code DrawItem} still carries its own {@code 250f} literal;
     * it has no production caller.) The height is {@link #CARD_HEIGHT} and is not involved in
     * horizontal layout.
     */
    private static final float CARD_WIDTH = 250f;

    /**
     * Fixed height of the select item box, in screen pixels.
     *
     * <p>This constant IS the box height: {@link #buildFromProjection()} passes it straight to
     * {@link DrawItem}. {@link #defaultCardY} uses it to keep the fallback row clear of the native
     * 确认 pill, so a taller box automatically requires more clearance. It is not involved in the
     * horizontal layout (see {@link #CARD_WIDTH}).
     */
    private static final float CARD_HEIGHT = 350f;

    /**
     * Preferred horizontal gap between two fallback cards: 12 px, ~4.8 % of {@link #CARD_WIDTH}.
     * Enough visual separation for the card frame/shadow, while a 7-card row still fits a 1920 px
     * screen ({@code (7 - 1) * PREFERRED_PITCH + CARD_WIDTH = 1822 px}); a 10-card row does not
     * ({@code (10 - 1) * PREFERRED_PITCH + CARD_WIDTH = 2608 px}), which is what the overflow rule in
     * {@link #defaultCardX} shrinks.
     */
    private static final float CARD_GAP = 12f;

    /** Preferred centre-to-centre pitch: one full card box plus {@link #CARD_GAP}. */
    private static final float PREFERRED_PITCH = CARD_WIDTH + CARD_GAP;

    /** Minimum screen margin kept free on each side of the fallback row before it shrinks. */
    private static final float EDGE_MARGIN = 8f;

    /**
     * Fallback centre-x for a pose-less select card in the synthetic hand row.
     *
     * <p>Non-overlap rule: the preferred pitch is one full {@link #CARD_WIDTH} plus
     * {@link #CARD_GAP}, so consecutive boxes never overlap whenever the row fits on screen.
     * Centring: {@code start = mid - (total - 1) * pitch * 0.5f} with {@code mid = WIDTH * 0.5f}
     * keeps the row symmetric about the screen centre, so the middle card of an odd row sits exactly
     * on {@code WIDTH/2} and an even row is centred on the gap between its two middle cards.
     * Overflow rule: when {@code total} cards cannot fit inside the {@link #EDGE_MARGIN} margins, the
     * row shrinks uniformly using
     * {@code maxPitch = (WIDTH - 2*EDGE_MARGIN - CARD_WIDTH) / (total - 1)} (still centred) instead
     * of pushing cards off screen; overlap is re-introduced only when even that shrunk row cannot
     * hold the card width, which is unavoidable for very large pools. {@code WIDTH} is read
     * fail-open. A single card is placed at the centre; the pitch is irrelevant for it.
     */
    private static float defaultCardX(int index, int total) {
        float mid;
        try {
            mid = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        } catch (Throwable t) {
            mid = 960f;
        }
        if (total <= 1) return mid;
        float width = mid * 2f;
        float pitch = PREFERRED_PITCH;
        float maxPitch = (width - 2f * EDGE_MARGIN - CARD_WIDTH) / (total - 1);
        if (maxPitch > 0f) pitch = Math.min(PREFERRED_PITCH, maxPitch);
        float start = mid - (total - 1) * pitch * 0.5f;
        return start + index * pitch;
    }

    /**
     * Vertical clearance in pixels kept between the native 确认 ({@code CardSelectConfirmButton}) pill
     * and the fallback card band, measured in libGDX y-up screen coordinates: from the pill's UPPER
     * edge ({@code y + h/2} = {@code 475f + 128f = 603f} at {@code scale = 1}) to the card band's
     * LOWER edge ({@code y - h/2} = {@code 818f - 175f = 643f}), i.e. {@code 643f - 603f = 40f}.
     */
    private static final float CONFIRM_CLEARANCE = 40f;

    /**
     * Fallback centre-y for a pose-less select card in the synthetic hand row.
     *
     * <p><b>MOVE THE ROW, NOT THE PILL.</b> The native 确认 button geometry is FROZEN (see
     * {@link #confirmItem()}): texture {@code 512x256}, centre {@code (WIDTH/2, TAKE_Y)} with
     * {@code TAKE_Y = 475f * Settings.scale}. ART {@code DrawItem.x/y} are libGDX y-up
     * (bottom-origin) coordinates and the renderer derives a band as {@code (y - h/2) .. (y + h/2)},
     * so {@code y - h/2} is a band's LOWER edge and {@code y + h/2} its UPPER edge. At
     * {@code scale = 1} the pill therefore occupies the band {@code [347, 603]} (UPPER edge
     * {@code 475f + 256f * 0.5f = 603f}), and anchoring the card row on the screen height
     * ({@code HEIGHT * 0.45f}) put the {@code 250x350} card band {@code [311, 661]} straight through
     * it. The row therefore derives from the native TAKE_Y:
     * {@code y = TAKE_Y*scale + TEXTURE_H*0.5f*scale + CARD_HEIGHT*0.5f + CONFIRM_CLEARANCE}, i.e.
     * at {@code scale = 1}: {@code 475 + 128 + 175 + 40 = 818}. The resulting card band
     * {@code [643, 993]} lies entirely ABOVE the frozen pill band {@code [347, 603]} with a
     * {@code 643 - 603 = 40} px gap (={@link #CONFIRM_CLEARANCE}), and
     * {@code 818 + 175 = 993 <= 1080} keeps the band's UPPER edge inside the screen (87 px to
     * spare).
     *
     * <p>{@code Settings.scale} is read fail-open to {@code 1f} when the host type is unavailable or
     * reports a non-positive value, mirroring {@link #confirmSettings()} ({@code if (s > 0f)}). The
     * expression is pure arithmetic with fixed constants, so no host state can make it throw and no
     * {@code Settings.HEIGHT} / division is involved.
     */
    private static float defaultCardY() {
        float scale = 1f;
        try {
            float s = com.megacrit.cardcrawl.core.Settings.scale;
            if (s > 0f) {
                scale = s;
            }
        } catch (Throwable ignored) {
        }
        return CONFIRM_TAKE_Y * scale
                + CONFIRM_TEXTURE_H * 0.5f * scale
                + CARD_HEIGHT * 0.5f
                + CONFIRM_CLEARANCE;
    }
}
