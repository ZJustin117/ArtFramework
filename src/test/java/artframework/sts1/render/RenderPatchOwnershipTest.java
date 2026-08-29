package artframework.sts1.render;

import artframework.context.SurfaceIds;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    private static final Path COVERAGE_MANIFEST = Paths.get("tools/nrcc/manifests/sts1-native-coverage.yaml");
    private static final Path COVERAGE_SDD = Paths.get("docs/design/native-render-coverage-sdd.md");

    private static final String SUPPRESSION_OWNERSHIP_MESSAGE =
            "Suppressing patches must be allowlisted and backed by ART_DELEGATED manifest "
                    + "metadata. ART_DELEGATED means delegated with exposed pixel-supply gaps "
                    + "when the SDD records incomplete supply; it is not native-authoritative "
                    + "coverage.";

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
                    "SkeletonRenderPatches.java"));

    private static final Map<String, Set<String>> EXPECTED_DELEGATED_SURFACES_BY_PATCH = delegatedSurfacesByPatch();

    /**
     * Surfaces listed here must keep native-pixel authority according to the SDD's
     * observe-only/native-authoritative declarations. Suppressing surfaces are not added
     * here just because their current ART pixel supply is incomplete; they remain
     * ART_DELEGATED with exposed pixel-supply gaps.
     */
    private static final Set<String> EXPECTED_NATIVE_PIXEL_AUTHORITATIVE = new HashSet<String>(
            Collections.singletonList(SurfaceIds.COMBAT_TARGETING));

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
                            + "approved ART_DELEGATED allowlist. " + SUPPRESSION_OWNERSHIP_MESSAGE,
                    ALLOWED_SUPPRESS_PATCHES.contains(name));
        }
    }

    @Test
    public void suppressingPatchesHaveDelegatedManifestMetadata() throws IOException {
        Map<String, List<ManifestEntry>> byHook = manifestEntriesByHook();

        for (String patchName : ALLOWED_SUPPRESS_PATCHES) {
            Path patch = PATCH_ROOT.resolve(patchName);
            String patchText = new String(Files.readAllBytes(patch), StandardCharsets.UTF_8);
            if (!patchText.contains("SpireReturn.Return(null)")) {
                continue;
            }

            List<ManifestEntry> entries = byHook.get("artframework/sts1/patch/" + patchName);
            assertTrue(
                    patchName + " suppresses native draw but lacks manifest support. "
                            + SUPPRESSION_OWNERSHIP_MESSAGE,
                    entries != null && !entries.isEmpty());

            for (String surfaceId : EXPECTED_DELEGATED_SURFACES_BY_PATCH.get(patchName)) {
                assertTrue(
                        patchName + " suppresses " + surfaceId
                                + " but lacks an ART_DELEGATED manifest entry with justification/test metadata. "
                                + SUPPRESSION_OWNERSHIP_MESSAGE,
                        hasDelegatedMetadata(entries, surfaceId));
            }
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
    public void abstractCardRenderIsNeverPatchedForSuppression() throws IOException {
        List<Path> patchFiles = Files.walk(PATCH_ROOT)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());
        for (Path file : patchFiles) {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            assertFalse(
                    file.getFileName() + " must not target AbstractCard.render; card pixels stay native",
                    text.contains("clz = AbstractCard.class") && text.contains("method = \"render\""));
            assertFalse(
                    file.getFileName() + " must not target AbstractCard.render by class name",
                    text.contains("com.megacrit.cardcrawl.cards.AbstractCard")
                            && text.contains("method = \"render\"")
                            && text.contains("SpirePatch"));
        }
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
                    "Surface " + surfaceId + " must keep native pixel authority per SDD observe-only policy",
                    SurfaceDrawPlan.keepsNativePixelAuthority(surfaceId));
        }
    }

    @Test
    public void nativePixelAuthorityMatchesSddObserveOnlySurface() throws IOException {
        String sdd = new String(Files.readAllBytes(COVERAGE_SDD), StandardCharsets.UTF_8);
        assertTrue("SDD must document targeting as observe-only/native-authoritative",
                sdd.contains("Targeting arrow")
                        && sdd.contains("no (observe-only by default")
                        && sdd.contains("native arrow stays authoritative"));
        assertTrue("targeting keeps native pixel authority",
                SurfaceDrawPlan.keepsNativePixelAuthority(SurfaceIds.COMBAT_TARGETING));

        String[] delegatedWithGaps = {
            SurfaceIds.COMBAT_CONTROLS,
            SurfaceIds.COMBAT_ENERGY,
            SurfaceIds.COMBAT_INTENTS,
            SurfaceIds.MAP,
            SurfaceIds.EVENT,
            SurfaceIds.SELECT_GRID,
            SurfaceIds.SELECT_HAND,
            SurfaceIds.REWARD_COMBAT,
            SurfaceIds.REST,
            SurfaceIds.SHOP,
            SurfaceIds.TREASURE
        };
        for (String surfaceId : delegatedWithGaps) {
            assertFalse(
                    surfaceId + " has delegated-with-gap supply in the SDD, not native pixel authority",
                    SurfaceDrawPlan.keepsNativePixelAuthority(surfaceId));
        }
        assertTrue("SDD must label incomplete supply as delegated with exposed gaps",
                sdd.contains("ART_DELEGATED with exposed gap"));
    }

    @Test
    public void delegatedSurfacesAreNotNativeAuthoritative() {
        // Combat hand/controls/energy/intents/proceed, top-panel, and room/overlay surfaces are
        // ART_DELEGATED when FULL_READY. Minimal/incomplete supply stays visible as SDD gaps and
        // must not be mislabeled native-authoritative.
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

    @Test
    public void targetingRemainsNativeContinuingObserveFirst() throws IOException {
        String text = new String(Files.readAllBytes(PATCH_ROOT.resolve("CombatTargetingRenderPatches.java")),
                StandardCharsets.UTF_8);
        assertTrue("targeting patch must observe/capture before native rendering", text.contains("beginSurface"));
        assertTrue("targeting patch must continue native rendering", text.contains("SpireReturn.Continue()"));
        assertFalse("targeting patch must not suppress native rendering", text.contains("SpireReturn.Return(null)"));
        assertTrue("targeting surface remains native pixel authoritative",
                SurfaceDrawPlan.keepsNativePixelAuthority(SurfaceIds.COMBAT_TARGETING));
    }

    @Test
    public void suppressionOwnershipMessageMentionsDelegatedGapsNotNativeCoverage() {
        assertTrue(SUPPRESSION_OWNERSHIP_MESSAGE.contains("ART_DELEGATED"));
        assertTrue(SUPPRESSION_OWNERSHIP_MESSAGE.contains("exposed pixel-supply gaps"));
        assertTrue(SUPPRESSION_OWNERSHIP_MESSAGE.contains("not native-authoritative coverage"));
    }

    private static Map<String, Set<String>> delegatedSurfacesByPatch() {
        Map<String, Set<String>> m = new LinkedHashMap<String, Set<String>>();
        m.put("CombatHandRenderPatches.java", set(SurfaceIds.COMBAT_HAND));
        m.put("CombatControlsRenderPatches.java", set(SurfaceIds.COMBAT_CONTROLS));
        m.put("CombatEnergyRenderPatches.java", set(SurfaceIds.COMBAT_ENERGY));
        m.put("CombatIntentRenderPatches.java", set(SurfaceIds.COMBAT_INTENTS));
        m.put("TopPanelRenderPatches.java", set(SurfaceIds.TOP_PANEL));
        m.put("ProceedButtonRenderPatches.java", set(SurfaceIds.COMBAT_PROCEED));
        m.put("EventRenderPatches.java", set(SurfaceIds.EVENT));
        m.put("SelectRenderPatches.java", set(SurfaceIds.SELECT_GRID, SurfaceIds.SELECT_HAND));
        m.put("RoomRenderPatches.java", set(SurfaceIds.REWARD_COMBAT, SurfaceIds.REST,
                SurfaceIds.SHOP, SurfaceIds.TREASURE));
        m.put("MapRenderPatches.java", set(SurfaceIds.MAP));
        m.put("SkeletonRenderPatches.java", set(SurfaceIds.SKELETON));
        return m;
    }

    private static Set<String> set(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }

    private static boolean hasDelegatedMetadata(List<ManifestEntry> entries, String surfaceId) {
        for (ManifestEntry entry : entries) {
            if (surfaceId.equals(entry.surfaceId)
                    && "ART_DELEGATED".equals(entry.policy)
                    && !entry.justification.isEmpty()
                    && !entry.test.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, List<ManifestEntry>> manifestEntriesByHook() throws IOException {
        List<ManifestEntry> entries = readManifestEntries();
        Map<String, List<ManifestEntry>> byHook = new HashMap<String, List<ManifestEntry>>();
        for (ManifestEntry entry : entries) {
            if (entry.hook.isEmpty()) {
                continue;
            }
            List<ManifestEntry> list = byHook.get(entry.hook);
            if (list == null) {
                list = new java.util.ArrayList<ManifestEntry>();
                byHook.put(entry.hook, list);
            }
            list.add(entry);
        }
        return byHook;
    }

    private static List<ManifestEntry> readManifestEntries() throws IOException {
        List<String> lines = Files.readAllLines(COVERAGE_MANIFEST, StandardCharsets.UTF_8);
        List<ManifestEntry> entries = new java.util.ArrayList<ManifestEntry>();
        ManifestEntry current = null;
        String lastKey = "";
        for (String line : lines) {
            if (line.startsWith("- ownerId:")) {
                if (current != null) entries.add(current);
                current = new ManifestEntry();
                current.ownerId = valueAfterColon(line);
                lastKey = "ownerId";
            } else if (current != null && line.startsWith("  ") && line.contains(":")) {
                String trimmed = line.trim();
                String key = trimmed.substring(0, trimmed.indexOf(':'));
                String value = valueAfterColon(trimmed);
                if ("surfaceId".equals(key)) current.surfaceId = stripQuotes(value);
                if ("hook".equals(key)) current.hook = stripQuotes(value);
                if ("policy".equals(key)) current.policy = stripQuotes(value);
                if ("justification".equals(key)) current.justification = stripQuotes(value);
                if ("test".equals(key)) current.test = stripQuotes(value);
                lastKey = key;
            } else if (current != null && "justification".equals(lastKey) && line.startsWith("    ")) {
                current.justification = (current.justification + " " + line.trim()).trim();
            }
        }
        if (current != null) entries.add(current);
        return entries;
    }

    private static String valueAfterColon(String line) {
        int index = line.indexOf(':');
        if (index < 0 || index + 1 >= line.length()) {
            return "";
        }
        return stripQuotes(line.substring(index + 1).trim());
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        if ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static final class ManifestEntry {
        String ownerId = "";
        String surfaceId = "";
        String hook = "";
        String policy = "";
        String justification = "";
        String test = "";
    }
}
