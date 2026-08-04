package artframework.sts1.render;

import artframework.component.Rect;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;

import java.lang.reflect.Field;

/** Reads the native button's localized text and hitbox while ART owns only its pixels. */
final class Sts1EndTurnChrome {
    private Sts1EndTurnChrome() {}

    static Rect bounds(float fallbackWidth, float fallbackHeight) {
        EndTurnButton button = button();
        Object hb = field(button, "hb");
        Float x = number(hb, "x");
        Float y = number(hb, "y");
        Float w = number(hb, "width");
        Float h = number(hb, "height");
        if (x != null && y != null && w != null && h != null && w.floatValue() > 0f && h.floatValue() > 0f) {
            return new Rect(x.floatValue(), y.floatValue(), w.floatValue(), h.floatValue());
        }
        return ControlsDrawPath.endTurnBounds(fallbackWidth, fallbackHeight);
    }

    static String label(String fallback) {
        Object label = field(button(), "label");
        if (label != null && !String.valueOf(label).trim().isEmpty()) {
            return String.valueOf(label);
        }
        return fallback != null ? fallback : "";
    }

    private static EndTurnButton button() {
        try {
            return AbstractDungeon.overlayMenu != null ? AbstractDungeon.overlayMenu.endTurnButton : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Float number(Object target, String name) {
        Object value = field(target, name);
        return value instanceof Number ? Float.valueOf(((Number) value).floatValue()) : null;
    }

    private static Object field(Object target, String name) {
        if (target == null || name == null) {
            return null;
        }
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
