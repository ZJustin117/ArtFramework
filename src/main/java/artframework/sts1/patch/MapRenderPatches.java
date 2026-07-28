package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import artframework.sts1.render.MapDrawPath;

/** Skip native map screen draw when ART map full-present owns display (16.7). */
public final class MapRenderPatches {

    private MapRenderPatches() {}

    @SpirePatch(
            clz = DungeonMapScreen.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class SuppressNativeMapRender {
        public static SpireReturn<Void> Prefix(
                DungeonMapScreen __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            return MapDrawPath.shouldSuppressNativeMap()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
