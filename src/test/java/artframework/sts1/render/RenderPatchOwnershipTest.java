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
 * <p>Native suppression is decided per invocation by {@link NativeRenderBridge}
 * dispositions: a patch may return {@code SpireReturn.Return(null)} only when the
 * bridge resolved a FULL_READY surface and returned {@code DELEGATE_TO_ART};
 * panic and unknown owners fail open to native rendering.
 */
public class RenderPatchOwnershipTest {

    private static final Path PATCH_ROOT = Paths.get("src/main/java/artframework/sts1/patch");

    /** Files that are allowed to contain SpireReturn.Return(null) for native suppression. */
    private static final Set<String> ALLOWED_SUPPRESS_PATCHES = new HashSet<String>(
            Arrays.asList(
                    "CombatHandRenderPatches.java",
                    "CombatControlsRenderPatches.java",
                    "CombatEnergyRenderPatches.java",
                    "CombatIntentRenderPatches.java",
                    "TopPanelRenderPatches.java",
                    "ProceedButtonRenderPatches.java",
                    "EventRenderPatches.java",
                    "SelectRenderPatches.java",
                    "RoomRenderPatches.java",
                    "MapRenderPatches.java",
                    "SkeletonRenderPatches.java",
                    "TransientEffectRenderPatches.java"));

    /**
     * Surfaces listed here must keep native-pixel authority. The set is empty
     * today: every suppressing patch gates through NativeRenderBridge
     * dispositions instead of an unconditional native-authority rule. If a new
     * surface is added here, it must come with a focused ART_DELEGATED test.
     */
    private static final Set<String> EXPECTED_NATIVE_PIXEL_AUTHORITATIVE = new HashSet<String>();

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
                            + "approved ART_DELEGATED allowlist. Listed files gate suppression "
                            + "through NativeRenderBridge dispositions (DELEGATE_TO_ART only when "
                            + "the surface is FULL_READY; panic and unknown owners fail open). Add "
                            + "the file together with an ART_DELEGATED manifest entry, a "
                            + "justification, and a focused suppression-gate test.",
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
    public void controlsPatchSuppressesNativeEndTurnRender() throws IOException {
        Path file = PATCH_ROOT.resolve("CombatControlsRenderPatches.java");
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue("CombatControlsRenderPatches must suppress EndTurnButton.render",
                text.contains("EndTurnButton") && text.contains("SpireReturn.Return(null)"));
    }

    @Test
    public void energyAndIntentPatchesSuppressNativeWhenFullReady() throws IOException {
        for (String name : Arrays.asList(
                "CombatEnergyRenderPatches.java",
                "CombatIntentRenderPatches.java")) {
            Path file = PATCH_ROOT.resolve(name);
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            assertTrue(
                    name + " must suppress native rendering when ART owns the surface",
                    text.contains("SpireReturn.Return(null)"));
        }
    }

    @Test
    public void topPanelAndProceedPatchesSuppressNativeWhenFullReady() throws IOException {
        for (String name : Arrays.asList(
                "TopPanelRenderPatches.java",
                "ProceedButtonRenderPatches.java")) {
            Path file = PATCH_ROOT.resolve(name);
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            assertTrue(
                    name + " must suppress native rendering when ART owns the surface",
                    text.contains("SpireReturn.Return(null)"));
        }
    }

    @Test
    public void mapPatchSuppressesNativeWhenFullReady() throws IOException {
        // Phase 4: map is now ART-delegated when FULL + mounted + map scene.
        String name = "MapRenderPatches.java";
        Path file = PATCH_ROOT.resolve(name);
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue(
                name + " must suppress native rendering when ART owns the surface",
                text.contains("SpireReturn.Return(null)"));
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
    public void delegatedSurfacesAreNotNativeAuthoritative() {
        // Combat hand/controls/energy/intents/proceed, top-panel, and Phase 3 room/overlay
        // surfaces are ART-delegated when FULL + mounted + scene match.
        assertFalse("sts1.combat.hand is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.combat.hand"));
        assertFalse("sts1.combat.controls is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.combat.controls"));
        assertFalse("sts1.combat.energy is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.combat.energy"));
        assertFalse("sts1.combat.intents is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.combat.intents"));
        assertFalse("sts1.combat.proceed is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.combat.proceed"));
        assertFalse("sts1.top_panel is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.top_panel"));
        assertFalse("sts1.event is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.event"));
        assertFalse("sts1.select.grid is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.select.grid"));
        assertFalse("sts1.select.hand is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.select.hand"));
        assertFalse("sts1.reward.combat is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.reward.combat"));
        assertFalse("sts1.reward.card is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.reward.card"));
        assertFalse("sts1.reward.boss_relic is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.reward.boss_relic"));
        assertFalse("sts1.rest is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.rest"));
        assertFalse("sts1.shop is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.shop"));
        assertFalse("sts1.treasure is ART-delegated",
                SurfaceDrawPlan.keepsNativePixelAuthority("sts1.treasure"));
    }
}
