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
    public void backgroundStaysUnsupportedWithoutPreNativeBoundary() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BACKGROUND);
        assertFalse(Sts1VerifyDiagnostics.modeSupported());
        assertEquals("unsupported", Sts1VerifyDiagnostics.submissionStatus());
        assertFalse(Sts1VerifyDiagnostics.overlayDrawEnabled());
    }

    @Test
    public void overlayModesReportReadyWithoutPreNativeBoundary() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.GUIDES);
        assertTrue(Sts1VerifyDiagnostics.modeSupported());
        assertEquals("ready", Sts1VerifyDiagnostics.submissionStatus());
        assertTrue(Sts1VerifyDiagnostics.overlayDrawEnabled());

        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BOUNDS);
        assertTrue(Sts1VerifyDiagnostics.modeSupported());
        assertEquals("ready", Sts1VerifyDiagnostics.submissionStatus());
        assertTrue(Sts1VerifyDiagnostics.overlayDrawEnabled());
    }

    @Test
    public void overlayDrawPathIsModeGated() {
        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.OFF);
        assertFalse(VerifyGuideDrawPath.shouldDraw());
        assertFalse(VerifyGuideDrawPath.drawsGuides());
        assertFalse(VerifyGuideDrawPath.drawsBounds());

        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.GUIDES);
        assertTrue(VerifyGuideDrawPath.shouldDraw());
        assertTrue(VerifyGuideDrawPath.drawsGuides());
        assertFalse(VerifyGuideDrawPath.drawsBounds());

        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BOUNDS);
        assertTrue(VerifyGuideDrawPath.shouldDraw());
        assertFalse(VerifyGuideDrawPath.drawsGuides());
        assertTrue(VerifyGuideDrawPath.drawsBounds());

        Sts1VerifyDiagnostics.setMode(Sts1VerifyDiagnostics.Mode.BACKGROUND);
        assertFalse(VerifyGuideDrawPath.shouldDraw());
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
    public void consoleNativeParserAcceptsFamilySwitchAndRejectsBadInput() throws Exception {
        Method parse = artframework.console.ArtCommand.class.getDeclaredMethod(
                "parseVerifyNative", String[].class);
        parse.setAccessible(true);
        Object on = parse.invoke(null, (Object) new String[] {"sts1.combat.intents", "ON"});
        assertTrue(on != null);
        assertEquals("sts1.combat.intents", family(on));
        assertEquals(Boolean.TRUE, enabled(on));
        Object off = parse.invoke(null, (Object) new String[] {" sts1.combat.hand ", "off"});
        assertTrue(off != null);
        assertEquals("sts1.combat.hand", family(off));
        assertEquals(Boolean.FALSE, enabled(off));

        assertNull(parse.invoke(null, (Object) new String[] {"sts1.combat.intents"}));
        assertNull(parse.invoke(null, (Object) new String[] {"", "on"}));
        assertNull(parse.invoke(null, (Object) new String[] {"--", "on"}));
        assertNull(parse.invoke(null, (Object) new String[] {"sts1.combat.intents", "maybe"}));
        assertNull(parse.invoke(null, (Object) null));
    }

    private static String family(Object request) throws Exception {
        java.lang.reflect.Field field = request.getClass().getDeclaredField("family");
        field.setAccessible(true);
        return (String) field.get(request);
    }

    private static Boolean enabled(Object request) throws Exception {
        java.lang.reflect.Field field = request.getClass().getDeclaredField("enabled");
        field.setAccessible(true);
        return Boolean.valueOf(field.getBoolean(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nativeFilterControlRoundTripsAndSurfacesInProbe() {
        Sts1VerifyDiagnostics.enableNativeFilter("sts1.combat.intents");
        assertTrue(Sts1VerifyDiagnostics.isNativeFilterEnabled("sts1.combat.intents"));
        assertFalse(Sts1VerifyDiagnostics.isNativeFilterEnabled("sts1.combat.hand"));

        Map<String, Object> filters =
                (Map<String, Object>) Sts1VerifyDiagnostics.probeSlice().get("nativeFilters");
        assertTrue(filters != null);
        assertEquals(Boolean.TRUE, filters.get("active"));
        assertTrue(((java.util.List<String>) filters.get("filteredFamilies"))
                .contains("sts1.combat.intents"));

        Sts1VerifyDiagnostics.disableNativeFilter("sts1.combat.intents");
        assertFalse(Sts1VerifyDiagnostics.isNativeFilterEnabled("sts1.combat.intents"));
        assertEquals(Boolean.FALSE,
                ((Map<String, Object>) Sts1VerifyDiagnostics.probeSlice().get("nativeFilters"))
                        .get("active"));

        Sts1VerifyDiagnostics.enableNativeFilter("sts1.combat.intents");
        Sts1VerifyDiagnostics.enableNativeFilter("sts1.combat.hand");
        Sts1VerifyDiagnostics.clearNativeFilters();
        Map<String, Object> cleared =
                (Map<String, Object>) Sts1VerifyDiagnostics.probeSlice().get("nativeFilters");
        assertEquals(Boolean.FALSE, cleared.get("active"));
        assertTrue(((java.util.List<String>) cleared.get("filteredFamilies")).isEmpty());
    }

    @Test
    public void blankNativeFilterFamilyIsIgnored() {
        Sts1VerifyDiagnostics.enableNativeFilter("   ");
        Sts1VerifyDiagnostics.enableNativeFilter(null);
        Map<String, Object> filters = Sts1VerifyDiagnostics.probeSlice();
        assertTrue(filters.containsKey("nativeFilters"));
        Map<?, ?> nativeFilters = (Map<?, ?>) filters.get("nativeFilters");
        assertEquals(Boolean.FALSE, nativeFilters.get("active"));
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
        assertEquals("ready", verify.get("submissionStatus"));
        assertEquals(Boolean.TRUE, verify.get("modeSupported"));
        assertTrue(verify.containsKey("nativeFilters"));
    }
}
