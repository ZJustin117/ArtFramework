package artframework.sts1.patch;

import artframework.sts1.skeleton.Sts1SkeletonBridge;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/** Fine-grained native Spine draw interception. Health bars and creature chrome remain native. */
public final class SkeletonRenderPatches {
    private SkeletonRenderPatches() {}

    @SpirePatch(
            clz = AbstractCreature.class,
            method = "loadAnimation",
            paramtypez = {String.class, String.class, float.class})
    public static class CaptureNativeSkeletonSource {
        public static void Postfix(AbstractCreature __instance, String atlasPath, String skeletonPath,
                float scale) {
            Sts1SkeletonBridge.observeNativeCreature(__instance, atlasPath, skeletonPath);
        }
    }

    @SpirePatch(
            clz = com.esotericsoftware.spine.SkeletonMeshRenderer.class,
            method = "draw",
            paramtypez = {
                com.badlogic.gdx.graphics.g2d.Batch.class,
                com.esotericsoftware.spine.Skeleton.class})
    public static class SuppressClaimedNativeSkeleton {
        public static SpireReturn<Void> Prefix(
                com.esotericsoftware.spine.SkeletonMeshRenderer __instance,
                com.badlogic.gdx.graphics.g2d.Batch batch,
                com.esotericsoftware.spine.Skeleton skeleton) {
            return Sts1SkeletonBridge.renderClaimedNative(skeleton, batch)
                    ? SpireReturn.Return(null)
                    : SpireReturn.Continue();
        }
    }
}
