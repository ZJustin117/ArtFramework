package artframework.presentation;

import artframework.api.ArtFramework;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * NRM-14: {@link PresentationRegistry#probeAll()} must bound per-scope enumeration of
 * per-instance transient-effect entities ({@code effect:*}) while always enumerating every
 * stable/named entity, and must report additive totals/truncation without changing the shape.
 */
public class PresentationRegistryProbeBoundTest {

    private static final String SCOPE = "probe-bound-scope";

    @After public void tearDown() { ArtFramework.resetForTests(); }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> scopeOf(List<Map<String, Object>> probe, String scope) {
        for (Map<String, Object> row : probe) {
            if (scope.equals(row.get("scope"))) {
                return row;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entitiesOf(Map<String, Object> scope) {
        return (List<Map<String, Object>>) scope.get("entities");
    }

    /** Stable/named projection entity, distinct from the transient-effect owner convention. */
    private static void createStable(PresentationContext context, String localId, String name) {
        context.create(new PresentationKey("probe", localId), name, "native_render", "sts1");
    }

    /** Per-instance transient-effect entity as projected by the transient-effect pipeline. */
    private static void createTransient(PresentationContext context, int index) {
        String instance = "inst-" + index;
        context.create(new PresentationKey("probe", "effect_" + instance),
                "effect:" + instance, "native_render", "vfx");
    }

    private static boolean containsName(List<Map<String, Object>> entities, String name) {
        for (Map<String, Object> row : entities) {
            if (name.equals(row.get("name"))) {
                return true;
            }
        }
        return false;
    }

    @Test public void belowCapEnumeratesEveryEntityPerScopeAndReportsNoTruncation() {
        PresentationContext context = PresentationRegistry.context(SCOPE);
        createStable(context, "sts1.combat.hand", "sts1.combat.hand");
        createStable(context, "sts1.combat.energy", "sts1.combat.energy");
        createStable(context, "context", "context");
        for (int i = 0; i < 3; i++) {
            createTransient(context, i);
        }

        Map<String, Object> scope = scopeOf(PresentationRegistry.probeAll(), SCOPE);
        assertNotNull(scope);

        assertEquals(Integer.valueOf(6), scope.get("entitiesTotal"));
        assertEquals(Integer.valueOf(6), scope.get("entitiesIncluded"));
        assertEquals(Boolean.FALSE, scope.get("entitiesTruncated"));
        assertEquals(6, entitiesOf(scope).size());
        // Shape and per-entity keys are unchanged.
        assertEquals(SCOPE, scope.get("scope"));
        Map<String, Object> row = entitiesOf(scope).get(0);
        assertTrue(row.containsKey("entity"));
        assertTrue(row.containsKey("key"));
        assertTrue(row.containsKey("name"));
        assertTrue(row.containsKey("type"));
        assertTrue(row.containsKey("source"));
        assertTrue(row.containsKey("parent"));
        assertTrue(row.containsKey("children"));
        assertTrue(row.containsKey("mounted"));
        assertTrue(row.containsKey("ready"));
        assertTrue(row.containsKey("components"));
        assertTrue(containsName(entitiesOf(scope), "effect:inst-0"));
        assertTrue(containsName(entitiesOf(scope), "sts1.combat.hand"));
    }

    @Test public void aboveCapBoundsOnlyTransientEntitiesAndKeepsStableEntities() {
        PresentationContext context = PresentationRegistry.context(SCOPE);
        PresentationRegistry.setMaxProbeEffectEntities(64);
        assertEquals(64, PresentationRegistry.maxProbeEffectEntities());
        createStable(context, "sts1.combat.hand", "sts1.combat.hand");
        createStable(context, "sts1.combat.energy", "sts1.combat.energy");
        createStable(context, "context", "context");
        int transientCount = 1000;
        for (int i = 0; i < transientCount; i++) {
            createTransient(context, i);
        }

        Map<String, Object> scope = scopeOf(PresentationRegistry.probeAll(), SCOPE);
        assertNotNull(scope);

        int stableCount = 3;
        int boundedMax = stableCount + PresentationRegistry.maxProbeEffectEntities();
        assertEquals(Integer.valueOf(stableCount + transientCount), scope.get("entitiesTotal"));
        assertEquals(Integer.valueOf(boundedMax), scope.get("entitiesIncluded"));
        assertEquals(Boolean.TRUE, scope.get("entitiesTruncated"));
        assertEquals(boundedMax, entitiesOf(scope).size());

        // Every stable/named entity survives the cap.
        assertTrue(containsName(entitiesOf(scope), "sts1.combat.hand"));
        assertTrue(containsName(entitiesOf(scope), "sts1.combat.energy"));
        assertTrue(containsName(entitiesOf(scope), "context"));
    }

    @Test public void capSetterIsHonoredAndNegativeClampsToZero() {
        PresentationContext context = PresentationRegistry.context(SCOPE);
        for (int i = 0; i < 10; i++) {
            createTransient(context, i);
        }
        createStable(context, "sts1.combat.hand", "sts1.combat.hand");

        PresentationRegistry.setMaxProbeEffectEntities(2);
        Map<String, Object> scope = scopeOf(PresentationRegistry.probeAll(), SCOPE);
        assertEquals(Integer.valueOf(11), scope.get("entitiesTotal"));
        assertEquals(Integer.valueOf(3), scope.get("entitiesIncluded"));
        assertEquals(Boolean.TRUE, scope.get("entitiesTruncated"));
        assertTrue(containsName(entitiesOf(scope), "sts1.combat.hand"));

        PresentationRegistry.setMaxProbeEffectEntities(-5);
        assertEquals(0, PresentationRegistry.maxProbeEffectEntities());
        scope = scopeOf(PresentationRegistry.probeAll(), SCOPE);
        assertEquals(Integer.valueOf(1), scope.get("entitiesIncluded"));
        assertEquals(Boolean.TRUE, scope.get("entitiesTruncated"));
        assertTrue(containsName(entitiesOf(scope), "sts1.combat.hand"));
    }

    @Test public void resetForTestsRestoresDefaultCap() {
        PresentationRegistry.setMaxProbeEffectEntities(1);
        assertEquals(1, PresentationRegistry.maxProbeEffectEntities());

        PresentationRegistry.resetForTests();

        assertEquals(PresentationRegistry.DEFAULT_MAX_PROBE_EFFECT_ENTITIES,
                PresentationRegistry.maxProbeEffectEntities());
    }

    @Test public void transientPredicateMatchesOwnerConventionAndIgnoredStableShapes() {
        assertTrue(PresentationRegistry.isTransientEffectEntity(
                new NodeIdentityComponent(new PresentationKey("sts1.native", "effect_x"), "effect:x",
                        "native_render", "vfx")));
        assertTrue("localized key fallback must still match",
                PresentationRegistry.isTransientEffectEntity(
                        new NodeIdentityComponent(new PresentationKey("sts1.native", "effect_x"),
                                "unexpected-name", "native_render", "vfx")));
        assertFalse(PresentationRegistry.isTransientEffectEntity(
                new NodeIdentityComponent(new PresentationKey("sts1.native", "sts1.combat.hand"),
                        "sts1.combat.hand", "native_render", "sts1")));
        assertFalse(PresentationRegistry.isTransientEffectEntity(
                new NodeIdentityComponent(new PresentationKey("render.surface", "sts1.combat.hand"),
                        "sts1.combat.hand", "render", "artframework")));
        assertFalse(PresentationRegistry.isTransientEffectEntity(null));
    }
}
