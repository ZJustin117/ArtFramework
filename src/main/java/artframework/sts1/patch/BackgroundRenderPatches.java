package artframework.sts1.patch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.scenes.TheBeyondScene;
import com.megacrit.cardcrawl.scenes.TheBottomScene;
import com.megacrit.cardcrawl.scenes.TheCityScene;
import com.megacrit.cardcrawl.scenes.TheEndingScene;

/**
 * Pre-native combat-room background suppression for {@code art verify mode background}.
 *
 * <p>{@code AbstractScene.renderCombatRoomBg(SpriteBatch)} is abstract, so ModTheSpire cannot
 * attach a Prefix to it. The four concrete scene overrides own the native background pixels and are
 * the patchable suppression targets; {@code AbstractDungeon.render} dispatches them via
 * {@code invokevirtual}, so a Prefix on each override intercepts every in-run scene background.
 *
 * <p>Each Prefix decouples draw from suppression. It first calls {@link
 * artframework.sts1.render.BackgroundRenderGate#renderSelectedVariant(SpriteBatch)}: the ART
 * background is drawn whenever a non-{@code off} variant is selected and no panic is active (so a
 * retained native background hides the ART pixels — the z-order evidence). It then consults {@link
 * artframework.sts1.render.BackgroundRenderGate#suppressNativeBackground()}: native pixels are
 * skipped only when, additionally, the {@code sts1.room.background} family is explicitly filtered
 * via {@code art verify native} and no panic is active. Every other state — including a renderer
 * failure — fails open with {@code SpireReturn.Continue()}, leaving the native scene background
 * unchanged.
 */
public final class BackgroundRenderPatches {
    private BackgroundRenderPatches() {}

    @SpirePatch(clz = TheBottomScene.class, method = "renderCombatRoomBg",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeBottomBackground {
        public static SpireReturn<Void> Prefix(TheBottomScene __instance, SpriteBatch sb) {
            boolean painted =
                    artframework.sts1.render.BackgroundRenderGate.renderSelectedVariant(sb);
            return painted && artframework.sts1.render.BackgroundRenderGate.suppressNativeBackground()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = TheCityScene.class, method = "renderCombatRoomBg",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeCityBackground {
        public static SpireReturn<Void> Prefix(TheCityScene __instance, SpriteBatch sb) {
            boolean painted =
                    artframework.sts1.render.BackgroundRenderGate.renderSelectedVariant(sb);
            return painted && artframework.sts1.render.BackgroundRenderGate.suppressNativeBackground()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = TheBeyondScene.class, method = "renderCombatRoomBg",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeBeyondBackground {
        public static SpireReturn<Void> Prefix(TheBeyondScene __instance, SpriteBatch sb) {
            boolean painted =
                    artframework.sts1.render.BackgroundRenderGate.renderSelectedVariant(sb);
            return painted && artframework.sts1.render.BackgroundRenderGate.suppressNativeBackground()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = TheEndingScene.class, method = "renderCombatRoomBg",
            paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class})
    public static class ObserveNativeEndingBackground {
        public static SpireReturn<Void> Prefix(TheEndingScene __instance, SpriteBatch sb) {
            boolean painted =
                    artframework.sts1.render.BackgroundRenderGate.renderSelectedVariant(sb);
            return painted && artframework.sts1.render.BackgroundRenderGate.suppressNativeBackground()
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
