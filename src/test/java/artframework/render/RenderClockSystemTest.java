package artframework.render;

import artframework.ecs.ArtEcs;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RenderClockSystemTest {
    @After public void tearDown() {
        RenderHosts.resetForTests();
    }

    @Test public void advancesHostClockThroughOrderedEcsPipeline() {
        EcsPipeline.run(ArtEcs.world(), new EcsTick(0.25f, 7L),
                Collections.<EcsSystem>singletonList(new RenderClockSystem()));

        assertEquals(0.25f, RenderHosts.get().timeSeconds(), 0.0001f);
    }

    @Test public void zeroDeltaLeavesHostClockUnchanged() {
        RenderHosts.get().tick(0.25f);

        EcsPipeline.run(ArtEcs.world(), new EcsTick(0f, 8L),
                Collections.<EcsSystem>singletonList(new RenderClockSystem()));

        assertEquals(0.25f, RenderHosts.get().timeSeconds(), 0.0001f);
    }
}
