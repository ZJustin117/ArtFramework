package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;
import artframework.assets.ResourceIds;
import artframework.context.RoomShellView;
import artframework.sts1.assets.Sts1VanillaCatalog;
import artframework.sts1.backend.Sts1RoomShellProjection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resource-backed chrome/label overlay for native room-shell pixels. */
public final class Sts1RoomShellDrawPath {
    private Sts1RoomShellDrawPath() {}

    public static RoomShellView current() { return Sts1RoomShellProjection.current(); }

    public static String resourceFor(RoomShellView view) {
        if (view == null) return ResourceIds.UI_ROOM_SHELL_UNKNOWN;
        if (Sts1VanillaCatalog.isKnown(view.resourceId)) return view.resourceId;
        String candidate = ResourceIds.roomShell(view.kind);
        return Sts1VanillaCatalog.isKnown(candidate) ? candidate : ResourceIds.UI_ROOM_SHELL_UNKNOWN;
    }

    public static Map<String, Object> probeSlice() {
        RoomShellView view = current();
        Map<String, Object> out = new LinkedHashMap<String, Object>(view.toMap());
        out.put("resourceId", resourceFor(view));
        AssetResolveResult resolved = null;
        try { resolved = ArtFramework.assets().resolve(resourceFor(view)); } catch (Throwable ignored) {}
        out.put("resourceFound", Boolean.valueOf(resolved != null && resolved.found));
        out.put("nativeContinuation", Boolean.TRUE);
        out.put("nativePixelsSuppressed", Boolean.FALSE);
        out.put("surface", "overlay-only");
        return out;
    }

    public static void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
        RoomShellView view = current();
        // Event dialog already owns the event surface when projected; keep this room-shell chrome
        // metadata-only there so the existing delegated event surface is not visually duplicated.
        if (sb == null || !view.available || !view.visible
                || ("event".equals(view.kind) && ArtFramework.projection().event().available)) return;
        try {
            artframework.assets.AssetResolveResult result = ArtFramework.assets().resolve(resourceFor(view));
            com.badlogic.gdx.graphics.Texture texture =
                    artframework.sts1.assets.Sts1AssetMaterializer.resolveTexture(result);
            if (texture != null && view.bounds.width > 0f && view.bounds.height > 0f) {
                sb.draw(texture, view.bounds.x, view.bounds.y, view.bounds.width, view.bounds.height);
            }
            if (!view.title.isEmpty()) {
                com.megacrit.cardcrawl.helpers.FontHelper.renderFontCentered(
                        sb, com.megacrit.cardcrawl.helpers.FontHelper.buttonLabelFont, view.title,
                        view.bounds.x + view.bounds.width * 0.5f,
                        view.bounds.y + view.bounds.height * 0.54f,
                        com.badlogic.gdx.graphics.Color.WHITE);
            }
        } catch (Throwable ignored) {
            // Native room rendering has already continued; overlay failure is fail-open.
        }
    }
}
