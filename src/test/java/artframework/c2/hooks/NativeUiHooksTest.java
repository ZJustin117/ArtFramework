package artframework.c2.hooks;

import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.NativeTemplateIds;
import artframework.c2.SelectKind;
import artframework.core.SignalDecision;
import artframework.context.NativeInputComponent;
import artframework.context.NativeInterceptComponent;
import artframework.context.IntentNames;
import artframework.context.SurfaceIds;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
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
        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.MAP));
        assertEquals(IntentNames.CLICK_MAP_NODE, artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInputComponent.class).name);
        assertEquals("map_unbound", artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInterceptComponent.class).reason);
    }

    @Test
    public void hooksUseSharedBusStopResult() {
        bind(NativeTemplateIds.MAP);
        ArtFramework.connect(
                "ui/" + NativeTemplateIds.MAP + "/node_clicked",
                signal -> SignalDecision.stopRejected("blocked"));
        assertFalse(HostPatchResults.allowsNative(NativeUiHooks.onMapNodeClick(1, 2, "event")));
        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.MAP));
        assertEquals(IntentNames.CLICK_MAP_NODE, artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInputComponent.class).name);
        assertTrue(artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInterceptComponent.class).suppressNative);
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

    @Test
    public void selectHookRecordsUnboundInputPerSurface() {
        assertTrue(HostPatchResults.allowsNative(
                NativeUiHooks.onSelectCard(SelectKind.GRID, "x", 0)));
        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.SELECT_GRID));
        assertEquals(IntentNames.SELECT_CARD, artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInputComponent.class).name);
        assertEquals("select_grid_unbound", artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInterceptComponent.class).reason);
    }

    @Test
    public void disabledEndTurnRecordsNativeSuppressionDecision() {
        bind(NativeTemplateIds.END_TURN);
        artframework.c2.NativeTemplateRuntime.endTurn().setButtonEnabled(false);
        assertFalse(HostPatchResults.allowsNative(NativeUiHooks.onEndTurnPress()));
        EntityId entity = artframework.presentation.PresentationRegistry.context("sts1-input")
                .entity(new artframework.presentation.PresentationKey("sts1.input", SurfaceIds.END_TURN));
        NativeInterceptComponent intercept = artframework.presentation.PresentationRegistry.world()
                .get(entity, NativeInterceptComponent.class);
        assertTrue(intercept.suppressNative);
        assertEquals("endturn_disabled", intercept.reason);
    }
}
