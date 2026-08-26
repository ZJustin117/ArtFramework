package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import artframework.sts1.render.NativeRenderBridge;

/** Observe or suppress native map screen draw depending on C2 full-present ownership. */
public final class MapRenderPatches {

    private MapRenderPatches() {}

    @SpirePatch(
            clz = DungeonMapScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeMapRender {
        public static SpireReturn<Void> Prefix(
                DungeonMapScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            artframework.sts1.render.RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.map", "com.megacrit.cardcrawl.screens.DungeonMapScreen", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return !disposition.nativeContinuation
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
