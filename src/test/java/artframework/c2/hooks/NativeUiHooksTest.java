package artframework.c2.hooks;

import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c2.NativeTemplateIds;
import artframework.c2.SelectKind;
import artframework.core.SignalDecision;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeUiHooksTest {
    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    private void bind(String id) {
        ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id));
        ArtFramework.bind(id);
    }

    @Test
    public void inactiveMapContinues() {
        assertTrue(HostPatchResults.allowsNative(NativeUiHooks.onMapNodeClick(0, 0, "monster")));
    }

    @Test
    public void hooksUseSharedBusStopResult() {
        bind(NativeTemplateIds.MAP);
        ArtFramework.connect(
                "ui/" + NativeTemplateIds.MAP + "/node_clicked",
                signal -> SignalDecision.stopRejected("blocked"));
        assertFalse(HostPatchResults.allowsNative(NativeUiHooks.onMapNodeClick(1, 2, "event")));
    }

    @Test
    public void eventSelectAndEndTurnUseBusResults() {
        bind(NativeTemplateIds.EVENT);
        bind(NativeTemplateIds.SELECT_GRID);
        bind(NativeTemplateIds.END_TURN);
        ArtFramework.connect(
                "ui/" + NativeTemplateIds.EVENT + "/option_chosen",
                signal -> SignalDecision.stopRejected("blocked"));
        ArtFramework.connect(
                "ui/" + NativeTemplateIds.SELECT_GRID + "/card_selected",
                signal -> SignalDecision.stopRejected("blocked"));
        assertFalse(HostPatchResults.allowsNative(NativeUiHooks.onEventOption(0, "a")));
        assertFalse(HostPatchResults.allowsNative(NativeUiHooks.onSelectCard(SelectKind.GRID, "x", 0)));
        assertTrue(HostPatchResults.allowsNative(NativeUiHooks.onEndTurnPress()));
    }
}
