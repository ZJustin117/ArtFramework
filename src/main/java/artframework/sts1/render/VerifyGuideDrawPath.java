package artframework.sts1.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import artframework.render.RenderHosts;
import artframework.render.RenderTarget;

/**
 * ART-owned verification guides/bounds overlay (render phase {@code VERIFY_GUIDES}, rank 1000).
 * Pure decision helpers plus a mode-gated {@code SpriteBatch} submit; the path never touches
 * native pixels or ECS state.
 */
public final class VerifyGuideDrawPath {

    private static final float DEFAULT_WIDTH = 1920f;
    private static final float DEFAULT_HEIGHT = 1080f;

    private VerifyGuideDrawPath() {}

    /** True when the configured verify mode paints an ART overlay this frame. */
    public static boolean shouldDraw() {
        return Sts1VerifyDiagnostics.overlayDrawEnabled();
    }

    public static boolean drawsGuides() {
        return Sts1VerifyDiagnostics.configuredMode() == Sts1VerifyDiagnostics.Mode.GUIDES;
    }

    public static boolean drawsBounds() {
        return Sts1VerifyDiagnostics.configuredMode() == Sts1VerifyDiagnostics.Mode.BOUNDS;
    }

    public static void render(SpriteBatch sb) {
        if (sb == null || !shouldDraw()) {
            return;
        }
        if (artframework.sts1.PresentSafety.isPanic()) {
            return;
        }
        com.badlogic.gdx.graphics.Texture texture =
                com.megacrit.cardcrawl.helpers.ImageMaster.WHITE_SQUARE_IMG;
        if (texture == null) {
            return;
        }
        Color prev = sb.getColor();
        try {
            if (drawsGuides()) {
                drawGuides(sb, texture);
            }
            if (drawsBounds()) {
                drawBounds(sb, texture);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                sb.setColor(prev);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void drawGuides(SpriteBatch sb, com.badlogic.gdx.graphics.Texture texture) {
        float sw = screenWidth();
        float sh = screenHeight();
        sb.setColor(1f, 0f, 0f, 0.6f);
        sb.draw(texture, sw * 0.5f - 0.5f, 0f, 1f, sh);
        sb.draw(texture, 0f, sh * 0.5f - 0.5f, sw, 1f);
    }

    private static void drawBounds(SpriteBatch sb, com.badlogic.gdx.graphics.Texture texture) {
        sb.setColor(0f, 0.9f, 1f, 0.8f);
        for (String id : RenderHosts.get().listTargetIds()) {
            RenderTarget target = RenderHosts.get().getTarget(id);
            if (target == null || !target.isEnabled()) {
                continue;
            }
            drawOutline(sb, texture, target.x(), target.y(),
                    target.width(), target.height());
        }
    }

    private static void drawOutline(
            SpriteBatch sb,
            com.badlogic.gdx.graphics.Texture texture,
            float x,
            float y,
            float width,
            float height) {
        if (width <= 0f || height <= 0f) {
            return;
        }
        float thickness = 2f;
        sb.draw(texture, x, y, width, thickness);
        sb.draw(texture, x, y + height - thickness, width, thickness);
        sb.draw(texture, x, y, thickness, height);
        sb.draw(texture, x + width - thickness, y, thickness, height);
    }

    private static float screenWidth() {
        try {
            float w = com.megacrit.cardcrawl.core.Settings.WIDTH;
            return w > 0f ? w : DEFAULT_WIDTH;
        } catch (Throwable ignored) {
            return DEFAULT_WIDTH;
        }
    }

    private static float screenHeight() {
        try {
            float h = com.megacrit.cardcrawl.core.Settings.HEIGHT;
            return h > 0f ? h : DEFAULT_HEIGHT;
        } catch (Throwable ignored) {
            return DEFAULT_HEIGHT;
        }
    }
}
