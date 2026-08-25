package artframework.sts1.render;

import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Sts1NativePresentationAdapterTest {
    @After
    public void tearDown() {
        Sts1NativePresentationAdapter.clear();
        PresentationRegistry.resetForTests();
    }

    private NativeRenderInvocation invocation(String owner, long frame, Rect bounds) {
        return new NativeRenderInvocation(1L, frame, "combat", owner, "Native", "render",
                "surface", "source", bounds);
    }

    @Test
    public void presentCreatesStableEntityWithHostNeutralComponents() {
        String first = Sts1NativePresentationAdapter.present(
                invocation("sts1.combat.hand", 3L, new Rect(1f, 2f, 30f, 40f)));
        String second = Sts1NativePresentationAdapter.present(
                invocation("sts1.combat.hand", 4L, new Rect(5f, 6f, 50f, 60f)));

        assertEquals(first, second);
        EntityId entity = Sts1NativePresentationAdapter.entity("sts1.combat.hand");
        assertNotNull(entity);
        BoundsComponent bounds = PresentationRegistry.world().get(entity, BoundsComponent.class);
        DrawComponent draw = PresentationRegistry.world().get(entity, DrawComponent.class);
        VisibilityComponent visibility = PresentationRegistry.world().get(entity, VisibilityComponent.class);
        assertEquals(new Rect(5f, 6f, 50f, 60f), bounds.rect);
        assertEquals("render", draw.role);
        assertTrue(visibility.visible);
    }

    @Test
    public void removeAndClearReleaseNativeEntities() {
        Sts1NativePresentationAdapter.present(invocation("owner.a", 1L, Rect.ZERO));
        Sts1NativePresentationAdapter.present(invocation("owner.b", 1L, Rect.ZERO));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("owner.a"));

        Sts1NativePresentationAdapter.remove("owner.a");
        assertFalse(Sts1NativePresentationAdapter.hasEntity("owner.a"));
        assertTrue(Sts1NativePresentationAdapter.hasEntity("owner.b"));

        Sts1NativePresentationAdapter.clear();
        assertFalse(Sts1NativePresentationAdapter.hasEntity("owner.b"));
    }
}
