package artframework.core;

import artframework.component.UiNode;
import artframework.ecs.EntityId;
import artframework.test.C1RuntimeFixture;
import artframework.skeleton.SkeletonNodeBinding;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SkeletonNodeBindingTest {
    @Test public void skeletonNodeStoresEntityReferenceInPresentationEcs() {
        UiNode node = UiNode.of("art.skeleton").id("hero")
                .prop("entity", "combat:hero").prop("anchor", "root")
                .prop("offset_x", 4).prop("local_scale", 0.5f).build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("skeleton_test", node);
        EntityId id = fixture.root;
        SkeletonNodeBindingComponent component = fixture.context.world().get(id, SkeletonNodeBindingComponent.class);
        assertNotNull(component);
        SkeletonNodeBinding binding = component.binding;
        assertEquals("combat:hero", binding.entityKey);
        assertEquals("root", binding.anchorBone);
        assertEquals(4f, binding.offsetX, 0.001f);
        assertEquals(0.5f, binding.localScale, 0.001f);
        fixture.close();
    }
}
