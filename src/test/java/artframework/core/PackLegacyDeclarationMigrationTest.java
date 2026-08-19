package artframework.core;

import artframework.api.ArtFramework;
import artframework.component.EffectDecl;
import artframework.context.SurfaceIds;
import artframework.ecs.ArtEcs;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TE-23: proves that a legacy {@link PresentPack} declaration field can never be the authority.
 *
 * <p>{@code PresentPack} normalizes every non-empty legacy declaration into a {@link PackOperation},
 * and {@code PresentPacks.activate} always enables those operations before projecting. So whenever a
 * pack is active with a non-empty legacy field, the matching ECS contribution necessarily exists and
 * the removed {@code pack.<legacyField>} fallback branches were unreachable.
 */
public class PackLegacyDeclarationMigrationTest {

    private static final EffectDecl EFFECT =
            new EffectDecl("lightwave", Collections.<String, Object>emptyMap());

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    private static PresentPack legacyPack(String id) {
        return PresentPack.builder(id)
                .profileId(id + "_prof")
                .effectDefault("panel", EFFECT)
                .surfaceEffect(SurfaceIds.EVENT, EFFECT)
                .fullFrameEffect(EFFECT)
                .bindSurface(SurfaceIds.EVENT)
                .build();
    }

    private static void registerProfile(PresentPack pack) {
        Theme theme = LightwaveTheme.createDefault();
        theme.setName(pack.profileId);
        ArtFramework.registerPresentProfile(pack.profileId, theme, pack.id);
    }

    private static void assertEveryContributionPresent(String packId) {
        assertTrue("effect defaults must be ECS-owned while the pack is active",
                PackEffectDefaults.hasContribution(ArtEcs.world()));
        assertTrue("surface effects must be ECS-owned while the pack is active",
                PackSurfaceEffects.hasContribution(ArtEcs.world(), packId));
        assertTrue("full-frame effects must be ECS-owned while the pack is active",
                PackFullFrameEffects.hasContribution(ArtEcs.world(), packId));
        assertTrue("surface bindings must be ECS-owned while the pack is active",
                PackSurfaceBindings.hasContribution(ArtEcs.world(), packId));
    }

    private static void assertNoContribution(String packId) {
        assertFalse(PackEffectDefaults.hasContribution(ArtEcs.world()));
        assertFalse(PackSurfaceEffects.hasContribution(ArtEcs.world(), packId));
        assertFalse(PackFullFrameEffects.hasContribution(ArtEcs.world(), packId));
        assertFalse(PackSurfaceBindings.hasContribution(ArtEcs.world(), packId));
    }

    @Test
    public void everyNonEmptyLegacyDeclarationBecomesAnOperation() {
        PresentPack pack = legacyPack("mod.legacy-normalize");
        assertTrue("legacy fields must not stay declaration-only", pack.operations.size() >= 4);

        boolean defaults = false;
        boolean surfaceEffects = false;
        boolean fullFrame = false;
        boolean bindings = false;
        for (PackOperation operation : pack.operations) {
            if (operation instanceof PackOperations.EffectDefaultsOperation) defaults = true;
            if (operation instanceof PackOperations.SurfaceEffectsOperation) surfaceEffects = true;
            if (operation instanceof PackOperations.FullFrameEffectsOperation) fullFrame = true;
            if (operation instanceof PackOperations.SurfaceBindingsOperation) bindings = true;
        }
        assertTrue("effectDefaults must normalize into an operation", defaults);
        assertTrue("surfaceEffects must normalize into an operation", surfaceEffects);
        assertTrue("fullFrameEffects must normalize into an operation", fullFrame);
        assertTrue("bindSurfaces must normalize into an operation", bindings);
    }

    @Test
    public void activationCreatesEveryEcsContributionForLegacyDeclarations() {
        PresentPack pack = legacyPack("mod.legacy-activate");
        registerProfile(pack);
        PresentPacks.register(pack);

        PresentPacks.activate(pack.id);
        assertEveryContributionPresent(pack.id);
    }

    @Test
    public void reactivatingTheAlreadyActivePackKeepsEveryContribution() {
        PresentPack pack = legacyPack("mod.legacy-reactivate");
        registerProfile(pack);
        PresentPacks.register(pack);

        PresentPacks.activate(pack.id);
        // Early-return branch in PresentPacks.activate: syncs projection without re-enabling.
        PresentPacks.activate(pack.id);
        assertEveryContributionPresent(pack.id);
    }

    @Test
    public void profileDrivenActivationCreatesEveryEcsContribution() {
        PresentPack pack = legacyPack("mod.legacy-profile");
        registerProfile(pack);
        PresentPacks.register(pack);

        ArtFramework.setProjectPresent(pack.profileId);
        assertEveryContributionPresent(pack.id);
    }

    @Test
    public void deactivationRemovesEveryEcsContribution() {
        PresentPack pack = legacyPack("mod.legacy-deactivate");
        registerProfile(pack);
        PresentPacks.register(pack);

        PresentPacks.activate(pack.id);
        PresentPacks.deactivate(pack.id);
        assertNoContribution(pack.id);
    }

    @Test
    public void failedActivationLeavesNoPartialEcsContribution() {
        PresentPack pack = PresentPack.builder("mod.legacy-abort")
                .effectDefault("panel", EFFECT)
                .surfaceEffect(SurfaceIds.EVENT, EFFECT)
                .fullFrameEffect(EFFECT)
                .bindSurface(SurfaceIds.EVENT)
                // Unresolvable template resource forces the operation log to roll back.
                .template("mod.legacy-abort.t", "present-packs/does-not-exist.json")
                .build();
        PresentPacks.register(pack);

        try {
            PresentPacks.activate(pack.id);
            fail("expected activation failure for a missing template resource");
        } catch (RuntimeException expected) {
            // Abort must remove every applied contribution, so no fallback state can survive.
        }
        assertNoContribution(pack.id);
        assertFalse(PresentPackRuntime.isEnabled(pack.id));
    }

    @Test
    public void packResolversReadTheSingleArtWorld() {
        PresentPack pack = legacyPack("mod.legacy-world");
        registerProfile(pack);
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);

        // The resolver world must be the world PackWorld wrote to, otherwise a stale read could
        // observe no contribution and silently resurrect a legacy declaration path.
        assertSame(ArtEcs.world(), artframework.presentation.PresentationRegistry.world());
        assertSame(ArtEcs.world(),
                artframework.presentation.PresentationRegistry.context("c2-surfaces").world());
        assertEveryContributionPresent(pack.id);
    }

    /**
     * The deleted legacy branch pre-checked {@code PresentProfiles.contains}. The ECS branch never
     * had that guard and instead relies on {@code SurfacePresent.bind} rejecting an unknown profile,
     * so an unregistered profile must still leave no binding and no cleanup bookkeeping.
     */
    @Test
    public void unregisteredProfileBindsNothingAndRecordsNoCleanup() {
        PresentPack pack = PresentPack.builder("mod.unregistered-profile")
                .profileId("mod.never-registered")
                .bindSurface(SurfaceIds.EVENT)
                .build();
        PresentPacks.register(pack);

        PresentPacks.activate(pack.id);

        assertTrue("surface bindings must still be ECS-owned",
                PackSurfaceBindings.hasContribution(ArtEcs.world(), pack.id));
        assertNull("an unknown profile must not be bound",
                SurfacePresent.profileId(SurfaceIds.EVENT));
        assertEquals("a rejected bind must not record cleanup state",
                "[]", PresentPackApply.probeSummary().get("boundSurfaces").toString());

        // Deactivation must stay clean when nothing was applied.
        PresentPacks.deactivate(pack.id);
        assertNull(SurfacePresent.profileId(SurfaceIds.EVENT));
    }
}
