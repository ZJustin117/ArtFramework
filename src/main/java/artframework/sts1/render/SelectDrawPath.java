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
                            250f,
                            350f,
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
                            c.pose != null ? c.pose.rotation : 0f));
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

    private static float defaultCardX(int index, int total) {
        float mid;
        try {
            mid = com.megacrit.cardcrawl.core.Settings.WIDTH * 0.5f;
        } catch (Throwable t) {
            mid = 960f;
        }
        float spacing = 140f;
        float start = mid - (Math.max(total, 1) - 1) * spacing * 0.5f;
        return start + index * spacing;
    }

    private static float defaultCardY() {
        try {
            return com.megacrit.cardcrawl.core.Settings.HEIGHT * 0.45f;
        } catch (Throwable t) {
            return 540f;
        }
    }
}
