package artframework.sts1.render;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression gates for the Native Render Coverage Contract (NRCC).
 *
 * <p>Original STS renderers remain the visual authority. Any patch that returns
 * {@code SpireReturn.Return(null)} to suppress a native render call must be a
 * documented, tested, ART-owned surface or per-instance claim.
 */
public class RenderPatchOwnershipTest {

    private static final Path PATCH_ROOT = Paths.get("src/main/java/artframework/sts1/patch");

    /** Files that are allowed to contain SpireReturn.Return(null) for native suppression. */
    private static final Set<String> ALLOWED_SUPPRESS_PATCHES = new HashSet<String>(
            Arrays.asList(
                    "CombatHandRenderPatches.java",
                    "SkeletonRenderPatches.java",
                    "TransientEffectRenderPatches.java"));

    /**
     * The listed surface ids must keep native-pixel authority. If a new surface is
     * added here, it must come with an focused ART_DELEGATED test proving ART is
     * the sole pixel owner.
     */
    private static final Set<String> EXPECTED_NATIVE_PIXEL_AUTHORITATIVE = new HashSet<String>(
            Arrays.asList(
                    "sts1.combat.controls",
                    "sts1.combat.energy",
                    "sts1.combat.intents",
                    "sts1.map",
                    "sts1.event",
                    "sts1.select.grid",
                    "sts1.select.hand",
                    "sts1.reward.combat",
                    "sts1.reward.card",
                    "sts1.reward.boss_relic",
                    "sts1.rest",
                    "sts1.shop",
                    "sts1.treasure",
                    "sts1.skeleton"));

    @Test
    public void onlyApprovedPatchesSuppressNativeDraw() throws IOException {
        List<Path> patchFiles = Files.walk(PATCH_ROOT)
                .filter(p -> p.toString().endsWith("RenderPatches.java"))
                .collect(Collectors.toList());

        for (Path file : patchFiles) {
            String name = file.getFileName().toString();
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            boolean containsSuppress = text.contains("SpireReturn.Return(null)");
            if (!containsSuppress) {
                continue;
            }
            assertTrue(
                    "Patch " + name + " contains SpireReturn.Return(null) but is not on the "
                            + "approved ART_DELEGATED list (CombatHandRenderPatches, SkeletonRenderPatches, "
                            + "TransientEffectRenderPatches). Native STS renderers must not be suppressed.",
                    ALLOWED_SUPPRESS_PATCHES.contains(name));
        }
    }

    @Test
    public void combatHandPatchSuppressesRenderHand() throws IOException {
        Path file = PATCH_ROOT.resolve("CombatHandRenderPatches.java");
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue("CombatHandRenderPatches must suppress AbstractPlayer.renderHand",
                text.contains("AbstractPlayer") && text.contains("renderHand")
                        && text.contains("SpireReturn.Return(null)"));
    }

    @Test
    public void controlsEnergyIntentPatchesDoNotSuppressNative() throws IOException {
        for (String name : Arrays.asList(
                "CombatControlsRenderPatches.java",
                "CombatEnergyRenderPatches.java",
                "CombatIntentRenderPatches.java")) {
            Path file = PATCH_ROOT.resolve(name);
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            assertFalse(
                    name + " must not suppress native rendering; original STS renderer stays authoritative",
                    text.contains("SpireReturn.Return(null)"));
        }
    }

    @Test
    public void mapEventSelectRoomPatchesDoNotSuppressNative() throws IOException {
        for (String name : Arrays.asList(
                "MapRenderPatches.java",
                "EventRenderPatches.java",
                "SelectRenderPatches.java",
                "RoomRenderPatches.java")) {
            Path file = PATCH_ROOT.resolve(name);
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            assertFalse(
                    name + " must not suppress native rendering; original STS renderer stays authoritative",
                    text.contains("SpireReturn.Return(null)"));
        }
    }

    @Test
    public void surfaceDrawPlanKeepsNativePixelAuthorityForExpectedSurfaces() {
        for (String surfaceId : EXPECTED_NATIVE_PIXEL_AUTHORITATIVE) {
            assertTrue(
                    "Surface " + surfaceId + " must keep native pixel authority",
                    SurfaceDrawPlan.keepsNativePixelAuthority(surfaceId));
        }
    }

    @Test
    public void combatHandIsTheOnlySurfaceAllowedToSuppressNative() {
        // Hand is explicitly ART-delegated; every other surface in the set keeps
        // native-pixel authority. This test guards against accidentally adding a new
        // surface to keepsNativePixelAuthority without an ART_DELEGATED test.
        assertFalse(
                "sts1.combat.hand is ART-delegated and must not be in the native-authoritative set",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.combat.hand"));
    }
}
