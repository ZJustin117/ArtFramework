package artframework.sts1.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.RenderDisposition;

/**
 * Gates native treasure CHEST rendering (D08 regression fix). The treasure room is no longer
 * suppressed at {@code TreasureRoom.render}: suppressing the room also removed
 * {@code AbstractRoom.render}, which draws the player sprite via {@code AbstractDungeon.player.render}.
 * Instead ART now owns only the {@code AbstractChest.render} invocation (all chest classes via the
 * {@code AbstractChest} base method), so the room continues and the player is preserved.
 *
 * <p>When the treasure surface is FULL + mounted + matching scene, ART suppresses the original chest
 * draw and paints the chest sprite through the synced C2 item. Otherwise the native renderer
 * continues unchanged. The surface family/ownerId stays {@code "sts1.treasure"} so the plan, probe,
 * and {@code shouldSuppressNative(SurfaceIds.TREASURE)} are unchanged.
 */
public final class TreasureChestRenderPatches {
    private TreasureChestRenderPatches() {}

    @SpirePatch(
            clz = AbstractChest.class,
            method = "render",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeTreasureChestRender {
        public static SpireReturn<Void> Prefix(
                AbstractChest __instance, com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    "sts1.treasure",
                    "com.megacrit.cardcrawl.rewards.chests.AbstractChest", "render",
                    __instance != null ? String.valueOf(System.identityHashCode(__instance)) : "");
            return disposition.nativeContinuation
                    ? SpireReturn.Continue()
                    : SpireReturn.Return(null);
        }
    }
}
