package artframework.render;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LightwaveDiagnosticsTest {
    @After
    public void tearDown() {
        LightwaveDiagnostics.resetForTests();
    }

    @Test
    public void defaultsLeaveEveryRenderPathEnabled() {
        assertTrue(LightwaveDiagnostics.c2EffectsEnabled());
        assertTrue(LightwaveDiagnostics.c2ItemsEnabled());
        assertTrue(LightwaveDiagnostics.c2PanelsEnabled());
        assertFalse(LightwaveDiagnostics.forceFallback());
    }

    @Test
    public void switchesAreIndependent() {
        LightwaveDiagnostics.setC2EffectsEnabled(false);
        LightwaveDiagnostics.setC2ItemsEnabled(false);
        LightwaveDiagnostics.setC2PanelsEnabled(false);
        LightwaveDiagnostics.setForceFallback(true);
        assertFalse(LightwaveDiagnostics.c2EffectsEnabled());
        assertFalse(LightwaveDiagnostics.c2ItemsEnabled());
        assertFalse(LightwaveDiagnostics.c2PanelsEnabled());
        assertTrue(LightwaveDiagnostics.forceFallback());
    }
}
