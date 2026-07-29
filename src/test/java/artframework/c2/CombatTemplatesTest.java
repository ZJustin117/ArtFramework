package artframework.c2;

import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.core.SignalDecision;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatTemplatesTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }
    private void bind(String id) { ArtFramework.register(new WindowDef(id, WindowClass.NATIVE_TEMPLATE, id)); ArtFramework.bind(id); }
    @Test public void templatesKeepLifecycleOnly() {
        bind(NativeTemplateIds.EVENT);
        NativeTemplateRuntime.event().setEventId("event");
        assertTrue(NativeTemplateRuntime.isEventBound());
        ArtFramework.close(NativeTemplateIds.EVENT);
        assertFalse(NativeTemplateRuntime.isEventBound());
        assertTrue(NativeTemplateRuntime.event().getEventId().isEmpty());
    }
    @Test public void eventAndSelectPolicyUseSignalBus() {
        bind(NativeTemplateIds.EVENT);
        bind(NativeTemplateIds.SELECT_GRID);
        ArtFramework.connect("ui/" + NativeTemplateIds.EVENT + "/option_chosen", signal -> SignalDecision.stopRejected("blocked"));
        ArtFramework.connect("ui/" + NativeTemplateIds.SELECT_GRID + "/card_selected", signal -> SignalDecision.stopRejected("blocked"));
        assertFalse(
                artframework.c2.hooks.HostPatchResults.allowsNative(
                        artframework.c2.hooks.NativeUiHooks.onEventOption(0, "Leave")));
        assertFalse(
                artframework.c2.hooks.HostPatchResults.allowsNative(
                        artframework.c2.hooks.NativeUiHooks.onSelectCard(SelectKind.GRID, "Strike", 0)));
    }
    @Test public void endTurnEnabledHintRemainsPresentationState() {
        bind(NativeTemplateIds.END_TURN);
        NativeTemplateRuntime.endTurn().setButtonEnabled(false);
        assertFalse(
                artframework.c2.hooks.HostPatchResults.allowsNative(
                        artframework.c2.hooks.NativeUiHooks.onEndTurnPress()));
        NativeTemplateRuntime.endTurn().setButtonEnabled(true);
        assertTrue(
                artframework.c2.hooks.HostPatchResults.allowsNative(
                        artframework.c2.hooks.NativeUiHooks.onEndTurnPress()));
    }
}
