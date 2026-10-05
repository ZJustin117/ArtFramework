package artframework.sts1.render;

import artframework.render.RenderPlan;
import artframework.vfx.VfxDrawList;
import artframework.vfx.VfxParticleDraw;
import artframework.vfx.VfxRenderFrame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Sts1VfxOverlayRendererTest {
    @After public void cleanupMaterialSupport() {
        artframework.vfx.VfxMaterialSupport.resetForTests();
    }
    @Test public void overlaySubmissionSortsBySharedRenderOrder() {
        VfxParticleDraw high = draw("node-b", 2, 1, 5f);
        VfxParticleDraw lowKey = draw("node-a", 1, 0, 5f);
        VfxParticleDraw lowZ = draw("node-c", 3, 0, 1f);

        List<VfxParticleDraw> ordered = Sts1VfxOverlayRenderer.orderedDraws(
                new VfxDrawList(Arrays.asList(high, lowKey, lowZ)));

        assertEquals(lowZ, ordered.get(0));
        assertEquals(lowKey, ordered.get(1));
        assertEquals(high, ordered.get(2));
    }

    @Test public void payloadPathMatchesLegacyDrawPathFieldForField() {
        // A sheet that does not divide the texture evenly exercises the integer-division source
        // rectangle the legacy path used, which differs from the normalized UV payload rectangle.
        int textureWidth = 101;
        int textureHeight = 67;
        for (VfxParticleDraw draw : sampleDraws()) {
            Sts1VfxOverlayRenderer.DrawParams legacy =
                    Sts1VfxOverlayRenderer.params(draw, textureWidth, textureHeight);
            RenderPlan.Entry entry = VfxRenderFrame.payloadEntry(draw);
            Sts1VfxOverlayRenderer.DrawParams payload =
                    Sts1VfxOverlayRenderer.params(entry.payload, textureWidth, textureHeight);

            assertEquals("x", legacy.x, payload.x, 0f);
            assertEquals("y", legacy.y, payload.y, 0f);
            assertEquals("originX", legacy.originX, payload.originX, 0f);
            assertEquals("originY", legacy.originY, payload.originY, 0f);
            assertEquals("width", legacy.width, payload.width, 0f);
            assertEquals("height", legacy.height, payload.height, 0f);
            assertEquals("scaleX", legacy.scaleX, payload.scaleX, 0f);
            assertEquals("scaleY", legacy.scaleY, payload.scaleY, 0f);
            assertEquals("rotation", legacy.rotation, payload.rotation, 0f);
            assertEquals("sourceX", legacy.sourceX, payload.sourceX);
            assertEquals("sourceY", legacy.sourceY, payload.sourceY);
            assertEquals("sourceWidth", legacy.sourceWidth, payload.sourceWidth);
            assertEquals("sourceHeight", legacy.sourceHeight, payload.sourceHeight);
            assertEquals("red", legacy.r, payload.r, 0f);
            assertEquals("green", legacy.g, payload.g, 0f);
            assertEquals("blue", legacy.b, payload.b, 0f);
            assertEquals("alpha", legacy.a, payload.a, 0f);
            assertEquals("blend", legacy.blendMode, payload.blendMode);
            assertEquals("flipX", legacy.flipX, payload.flipX);
            assertEquals("flipY", legacy.flipY, payload.flipY);
        }
    }

    @Test public void payloadFlipbookFrameMatchesLegacySourceRect() {
        // Frame 5 on a 3x2 sheet -> column 2, row 1 with integer division on the texture sheet.
        VfxParticleDraw source = draw("node", 0, 0, 0f, 5, 3, 2, 10f, 20f, "MIX");
        Sts1VfxOverlayRenderer.DrawParams legacy =
                Sts1VfxOverlayRenderer.params(source, 96, 64);
        RenderPlan.Entry entry = VfxRenderFrame.payloadEntry(source);
        Sts1VfxOverlayRenderer.DrawParams payload =
                Sts1VfxOverlayRenderer.params(entry.payload, 96, 64);

        assertEquals(32, legacy.sourceWidth);
        assertEquals(32, legacy.sourceHeight);
        assertEquals(64, legacy.sourceX);
        assertEquals(32, legacy.sourceY);
        assertEquals(legacy.sourceX, payload.sourceX);
        assertEquals(legacy.sourceY, payload.sourceY);
        assertEquals("x = centre - tw/2", legacy.x, payload.x, 0f);
    }

    @Test public void overlaySourceRectPinsFirstAndLastCellsAndAgreesWithPayload() {
        // 4x2 sheet on a 96x64 texture -> 24x32 cells.
        VfxParticleDraw first = draw("node", 0, 0, 0f, 0, 4, 2, 48f, 32f, "MIX");
        Sts1VfxOverlayRenderer.DrawParams firstParams = Sts1VfxOverlayRenderer.params(first, 96, 64);
        assertEquals(0, firstParams.sourceX);
        assertEquals(0, firstParams.sourceY);
        assertEquals(24, firstParams.sourceWidth);
        assertEquals(32, firstParams.sourceHeight);

        // Last valid frame 7 -> column 3, row 1; the rect stays inside the texture.
        VfxParticleDraw last = draw("node", 0, 0, 0f, 7, 4, 2, 48f, 32f, "MIX");
        Sts1VfxOverlayRenderer.DrawParams lastParams = Sts1VfxOverlayRenderer.params(last, 96, 64);
        assertEquals(72, lastParams.sourceX);
        assertEquals(32, lastParams.sourceY);
        assertEquals(24, lastParams.sourceWidth);
        assertEquals(32, lastParams.sourceHeight);
        assertTrue(lastParams.sourceX + lastParams.sourceWidth <= 96);
        assertTrue(lastParams.sourceY + lastParams.sourceHeight <= 64);

        // The projection payload's normalized rect maps to the same integer cell (sheet divides
        // evenly), so the payload authority and the legacy draw path agree on the same frame.
        RenderPlan.Entry entry = VfxRenderFrame.payloadEntry(last);
        assertEquals(0.75f, entry.payload.sourceX(), 0f);
        assertEquals(0.5f, entry.payload.sourceY(), 0f);
        assertEquals(0.25f, entry.payload.sourceWidth(), 0f);
        assertEquals(0.5f, entry.payload.sourceHeight(), 0f);
        Sts1VfxOverlayRenderer.DrawParams payload =
                Sts1VfxOverlayRenderer.params(entry.payload, 96, 64);
        assertEquals(lastParams.sourceX, payload.sourceX);
        assertEquals(lastParams.sourceY, payload.sourceY);
        assertEquals((int) (entry.payload.sourceX() * 96), lastParams.sourceX);
        assertEquals((int) (entry.payload.sourceY() * 64), lastParams.sourceY);
    }

    @Test public void overlaySourceRectStaysInsideTextureAcrossEveryFlipbookFrame() {
        // Every in-range frame maps to a non-negative integer rect fully inside the texture.
        for (int frame = 0; frame < 8; frame++) {
            VfxParticleDraw source = draw("node", 0, 0, 0f, frame, 4, 2, 0f, 0f, "MIX");
            Sts1VfxOverlayRenderer.DrawParams params =
                    Sts1VfxOverlayRenderer.params(source, 96, 64);
            assertTrue(params.sourceX >= 0 && params.sourceY >= 0);
            assertTrue(params.sourceX + params.sourceWidth <= 96);
            assertTrue(params.sourceY + params.sourceHeight <= 64);
        }
    }

    @Test public void blendFunctionMappingIsStable() {
        assertBlend("ADD", com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE);
        assertBlend("PREMULT_ALPHA", com.badlogic.gdx.graphics.GL20.GL_ONE,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        assertBlend("MUL", com.badlogic.gdx.graphics.GL20.GL_DST_COLOR,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        assertBlend("MIX", com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        assertBlend(null, com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Test public void supportedBlendNamesProduceUnchangedFunctionsAndCountNoRejections() {
        // The support matrix must not change the rendered blend functions for any supported name,
        // including a case variant that canonicalizes to the same name.
        assertBlend("MIX", com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        assertBlend("add", com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE);
        assertBlend("mul", com.badlogic.gdx.graphics.GL20.GL_DST_COLOR,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        assertBlend("premult_alpha", com.badlogic.gdx.graphics.GL20.GL_ONE,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        @SuppressWarnings("unchecked")
        Map<String, Integer> rejected = (Map<String, Integer>)
                artframework.vfx.VfxMaterialSupport.probeSlice().get("rejected");
        assertTrue("supported names must not be counted", rejected.isEmpty());
    }

    @Test public void unsupportedAndUnknownBlendNamesFallOpenToMixAndAreCounted() {
        int[] mix = new int[] { com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA };
        // SUB has no renderer mapping today: it must fall open to MIX, not throw.
        assertBlend("SUB", mix[0], mix[1]);
        assertBlend("UNKNOWN", mix[0], mix[1]);
        assertBlend("", mix[0], mix[1]);

        @SuppressWarnings("unchecked")
        Map<String, Integer> rejected = (Map<String, Integer>)
                artframework.vfx.VfxMaterialSupport.probeSlice().get("rejected");
        assertEquals(Integer.valueOf(1), rejected.get("SUB"));
        assertEquals(Integer.valueOf(1), rejected.get("(blank)"));
        assertEquals(Integer.valueOf(1), rejected.get("UNKNOWN"));
    }

    @Test public void payloadBlendRegressionForSupportedModesIsPixelIdentical() {
        // A payload-bearing entry for each supported mode maps to the same functions as before and
        // never falls back; the projection payload still carries the raw emitter name.
        for (String mode : new String[] { "MIX", "ADD", "MUL", "PREMULT_ALPHA" }) {
            VfxParticleDraw source = draw("node", 0, 0, 0f, 0, 1, 1, 3f, 4f, mode);
            RenderPlan.Entry entry = VfxRenderFrame.payloadEntry(source);
            int[] blend = Sts1VfxOverlayRenderer.blendFunctions(entry.payload.blendMode());

            artframework.vfx.VfxMaterialSupport.Resolution resolution =
                    artframework.vfx.VfxMaterialSupport.resolve(mode);
            assertTrue(mode + " must be supported", resolution.supported);
            assertFalse(mode + " must not fall back", resolution.fallback);
            assertEquals("raw name reaches the payload unchanged", mode, entry.payload.blendMode());
            assertEquals("blend functions must not change for " + mode,
                    blendRef(mode)[0], blend[0]);
            assertEquals("blend functions must not change for " + mode,
                    blendRef(mode)[1], blend[1]);
        }
    }

    private static int[] blendRef(String mode) {
        switch (mode) {
            case "ADD":
                return new int[] { com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                        com.badlogic.gdx.graphics.GL20.GL_ONE };
            case "PREMULT_ALPHA":
                return new int[] { com.badlogic.gdx.graphics.GL20.GL_ONE,
                        com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA };
            case "MUL":
                return new int[] { com.badlogic.gdx.graphics.GL20.GL_DST_COLOR,
                        com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA };
            default:
                return new int[] { com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                        com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA };
        }
    }

    @Test public void payloadlessEntriesAreSkippedByThePayloadPath() {
        RenderPlan.Entry identityOnly = RenderPlan.Entry.payloadEntry("native:x",
                artframework.render.RenderTargetKind.SYNTHETIC_WIDGET,
                new artframework.component.Rect(0f, 0f, 1f, 1f),
                artframework.render.RenderPhase.NATIVE_RETAINED, 0f, "x", true, null);
        assertNull(identityOnly.payload);
        // The batch is null so the payload path returns before touching GL: it must not throw and
        // must tolerate identity-only entries.
        Sts1VfxOverlayRenderer.render(null, Arrays.asList(identityOnly), new Sts1VfxOverlayRenderer.TextureResolver() {
            @Override public com.badlogic.gdx.graphics.Texture resolve(String path) { return null; }
        });
    }

    private static void assertBlend(String mode, int src, int dst) {
        int[] functions = Sts1VfxOverlayRenderer.blendFunctions(mode);
        assertEquals(src, functions[0]);
        assertEquals(dst, functions[1]);
    }

    private static List<VfxParticleDraw> sampleDraws() {
        List<VfxParticleDraw> draws = new ArrayList<VfxParticleDraw>();
        draws.add(draw("a", 0, 0, 0f, 0, 1, 1, 100f, 50f, "MIX"));
        draws.add(draw("b", 1, 1, 3f, 5, 3, 2, 12.5f, -7.25f, "ADD"));
        draws.add(draw("c", 2, 2, -2f, 3, 2, 4, 0f, 0f, "PREMULT_ALPHA"));
        draws.add(draw("d", 3, 3, 9f, 7, 4, 4, -33f, 41.5f, "MUL"));
        return draws;
    }

    private static VfxParticleDraw draw(String node, int particle, int definition, float z) {
        return draw(node, particle, definition, z, 0, 1, 1, 0f, 0f, "MIX");
    }

    private static VfxParticleDraw draw(String node, int particle, int definition, float z,
            int frame, int columns, int rows, float x, float y, String blend) {
        return new VfxParticleDraw("scene", node, particle, definition, "texture.png",
                x, y, 33f, 2f, 3f, 1f, 0.25f, 0.5f, 0.75f, blend, z,
                frame, columns, rows, true, false);
    }
}
