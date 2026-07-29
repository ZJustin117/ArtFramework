package artframework.c2.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import artframework.c2.hooks.HostPatchResults;
import artframework.c2.hooks.NativeUiHooks;
import artframework.core.SignalDispatchResult;

/**
 * Thin map click gate: when {@code sts.map} is bound and bus rejects, cancel travel.
 */
@SuppressWarnings("unused")
public final class MapNodeClickPatches {

    private MapNodeClickPatches() {}

    @SpirePatch(clz = AbstractDungeon.class, method = "nextRoomTransitionStart", paramtypez = {})
    public static class InterceptTransitionStart {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            MapRoomNode next = AbstractDungeon.nextRoom;
            if (next == null) {
                return SpireReturn.Continue();
            }
            String roomType = next.room != null ? next.room.getClass().getSimpleName() : "";
            SignalDispatchResult result = NativeUiHooks.onMapNodeClick(next.y, next.x, roomType);
            if (!HostPatchResults.allowsNative(result)) {
                AbstractDungeon.nextRoom = null;
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = AbstractDungeon.class, method = "nextRoomTransition", paramtypez = {})
    public static class InterceptNextRoomTransition {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            MapRoomNode next = AbstractDungeon.nextRoom;
            if (next == null) {
                return SpireReturn.Continue();
            }
            String roomType = next.room != null ? next.room.getClass().getSimpleName() : "";
            SignalDispatchResult result = NativeUiHooks.onMapNodeClick(next.y, next.x, roomType);
            if (!HostPatchResults.allowsNative(result)) {
                AbstractDungeon.nextRoom = null;
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }
}
