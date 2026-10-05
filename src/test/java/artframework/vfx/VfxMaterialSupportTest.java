package artframework.vfx;

import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Focused no-GL tests for the restricted VFX material/blend support matrix. */
public class VfxMaterialSupportTest {
    @After public void cleanup() {
        VfxMaterialSupport.resetForTests();
    }

    @Test public void supportedNamesResolveCanonicalWithoutFallback() {
        assertResolution("MIX", "MIX", true, false);
        assertResolution("ADD", "ADD", true, false);
        assertResolution("MUL", "MUL", true, false);
        assertResolution("PREMULT_ALPHA", "PREMULT_ALPHA", true, false);
    }

    @Test public void supportedNamesCanonicalizeArbitraryCaseAndWhitespace() {
        assertResolution("add", "ADD", true, false);
        assertResolution("Premult_Alpha", "PREMULT_ALPHA", true, false);
        assertResolution("  mul  ", "MUL", true, false);
    }

    @Test public void knownUnsupportedSubFallsOpenToMix() {
        assertResolution("SUB", "MIX", false, true);
        assertResolution("sub", "MIX", false, true);
    }

    @Test public void unknownBlankAndNullFallOpenToMixAndNeverThrow() {
        assertResolution("UNKNOWN", "MIX", false, true);
        assertResolution("somethingElse", "MIX", false, true);
        assertResolution("", "MIX", false, true);
        assertResolution("   ", "MIX", false, true);
        assertResolution(null, "MIX", false, true);
        // Calling resolve must never throw regardless of the garbage shape.
        VfxMaterialSupport.resolve("\u0000weird/name\t");
    }

    @Test public void supportedAndKnownPredicates() {
        assertTrue(VfxMaterialSupport.isSupported("mix"));
        assertTrue(VfxMaterialSupport.isSupported("PREMULT_ALPHA"));
        assertFalse(VfxMaterialSupport.isSupported("SUB"));
        assertFalse(VfxMaterialSupport.isSupported(null));
        assertFalse(VfxMaterialSupport.isSupported("nope"));

        assertTrue(VfxMaterialSupport.isKnown("SUB"));
        assertTrue(VfxMaterialSupport.isKnown("mix"));
        assertFalse(VfxMaterialSupport.isKnown("nope"));
        assertFalse(VfxMaterialSupport.isKnown(null));

        assertEquals("ADD", VfxMaterialSupport.resolvedName("add"));
        assertEquals("MIX", VfxMaterialSupport.resolvedName("SUB"));
        assertEquals("MIX", VfxMaterialSupport.resolvedName(null));
    }

    @Test public void rejectionDiagnosticsIncrementWithoutDuplicateKeys() {
        VfxMaterialSupport.resolve("BOGUS");
        VfxMaterialSupport.resolve("BOGUS");
        VfxMaterialSupport.resolve("bogus"); // canonicalized to the same key
        VfxMaterialSupport.resolve("SUB");
        VfxMaterialSupport.resolve(null);

        Map<String, Object> slice = VfxMaterialSupport.probeSlice();
        @SuppressWarnings("unchecked")
        Map<String, Integer> rejected = (Map<String, Integer>) slice.get("rejected");
        assertEquals(Integer.valueOf(3), rejected.get("BOGUS"));
        assertEquals(Integer.valueOf(1), rejected.get("SUB"));
        assertEquals(Integer.valueOf(1), rejected.get("(null)"));
        assertEquals(0, slice.get("rejectedOverflow"));
        // Supported names are never counted as rejections.
        VfxMaterialSupport.resolve("MIX");
        assertFalse(((Map<?, ?>) VfxMaterialSupport.probeSlice().get("rejected")).containsKey("MIX"));
    }

    @Test public void rejectionDiagnosticsCapAndOverflow() {
        for (int index = 0; index < VfxMaterialSupport.MAX_REJECTED_KEYS; index++) {
            VfxMaterialSupport.resolve("unknown-" + index);
        }
        // Repeating an existing key increments rather than overflowing.
        VfxMaterialSupport.resolve("unknown-0");
        // Distinct keys beyond the cap go to the overflow counter, not the key map.
        VfxMaterialSupport.resolve("overflow-a");
        VfxMaterialSupport.resolve("overflow-b");
        VfxMaterialSupport.resolve("overflow-b");

        Map<String, Object> slice = VfxMaterialSupport.probeSlice();
        @SuppressWarnings("unchecked")
        Map<String, Integer> rejected = (Map<String, Integer>)
                artframework.vfx.VfxMaterialSupport.probeSlice().get("rejected");
        assertEquals(VfxMaterialSupport.MAX_REJECTED_KEYS, rejected.size());
        assertEquals(Integer.valueOf(2), rejected.get("UNKNOWN-0"));
        assertEquals(3, slice.get("rejectedOverflow"));
    }

    @Test public void probeSliceExposesDocumentedKeys() {
        Map<String, Object> slice = VfxMaterialSupport.probeSlice();
        assertEquals(VfxMaterialSupport.knownNames(), slice.get("known"));
        assertEquals(VfxMaterialSupport.supportedNames(), slice.get("supported"));
        assertEquals(VfxMaterialSupport.unsupportedNames(), slice.get("unsupported"));
        assertTrue(slice.containsKey("rejected"));
        assertTrue(slice.containsKey("rejectedOverflow"));

        @SuppressWarnings("unchecked")
        List<String> known = (List<String>) slice.get("known");
        assertEquals(java.util.Arrays.asList("MIX", "ADD", "SUB", "MUL", "PREMULT_ALPHA"), known);
        assertTrue(VfxMaterialSupport.supportedNames().containsAll(
                java.util.Arrays.asList("MIX", "ADD", "MUL", "PREMULT_ALPHA")));
        assertEquals(java.util.Collections.singletonList("SUB"), VfxMaterialSupport.unsupportedNames());
        assertEquals("MIX", VfxMaterialSupport.FALLBACK);
    }

    private static void assertResolution(String requested, String resolved,
            boolean supported, boolean fallback) {
        VfxMaterialSupport.Resolution result = VfxMaterialSupport.resolve(requested);
        assertEquals("requested", requested, result.requested);
        assertEquals("resolved", resolved, result.resolved);
        assertEquals("supported", supported, result.supported);
        assertEquals("fallback", fallback, result.fallback);
    }
}
