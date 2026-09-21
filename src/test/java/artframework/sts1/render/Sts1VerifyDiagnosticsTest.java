package artframework.sts1.render;

import artframework.api.ArtFramework;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Sts1VerifyDiagnosticsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void offModeIsDisabledAndSupported() {
        assertEquals(Sts1VerifyDiagnostics.Mode.OFF, Sts1VerifyDiagnostics.configuredMode());
        assertEquals("disabled", Sts1VerifyDiagnostics.submissionStatus());
        assertTrue(Sts1VerifyDiagnostics.modeSupported());
    }

    @Test
    public void visualModesReportUnsupportedWithoutPreNativeBoundary() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BACKGROUND);
        assertFalse(Sts1VerifyDiagnostics.modeSupported());
        assertEquals("unsupported", Sts1VerifyDiagnostics.submissionStatus());

        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.GUIDES);
        assertEquals("unsupported", Sts1VerifyDiagnostics.submissionStatus());

        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BOUNDS);
        assertEquals("unsupported", Sts1VerifyDiagnostics.submissionStatus());
    }

    @Test
    public void probeSliceReportsConfiguredModeAndSubmissionStatus() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BACKGROUND);
        Map<String, Object> slice = Sts1VerifyDiagnostics.probeSlice();
        assertEquals("background", slice.get("configuredMode"));
        assertEquals("unsupported", slice.get("submissionStatus"));
        assertEquals(Boolean.FALSE, slice.get("modeSupported"));
        assertEquals("stage.draw", slice.get("nativeInterval"));
        assertEquals("post_native_overlay", slice.get("artInterval"));
        assertFalse(slice.containsKey("lastError"));
    }

    @Test
    public void recordedErrorSurfacesWithoutChangingConfiguredMode() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.GUIDES);
        Sts1VerifyDiagnostics.recordError(new IllegalStateException("nope"));
        assertEquals(Sts1VerifyDiagnostics.Mode.GUIDES, Sts1VerifyDiagnostics.configuredMode());
        assertEquals("IllegalStateException:nope",
                Sts1VerifyDiagnostics.probeSlice().get("lastError"));
    }

    @Test
    public void settingNullModeFallsBackToOff() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BOUNDS);
        Sts1VerifyDiagnostics.setMode(null);
        assertEquals(Sts1VerifyDiagnostics.Mode.OFF, Sts1VerifyDiagnostics.configuredMode());
    }

    @Test
    public void consoleModeParserAcceptsOnlyDocumentedModes() throws Exception {
        Method parse = artframework.console.ArtCommand.class.getDeclaredMethod(
                "parseVerifyMode", String.class);
        parse.setAccessible(true);
        assertEquals(Sts1VerifyDiagnostics.Mode.OFF, parse.invoke(null, "off"));
        assertEquals(Sts1VerifyDiagnostics.Mode.BACKGROUND, parse.invoke(null, "BACKGROUND"));
        assertEquals(Sts1VerifyDiagnostics.Mode.GUIDES, parse.invoke(null, " guides "));
        assertEquals(Sts1VerifyDiagnostics.Mode.BOUNDS, parse.invoke(null, "bounds"));
        assertNull(parse.invoke(null, "nonsense"));
        assertNull(parse.invoke(null, (Object) null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void uiProbeBackendExposesVerifyDiagnostics() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BOUNDS);
        Map<String, Object> snap = ArtFramework.probe().asMap();
        Map<String, Object> backend = (Map<String, Object>) snap.get("backend");
        assertTrue(backend.containsKey("verify"));
        Map<String, Object> verify = (Map<String, Object>) backend.get("verify");
        assertEquals("bounds", verify.get("configuredMode"));
        assertEquals("unsupported", verify.get("submissionStatus"));
    }
}
