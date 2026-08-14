package artframework.core;

import artframework.api.ArtFramework;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HostBackendTickSystemTest {
    @After public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test public void advancesConfiguredBackendThroughOrderedPipeline() {
        final float[] observed = new float[1];
        ArtFramework.setHostBackend(new HostBackend() {
            @Override public boolean isReady() { return true; }
            @Override public void attach(artframework.presentation.PresentationMount mount) {}
            @Override public void detach(artframework.presentation.PresentationMount mount) {}
            @Override public void applyLayout(artframework.presentation.PresentationMount mount) {}
            @Override public void tick(float deltaSeconds) { observed[0] = deltaSeconds; }
        });

        EcsPipeline.run(ArtEcs.world(), new EcsTick(0.25f, 6L),
                Collections.<EcsSystem>singletonList(new HostBackendTickSystem()));

        assertEquals(0.25f, observed[0], 0.0001f);
    }
}
