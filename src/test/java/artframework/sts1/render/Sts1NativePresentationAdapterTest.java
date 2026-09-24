package artframework.sts1.render;

import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;
import artframework.render.NativeRenderFrameExtractor;
import artframework.render.NativeRenderInputComponent;
import artframework.render.NativeRenderInputSnapshot;
import artframework.render.NativeRenderOwnership;
import artframework.render.RenderPhase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Sts1NativePresentationAdapterTest {
    @After
    public void tearDown() {
        Sts1NativePresentationAdapter.clear();
        PresentationRegistry.resetForTests();
    }

    private NativeRenderInvocation invocation(String owner, long frame, Rect bounds) {
        return new NativeRenderInvocation(1L, frame, "combat", owner, "Native", "render",
                "surface", "source", bounds);
    }

    private NativeRenderInvocation effectInvocation(String owner, String nativeClass, long frame) {
        return new NativeRenderInvocation(1L, frame, "combat", owner, nativeClass, "render",
                "vfx-misc-root", owner, Rect.ZERO);
    }

    @Test
    public void presentCreatesStableEntityWithHostNeutralComponents() {
        String first = Sts1NativePresentationAdapter.present(
                invocation("sts1.combat.hand", 3L, new Rect(1f, 2f, 30f, 40f)));
        String second = Sts1NativePresentationAdapter.present(
                invocation("sts1.combat.hand", 4L, new Rect(5f, 6f, 50f, 60f)));

        assertEquals(first, second);
        EntityId entity = Sts1NativePresentationAdapter.entity("sts1.combat.hand");
        assertNotNull(entity);
        BoundsComponent bounds = PresentationRegistry.world().get(entity, BoundsComponent.class);
        DrawComponent draw = PresentationRegistry.world().get(entity, DrawComponent.class);
        VisibilityComponent visibility = PresentationRegistry.world().get(entity, VisibilityComponent.class);
        assertEquals(new Rect(5f, 6f, 50f, 60f), bounds.rect);
        assertEquals("render", draw.role);
        assertTrue(visibility.visible);
        NativeInvocationComponent nativeInvocation =
                PresentationRegistry.world().get(entity, NativeInvocationComponent.class);
        assertNotNull(nativeInvocation);
        assertEquals(1L, nativeInvocation.invocationId);
        assertEquals(4L, nativeInvocation.frameId);
        assertEquals("surface", nativeInvocation.surfaceFamily);
    }

    @Test
    public void stableEntityCarriesLatestInvocationWithoutChangingIdentity() {
        String first = Sts1NativePresentationAdapter.present(invocation("owner", 1L, Rect.ZERO));
        NativeRenderInvocation next = new NativeRenderInvocation(9L, 2L, "combat", "owner",
                "Native2", "render2", "family2", "source2", Rect.ZERO);
        String second = Sts1NativePresentationAdapter.present(next);

        assertEquals(first, second);
        EntityId entity = Sts1NativePresentationAdapter.entity("owner");
        NativeInvocationComponent metadata =
                PresentationRegistry.world().get(entity, NativeInvocationComponent.class);
        assertEquals(9L, metadata.invocationId);
        assertEquals("Native2", metadata.nativeClass);
    }

    @Test
    public void removeAndClearReleaseNativeEntities() {
        Sts1NativePresentationAdapter.present(invocation("owner.a", 1L, Rect.ZERO));
        Sts1NativePresentationAdapter.present(invocation("owner.b", 1L, Rect.ZERO));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("owner.a"));

        Sts1NativePresentationAdapter.remove("owner.a");
        assertFalse(Sts1NativePresentationAdapter.hasEntity("owner.a"));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("owner.b"));

        Sts1NativePresentationAdapter.clear();
        assertFalse(Sts1NativePresentationAdapter.hasEntity("owner.b"));
    }

    @Test
    public void transientEffectProjectsObservedVfxInputAndCleanupDestroysIt() {
        Rect bounds = new Rect(3f, 4f, 20f, 30f);
        Sts1NativePresentationAdapter.present(invocation("effect:stable-42", 1L, bounds));
        EntityId effect = Sts1NativePresentationAdapter.entity("effect:stable-42");
        NativeRenderInputComponent input = PresentationRegistry.world().get(effect,
                NativeRenderInputComponent.class);
        assertNotNull(input);
        assertEquals("vfx-misc-root", input.nativeRenderFamily());
        assertEquals("effect:stable-42", input.ownerId());
        assertEquals(RenderPhase.ART_EFFECTS, input.phase());
        assertEquals(0f, input.z(), 0f);
        assertEquals("effect:stable-42", input.stableKey());
        assertEquals(bounds, input.bounds());
        assertTrue(input.visible());
        assertEquals(NativeRenderOwnership.OBSERVED, input.ownership());
        assertEquals("combat", input.sceneId());
        assertEquals(1L, input.frameId());

        NativeRenderInputSnapshot snapshot = new NativeRenderFrameExtractor()
                .extract(PresentationRegistry.world()).inputs().get(0);
        assertEquals(NativeRenderOwnership.OBSERVED, snapshot.ownership());
        assertEquals("combat", snapshot.sceneId());
        assertEquals(1L, snapshot.frameId());
        assertFalse(snapshot.ownership() == NativeRenderOwnership.DELEGATED_TO_ART);

        Sts1NativePresentationAdapter.remove("effect:stable-42");
        assertFalse(PresentationRegistry.world().contains(effect));
        Sts1NativePresentationAdapter.present(invocation("effect:clear", 2L, Rect.ZERO));
        EntityId cleared = Sts1NativePresentationAdapter.entity("effect:clear");
        Sts1NativePresentationAdapter.clear();
        assertFalse(PresentationRegistry.world().contains(cleared));
        Sts1NativePresentationAdapter.present(invocation("effect:transient", 3L, Rect.ZERO));
        EntityId transientEffect = Sts1NativePresentationAdapter.entity("effect:transient");
        Sts1NativePresentationAdapter.clearTransientEffects();
        assertFalse(PresentationRegistry.world().contains(transientEffect));
    }

    @Test
    public void effectClassDrivesNativeRenderFamilyWhileStayingObserved() {
        Sts1NativePresentationAdapter.present(effectInvocation("effect:combat-1",
                "com.megacrit.cardcrawl.vfx.combat.StrikeEffect", 1L));
        Sts1NativePresentationAdapter.present(effectInvocation("effect:scene-1",
                "com.megacrit.cardcrawl.vfx.scene.DeathScreenFadeEffect", 1L));
        Sts1NativePresentationAdapter.present(effectInvocation("effect:misc-1",
                "com.megacrit.cardcrawl.vfx.AbstractGameEffect", 1L));

        NativeRenderInputComponent combat = PresentationRegistry.world().get(
                Sts1NativePresentationAdapter.entity("effect:combat-1"),
                NativeRenderInputComponent.class);
        NativeRenderInputComponent scene = PresentationRegistry.world().get(
                Sts1NativePresentationAdapter.entity("effect:scene-1"),
                NativeRenderInputComponent.class);
        NativeRenderInputComponent misc = PresentationRegistry.world().get(
                Sts1NativePresentationAdapter.entity("effect:misc-1"),
                NativeRenderInputComponent.class);

        assertEquals("vfx-combat", combat.nativeRenderFamily());
        assertEquals("vfx-scene-world", scene.nativeRenderFamily());
        assertEquals("vfx-misc-root", misc.nativeRenderFamily());
        // Distinct classes yield distinct families, but the projection stays observe-only.
        assertFalse(combat.nativeRenderFamily().equals(scene.nativeRenderFamily()));
        for (NativeRenderInputComponent input : new NativeRenderInputComponent[] {combat, scene, misc}) {
            assertEquals(NativeRenderOwnership.OBSERVED, input.ownership());
            assertEquals(RenderPhase.ART_EFFECTS, input.phase());
            assertFalse(input.ownership() == NativeRenderOwnership.DELEGATED_TO_ART);
        }
    }

    @Test
    public void nonEffectOwnerDoesNotRetainTransientInputOnReusedEntity() {
        Sts1NativePresentationAdapter.present(invocation("effect:reused", 1L, Rect.ZERO));
        EntityId entity = Sts1NativePresentationAdapter.entity("effect:reused");
        assertTrue(PresentationRegistry.world().has(entity, NativeRenderInputComponent.class));

        Sts1NativePresentationAdapter.present(invocation("effect_reused", 2L, Rect.ZERO));
        assertEquals(entity, Sts1NativePresentationAdapter.entity("effect_reused"));
        assertFalse(PresentationRegistry.world().has(entity, NativeRenderInputComponent.class));
    }

    @Test
    public void bridgeOwnershipIsRecordedOnSurfaceInputWithoutFakingVfxFamily() {
        for (NativeRenderOwnership ownership : NativeRenderOwnership.values()) {
            String owner = "surface-owner-" + ownership.name();
            Sts1NativePresentationAdapter.present(invocation(owner, 5L, Rect.ZERO), ownership);

            NativeRenderInputComponent input = PresentationRegistry.world().get(
                    Sts1NativePresentationAdapter.entity(owner), NativeRenderInputComponent.class);
            assertNotNull(input);
            assertEquals(ownership, input.ownership());
            // Surface owners have no NRCC vfx family; the explicit surface namespace uses the
            // invocation's surfaceFamily (here the test helper's "surface" value).
            assertEquals("surface:surface", input.nativeRenderFamily());
            assertEquals(RenderPhase.NATIVE_RETAINED, input.phase());
            assertEquals(owner, input.stableKey());
            assertFalse("surface families must never be vfx families",
                    input.nativeRenderFamily().startsWith("vfx-"));
        }

        Sts1NativePresentationAdapter.present(new NativeRenderInvocation(1L, 5L, "combat",
                "surface.combat.hand", "Player", "renderHand", "sts1.combat.hand", "src",
                Rect.ZERO), NativeRenderOwnership.DELEGATED_TO_ART);
        NativeRenderInputComponent hand = PresentationRegistry.world().get(
                Sts1NativePresentationAdapter.entity("surface.combat.hand"),
                NativeRenderInputComponent.class);
        assertEquals("surface:sts1.combat.hand", hand.nativeRenderFamily());
    }

    @Test
    public void ownershipForMapsEveryDispositionMode() {
        assertEquals(NativeRenderOwnership.DELEGATED_TO_ART,
                Sts1NativePresentationAdapter.ownershipFor(
                        RenderDisposition.Mode.DELEGATE_TO_ART));
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY,
                Sts1NativePresentationAdapter.ownershipFor(
                        RenderDisposition.Mode.CAPTURE_AND_PASS));
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY,
                Sts1NativePresentationAdapter.ownershipFor(RenderDisposition.Mode.PASS_THROUGH));
        assertEquals(NativeRenderOwnership.OBSERVED,
                Sts1NativePresentationAdapter.ownershipFor(RenderDisposition.Mode.FAIL_OPEN));
        assertEquals(NativeRenderOwnership.OBSERVED,
                Sts1NativePresentationAdapter.ownershipFor(RenderDisposition.Mode.BLOCKED));
    }

    @Test
    public void effectOwnerStaysObservedEvenWhenBridgeSuppliesOwnership() {
        Sts1NativePresentationAdapter.present(
                effectInvocation("effect:delegated", "com.megacrit.cardcrawl.vfx.combat.StrikeEffect", 1L),
                NativeRenderOwnership.DELEGATED_TO_ART);

        NativeRenderInputComponent input = PresentationRegistry.world().get(
                Sts1NativePresentationAdapter.entity("effect:delegated"),
                NativeRenderInputComponent.class);
        assertNotNull(input);
        assertEquals(NativeRenderOwnership.OBSERVED, input.ownership());
        assertEquals(RenderPhase.ART_EFFECTS, input.phase());
        assertEquals("vfx-combat", input.nativeRenderFamily());
    }

    @Test
    public void reconcileInputDowngradesOwnershipWithoutCreatingInput() {
        // No entity yet: reconcile must be a strict no-op for an unprojected owner.
        Sts1NativePresentationAdapter.projectInput(invocation("never-projected", 1L, Rect.ZERO),
                NativeRenderOwnership.OBSERVED);
        assertFalse(Sts1NativePresentationAdapter.hasEntity("never-projected"));

        Sts1NativePresentationAdapter.present(
                invocation("reconciled", 1L, Rect.ZERO), NativeRenderOwnership.DELEGATED_TO_ART);
        EntityId entity = Sts1NativePresentationAdapter.entity("reconciled");
        Sts1NativePresentationAdapter.projectInput(invocation("reconciled", 1L, Rect.ZERO),
                NativeRenderOwnership.OBSERVED);

        NativeRenderInputComponent input =
                PresentationRegistry.world().get(entity, NativeRenderInputComponent.class);
        assertEquals(NativeRenderOwnership.OBSERVED, input.ownership());
    }

    @Test
    public void cleanupRemovesBridgedSurfaceInput() {
        NativeRenderOwnership ownership = NativeRenderOwnership.DELEGATED_TO_ART;
        Sts1NativePresentationAdapter.present(invocation("surface.remove", 1L, Rect.ZERO), ownership);
        EntityId removed = Sts1NativePresentationAdapter.entity("surface.remove");
        assertTrue(PresentationRegistry.world().has(removed, NativeRenderInputComponent.class));
        Sts1NativePresentationAdapter.remove("surface.remove");
        assertFalse(PresentationRegistry.world().contains(removed));

        Sts1NativePresentationAdapter.present(invocation("surface.clear", 1L, Rect.ZERO), ownership);
        EntityId cleared = Sts1NativePresentationAdapter.entity("surface.clear");
        Sts1NativePresentationAdapter.clear();
        assertFalse(PresentationRegistry.world().contains(cleared));

        // clearTransientEffects keeps non-effect surface owners and their input.
        Sts1NativePresentationAdapter.present(invocation("surface.keep", 1L, Rect.ZERO), ownership);
        EntityId kept = Sts1NativePresentationAdapter.entity("surface.keep");
        Sts1NativePresentationAdapter.clearTransientEffects();
        assertTrue(PresentationRegistry.world().contains(kept));
        assertTrue(PresentationRegistry.world().has(kept, NativeRenderInputComponent.class));
    }

    @Test
    public void clearSurfaceInputsWithdrawsOnlySurfaceInputs() {
        Sts1NativePresentationAdapter.present(invocation("surface.stale", 1L, Rect.ZERO),
                NativeRenderOwnership.DELEGATED_TO_ART);
        Sts1NativePresentationAdapter.present(invocation("effect:observed", 1L, Rect.ZERO));

        EntityId surface = Sts1NativePresentationAdapter.entity("surface.stale");
        EntityId effect = Sts1NativePresentationAdapter.entity("effect:observed");
        assertTrue(PresentationRegistry.world().has(surface, NativeRenderInputComponent.class));
        assertTrue(PresentationRegistry.world().has(effect, NativeRenderInputComponent.class));

        Sts1NativePresentationAdapter.clearSurfaceInputs();

        // The surface entity stays (retained presentation state) but its stale input is withdrawn.
        assertTrue(PresentationRegistry.world().contains(surface));
        assertFalse(PresentationRegistry.world().has(surface, NativeRenderInputComponent.class));
        // Effect owners and their observed inputs are untouched by the surface cleanup.
        assertTrue(PresentationRegistry.world().contains(effect));
        assertTrue(PresentationRegistry.world().has(effect, NativeRenderInputComponent.class));

        Sts1NativePresentationAdapter.clearTransientEffects();
        assertFalse(PresentationRegistry.world().contains(effect));
    }
}
