package artframework.c1;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.UiTypes;
import artframework.component.ArtNodeTypes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pure registry tests (no scene2d on test classpath).
 */
public class C1NodeFactoriesTest {

    @After
    public void tearDown() {
        C1NodeFactories.global().resetBuiltinsForTests();
        ArtFramework.resetForTests();
    }

    @Test
    public void builtinsPresent() {
        C1NodeFactories r = C1NodeFactories.global();
        assertTrue(r.contains(UiTypes.BUTTON));
        assertTrue(r.contains(UiTypes.COL));
        assertTrue(r.contains(UiTypes.GRID));
        assertTrue(r.contains(UiTypes.TABS));
        assertTrue(r.contains(UiTypes.PROGRESS));
        assertTrue(r.contains(ArtNodeTypes.STS_BUTTON));
        assertTrue(r.contains(ArtNodeTypes.STS_MAP_NODE));
        assertNotNull(r.get(UiTypes.LABEL));
        assertFalse(r.contains(UiTypes.WINDOW));
        assertFalse(r.contains(UiTypes.REF));
    }

    @Test
    public void cannotUnregisterBuiltin() {
        try {
            C1NodeFactories.global().unregister(UiTypes.BUTTON);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("builtin"));
        }
    }

    @Test
    public void artFacadeExposesC1Nodes() {
        assertNotNull(ArtFramework.c1Nodes());
        assertTrue(ArtFramework.c1Nodes().contains(UiTypes.BUTTON));
    }

    @Test
    public void registerRequiresFactory() {
        try {
            C1NodeFactories.global().register(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("factory"));
        }
    }
}
