package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.screens.select.HandCardSelectScreen;
import artframework.sts1.render.SelectDrawPath;

/** Skip native grid/hand select draw while ART select full-present is active. */
public final class SelectRenderPatches {

    private SelectRenderPatches() {}

    @SpirePatch(
            clz = GridCardSelectScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeGridSelectRender {
        public static SpireReturn<Void> Prefix(
                GridCardSelectScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return SelectDrawPath.shouldSuppressNativeGrid()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = HandCardSelectScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeHandSelectRender {
        public static SpireReturn<Void> Prefix(
                HandCardSelectScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return SelectDrawPath.shouldSuppressNativeHandSelect()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
