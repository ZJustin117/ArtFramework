package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.ui.buttons.EndTurnButton;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native end-turn rendering. When the controls surface is FULL + mounted + combat scene,
 * ART suppresses the original EndTurnButton.render and paints the button through the synced C2
 * item. Otherwise the native renderer continues unchanged.
 */
public final class CombatControlsRenderPatches {

    private CombatControlsRenderPatches() {}

    @SpirePatch(clz = EndTurnButton.class, method = "render", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeEndTurnRender {
        public static SpireReturn<Void> Prefix(EndTurnButton __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.combat.controls", "com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
