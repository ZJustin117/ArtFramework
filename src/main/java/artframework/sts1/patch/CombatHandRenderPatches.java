package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import artframework.sts1.render.Sts1SurfaceRenderer;

/** Skip only the native hand draw while the explicit ART combat hand mode is active. */
public final class CombatHandRenderPatches {

    private CombatHandRenderPatches() {}

    @SpirePatch(clz = AbstractPlayer.class, method = "renderHand")
    public static class SuppressNativeHandRender {
        public static SpireReturn<Void> Prefix(AbstractPlayer __instance) {
            return Sts1SurfaceRenderer.shouldSuppressNativeHand()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
