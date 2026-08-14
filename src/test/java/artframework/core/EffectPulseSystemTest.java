package artframework.core;

import artframework.api.ArtFramework;
import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EffectPulseSystemTest {
    @After public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test public void completesEcsPulseThroughOrderedPipeline() {
        EffectPulse.pulse("window", "panel", "lightwave", 0.25f);
        assertTrue(EffectPulse.isActive("window", "panel"));

        EcsPipeline.run(ArtEcs.world(), new EcsTick(0.25f, 4L),
                Collections.<EcsSystem>singletonList(new EffectPulseSystem()));

        assertFalse(EffectPulse.isActive("window", "panel"));
    }
}
