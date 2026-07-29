package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import artframework.sts1.render.EventDrawPath;

/** Skip native generic event dialog draw while ART event full-present is active. */
public final class EventRenderPatches {

    private EventRenderPatches() {}

    @SpirePatch(clz = GenericEventDialog.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeEventRender {
        public static SpireReturn<Void> Prefix(GenericEventDialog __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return EventDrawPath.shouldSuppressNativeEvent()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
