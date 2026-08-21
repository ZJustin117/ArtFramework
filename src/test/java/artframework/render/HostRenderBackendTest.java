package artframework.render;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.presentation.EffectAttachment;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HostRenderBackendTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void defaultBackendIsDirect() {
        assertTrue(ArtFramework.render().hostBackend() instanceof DirectHostRenderBackend);
        assertTrue(ArtFramework.render().hostBackend().supportsCapture());
        assertTrue(ArtFramework.render().hostBackend().supportsShaders());
    }

    @Test
    public void canInstallNoOpBackend() {
        ArtFramework.render().setHostBackend(NoOpHostRenderBackend.INSTANCE);
        assertSame(NoOpHostRenderBackend.INSTANCE, ArtFramework.render().hostBackend());
        assertFalse(ArtFramework.render().hostBackend().supportsCapture());
    }

    @Test
    public void drawFrameUsesBackend() {
        final AtomicInteger draws = new AtomicInteger();
        HostRenderBackend counting =
                new HostRenderBackend() {
                    @Override
                    public boolean captureScreen(FrameCapture capture, int width, int height) {
                        return false;
                    }

                    @Override
                    public void drawEffect(
                            Effect effect,
                            RenderTarget target,
                            EffectBinding binding,
                            RenderContext context) {
                        draws.incrementAndGet();
                    }

                    @Override
                    public boolean supportsCapture() {
                        return false;
                    }

                    @Override
                    public boolean supportsShaders() {
                        return true;
                    }
                };
        RenderHost host = ArtFramework.render();
        host.setHostBackend(counting);
        host.ensureTarget("t", RenderTargetKind.OVERLAY);
        host.bindEffect("t", TintEffect.ID, Collections.<String, Object>emptyMap());
        host.drawFrame(null);
        assertEquals(1, draws.get());
    }

    @Test
    public void c2EffectBandDrawsOnlyActiveC2Targets() {
        final AtomicInteger draws = new AtomicInteger();
        RenderHost host = ArtFramework.render();
        host.setHostBackend(new HostRenderBackend() {
            @Override public boolean captureScreen(FrameCapture capture, int width, int height) {
                return false;
            }

            @Override public void drawEffect(
                    Effect effect, RenderTarget target, EffectBinding binding, RenderContext context) {
                draws.incrementAndGet();
            }

            @Override public boolean supportsCapture() { return false; }
            @Override public boolean supportsShaders() { return true; }
        });
        RenderStateEcs.surface("sts1.active", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surfaceEffects("sts1.active", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient", Collections.<String, Object>emptyMap())));
        RenderStateEcs.surface("sts1.inactive", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surfaceEffects("sts1.inactive", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient", Collections.<String, Object>emptyMap())));

        RenderProjectionQueue.projectActiveSurfaces(Collections.singleton("sts1.active"));
        host.drawFrame(null, true, RenderHost.kindsC2UnderPresent());

        assertEquals(1, draws.get());
    }

    @Test
    public void resetRestoresDirectBackend() {
        ArtFramework.render().setHostBackend(NoOpHostRenderBackend.INSTANCE);
        ArtFramework.resetForTests();
        assertTrue(ArtFramework.render().hostBackend() instanceof DirectHostRenderBackend);
    }

    @Test
    public void probeIncludesBackendCapabilities() {
        ArtFramework.render().setHostBackend(NoOpHostRenderBackend.INSTANCE);
        java.util.Map<String, Object> p = ArtFramework.render().probeMap();
        assertEquals(Boolean.FALSE, p.get("hostSupportsCapture"));
        assertEquals(Boolean.FALSE, p.get("hostSupportsShaders"));
    }
}
