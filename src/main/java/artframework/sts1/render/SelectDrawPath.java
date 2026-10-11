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
                            350f));
            i++;
        }
        if (sv.confirmVisible || sv.confirmEnabled) {
            out.add(confirmItem());
        }
        return out;
    }

    /**
     * Derives the select card BASE-BACKGROUND frame descriptor from the card's native type/color:
     * {@code cardui#512/bg_<type>_<color>} for a known {@code attack|skill|power} type and a mappable
     * color, else {@code ""}. Type/color are matched case-insensitively; {@code colorless} maps to the
     * {@code gray} base-background page. Fail-open: a null/blank/unmapped value or any failure yields
     * {@code ""} (no frame draw instead of a bogus one).
     */
    static String cardFrameDescriptor(String type, String color) {
        try {
            String t = type != null ? type.trim().toLowerCase(java.util.Locale.ROOT) : "";
            if (!"attack".equals(t) && !"skill".equals(t) && !"power".equals(t)) {
                return "";
            }
            String mapped = mappedColor(color);
            if (mapped.isEmpty()) {
                return "";
            }
            return "cardui#512/bg_" + t + "_" + mapped;
        } catch (Throwable ignored) {
            return "";
        }
    }

    /** Maps a native card color name to the cardui base-background color token, or {@code ""}. */
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
     * Required resources = the primary id, plus the type/color-derived frame descriptor for
     * non-confirm card items. The {@code !item.confirm && !item.cardId.isEmpty()} guard mirrors
     * {@code Sts1SurfaceRenderer.renderSelect}'s frame draw. For a card item the frame requirement
     * is satisfied iff the descriptor resolves: {@code "<atlasKey>#<region>"} descriptors resolve via
     * {@link artframework.sts1.assets.Sts1AtlasMaterializer#regionFromAtlas}, any other id via
     * {@link #resolvesToDrawable}. A blank frame descriptor is NOT materialized (the card is missing
     * its base background), so it counts as a gap.
     */
    private static boolean itemFullyResolves(DrawItem item) {
        if (!resolvesToDrawable(item.resourceId)) return false;
        if (!item.confirm && !item.cardId.isEmpty()) {
            return frameDescriptorResolves(item.frameResourceId);
        }
        return true;
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
