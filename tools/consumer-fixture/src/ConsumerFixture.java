import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.c2.NativeTemplateIds;
import artframework.core.HostCapabilities;

/** Compile-only consumer fixture for the documented ART public API. */
public final class ConsumerFixture {

    private ConsumerFixture() {}

    public static void useArtFramework() {
        ArtFramework.register(
                new WindowDef(NativeTemplateIds.MAP, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.MAP));
        UiOpResult result = ArtFramework.ops().invoke(NativeTemplateIds.MAP, "click_node");
        HostCapabilities capabilities = ArtFramework.host().capabilities();
        if (result == null || capabilities == null) {
            throw new IllegalStateException("ART API unavailable");
        }
    }
}
