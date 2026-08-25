package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import artframework.sts1.render.NativeRenderBridge;

/** Observe native generic event dialog draw; original renderer stays the visual authority (NRO-03). */
public final class EventRenderPatches {

    private EventRenderPatches() {}

    @SpirePatch(clz = GenericEventDialog.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeEventRender {
        public static SpireReturn<Void> Prefix(GenericEventDialog __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            NativeRenderBridge.beginSurface(
                    "sts1.event", "com.megacrit.cardcrawl.events.GenericEventDialog", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return SpireReturn.Continue();
        }
    }
}
