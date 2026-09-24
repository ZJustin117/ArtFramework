package artframework.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Focused contract tests for the host-neutral pixel payload and its frame plumbing. */
public class RenderPixelPayloadTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }

    private static RenderPixelPayload payload(String resourceId) {
        return new RenderPixelPayload(resourceId, "label",
                1f, 2f, 3f, 4f, 0.25f, 0.5f, 0.5f, 0.5f,
                true, false, 90f, 2f, 3f, 1f, 0f, 0f, 0.5f, "ADD", 3, 4, 2);
    }

    @Test public void payloadIsImmutableAndNormalizesNullStrings() {
        RenderPixelPayload value = new RenderPixelPayload(
                null, null,
                0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f,
                false, false, 0f, 1f, 1f, 1f, 1f, 1f, 1f, null,
                0, 1, 1);
        assertEquals("", value.resourceId());
        assertEquals("", value.label());
        assertEquals("", value.blendMode());
        for (Field field : RenderPixelPayload.class.getDeclaredFields()) {
            assertTrue("payload field must be final: " + field.getName(),
                    Modifier.isFinal(field.getModifiers()));
        }
        assertNotSame(value.sourceRect(), value.sourceRect());
    }

    @Test public void payloadCarriesOnlyHostNeutralData() {
        for (Field field : RenderPixelPayload.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            boolean allowed = type.isPrimitive()
                    || type == String.class
                    || type == Rect.class;
            assertTrue("payload field must be host-neutral: " + field.getName()
                    + " (" + type.getName() + ")", allowed);
        }
    }

    @Test public void payloadRejectsNonFiniteAndNegativeDimensions() {
        assertInvalidPayload(Float.NaN, 1f, 1f, 1f);
        assertInvalidPayload(Float.POSITIVE_INFINITY, 1f, 1f, 1f);
        assertInvalidPayload(1f, -1f, 1f, 1f);
        assertInvalidPayload(1f, 1f, -0.5f, 1f);
    }

    private static void assertInvalidPayload(float x, float width, float sourceWidth, float alpha) {
        try {
            new RenderPixelPayload("tex", null,
                    x, 0f, width, 1f, 0f, 0f, sourceWidth, 1f,
                    false, false, 0f, 1f, 1f, 1f, 1f, 1f, alpha, "MIX",
                    0, 1, 1);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected invalid pixel payload");
    }

    @Test public void payloadDoesNotParticipateInOrderingOrDedupIdentity() throws Exception {
        // Two entries with identical ordering metadata but different payloads keep distinct
        // identity (different stable keys) and sort purely by (phase, z, stableKey).
        RenderPlan.Entry withPayload = RenderPlan.Entry.payloadEntry("b", RenderTargetKind.OVERLAY,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.ART_EFFECTS, 5f, "b", true, payload("tex-b"));
        RenderPlan.Entry without = new RenderPlan.Entry("a", RenderTargetKind.SYNTHETIC_WIDGET,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.NATIVE_RETAINED, 5f, "a", true,
                Collections.<artframework.presentation.EffectAttachment>emptyList());

        RenderPlan plan = RenderPlan.unifiedFrame(
                Collections.singletonList(without), Collections.singletonList(withPayload));

        assertEquals(Arrays.asList("a", "b"), ids(plan));
        assertNull("identity-only entry keeps no payload", plan.entries().get(0).payload);
        assertNotNull("payload-bearing entry keeps its payload", plan.entries().get(1).payload);
    }

    @Test public void unifiedFrameSortsMixedEntriesByPhaseThenZThenStableKey() {
        RenderPlan.Entry nativeLow = RenderPlan.Entry.payloadEntry("native:x",
                RenderTargetKind.SYNTHETIC_WIDGET, new Rect(0f, 0f, 1f, 1f),
                RenderPhase.NATIVE_RETAINED, 0f, "n", true, null);
        RenderPlan.Entry vfxHigh = RenderPlan.Entry.payloadEntry("vfx:z",
                RenderTargetKind.OVERLAY, new Rect(0f, 0f, 1f, 1f),
                RenderPhase.ART_EFFECTS, 1f, "z", true, payload("tex"));
        RenderPlan.Entry vfxLow = RenderPlan.Entry.payloadEntry("vfx:a",
                RenderTargetKind.OVERLAY, new Rect(0f, 0f, 1f, 1f),
                RenderPhase.ART_EFFECTS, 1f, "a", true, payload("tex"));

        RenderPlan plan = RenderPlan.unifiedFrame(Arrays.asList(vfxHigh, nativeLow),
                Collections.singletonList(vfxLow));

        assertEquals(Arrays.asList("native:x", "vfx:a", "vfx:z"), ids(plan));
    }

    @Test public void unifiedFrameRejectsDuplicateStableKeysAcrossPayloadAndIdentity() {
        RenderPlan.Entry identity = new RenderPlan.Entry("native:same", RenderTargetKind.SYNTHETIC_WIDGET,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.NATIVE_RETAINED, 0f, "same", true,
                Collections.<artframework.presentation.EffectAttachment>emptyList());
        RenderPlan.Entry produced = RenderPlan.Entry.payloadEntry("vfx:same", RenderTargetKind.OVERLAY,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.ART_EFFECTS, 0f, "same", true, payload("tex"));
        try {
            RenderPlan.unifiedFrame(Collections.singletonList(identity),
                    Collections.singletonList(produced));
            fail("expected duplicate stable key rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("duplicate render stable key"));
        }
    }

    @Test public void snapshotPassesPayloadThroughToPlanWithoutChangingOwnership() {
        PresentationContext context = PresentationRegistry.context("nrcc-native");
        EntityId entity = context.create(new PresentationKey("test.payload", "observed"),
                "observed", "native_render", "test");
        context.world().put(entity, NativeRenderInputComponent.class,
                new NativeRenderInputComponent("surface:test", "observed", RenderPhase.ART_EFFECTS,
                        0f, "observed", new Rect(0f, 0f, 5f, 5f), true,
                        NativeRenderOwnership.OBSERVED, "combat", 7L, payload("observed-tex")));
        // A delegated input with a payload must still be skipped: a payload is never a delegation.
        EntityId delegated = context.create(new PresentationKey("test.payload", "delegated"),
                "delegated", "native_render", "test");
        context.world().put(delegated, NativeRenderInputComponent.class,
                new NativeRenderInputComponent("surface:test", "delegated", RenderPhase.ART_BACKGROUND,
                        0f, "delegated", new Rect(0f, 0f, 5f, 5f), true,
                        NativeRenderOwnership.DELEGATED_TO_ART, "combat", 7L, payload("delegated-tex")));

        RenderPlan plan = RenderPlan.fromEcs(Collections.<String>emptySet());

        RenderPlan.Entry observed = null;
        for (RenderPlan.Entry entry : plan.entries()) {
            assertFalse("delegated pixels must not be native-retained even with a payload",
                    "native:delegated".equals(entry.id));
            if ("native:observed".equals(entry.id)) observed = entry;
        }
        assertNotNull(observed);
        assertNotNull("snapshot payload must reach the plan entry", observed.payload);
        assertEquals("observed-tex", observed.payload.resourceId());
        assertEquals("payload must never alter the retained boundary",
                RenderPhase.NATIVE_RETAINED, observed.phase);
    }

    @Test public void payloadPresenceDoesNotPromoteOverlayOwnership() {
        PresentationContext context = PresentationRegistry.context("nrcc-native");
        EntityId overlay = context.create(new PresentationKey("test.payload", "overlay"),
                "overlay", "native_render", "test");
        context.world().put(overlay, NativeRenderInputComponent.class,
                new NativeRenderInputComponent("surface:test", "overlay", RenderPhase.NATIVE_RETAINED,
                        0f, "overlay", new Rect(0f, 0f, 5f, 5f), true,
                        NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY, "combat", 1L,
                        payload("overlay-tex")));

        RenderPlan plan = RenderPlan.fromEcs(Collections.<String>emptySet());

        RenderPlan.Entry entry = null;
        for (RenderPlan.Entry candidate : plan.entries()) {
            if ("native:overlay".equals(candidate.id)) entry = candidate;
        }
        assertNotNull(entry);
        assertEquals(RenderPhase.NATIVE_RETAINED, entry.phase);
        assertNotNull(entry.payload);
        assertEquals("overlay must not be read as a delegated pixel claim",
                NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY,
                context.world().get(overlay, NativeRenderInputComponent.class).ownership());
    }

    private static List<String> ids(RenderPlan plan) {
        List<String> out = new ArrayList<String>();
        for (RenderPlan.Entry entry : plan.entries()) out.add(entry.id);
        return out;
    }
}
