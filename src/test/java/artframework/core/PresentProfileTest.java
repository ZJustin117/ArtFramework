package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.test.C1RuntimeFixture;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PresentProfileTest {
    @Test public void mountUsesDeclaredPresentProfile() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("profile", UiNode.of(UiTypes.WINDOW).id("root")
                .prop("present_profile", PresentProfiles.LIGHTWAVE).build());
        try { assertEquals(PresentProfiles.LIGHTWAVE,
                PresentResolve.forEntity(fixture.context, fixture.root).profileId); }
        finally { fixture.close(); }
    }

    @Test public void mountUsesDeclaredThemeName() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("theme", UiNode.of(UiTypes.WINDOW).id("root")
                .prop("theme", "lightwave").build());
        try { assertEquals("lightwave",
                PresentResolve.forEntity(fixture.context, fixture.root).theme.name()); }
        finally { fixture.close(); }
    }
}
