package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native generic event dialog draw. When the event surface is FULL + mounted + event scene,
 * ART suppresses the original GenericEventDialog.render and paints the event through synced C2
 * items. Otherwise the native renderer continues unchanged.
 */
public final class EventRenderPatches {

    private EventRenderPatches() {}

    @SpirePatch(clz = GenericEventDialog.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeEventRender {
        public static SpireReturn<Void> Prefix(GenericEventDialog __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.event", "com.megacrit.cardcrawl.events.GenericEventDialog", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
