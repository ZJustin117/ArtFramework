package artframework.sts1.render;

import artframework.core.PresentChromeStyle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/** Small, shared chrome painter for ART-owned C2 surface regions. */
final class C2ChromePainter {
    private C2ChromePainter() {}

    static void panel(SpriteBatch sb, float x, float y, float w, float h, PresentChromeStyle chrome) {
        if (sb == null || chrome == null || w <= 0f || h <= 0f) {
            return;
        }
        Texture white = ImageMaster.WHITE_SQUARE_IMG;
        if (white == null) {
            return;
        }
        Color previous = sb.getColor();
        try {
            sb.setColor(chrome.panelR, chrome.panelG, chrome.panelB, chrome.panelAlpha);
            sb.draw(white, x, y, w, h);
            float bw = chrome.borderWidth;
            if (bw > 0f) {
                sb.setColor(chrome.borderR, chrome.borderG, chrome.borderB, chrome.borderA);
                sb.draw(white, x, y + h - bw, w, bw);
                sb.draw(white, x, y, w, bw);
                sb.draw(white, x, y, bw, h);
                sb.draw(white, x + w - bw, y, bw, h);
            }
        } finally {
            sb.setColor(previous);
        }
    }
}
