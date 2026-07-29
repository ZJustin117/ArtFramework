import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.assets.AssetDomain;
import artframework.assets.AssetPack;
import artframework.assets.AssetResolveResult;
import artframework.assets.HostAssets;
import artframework.assets.ResourceIds;
import artframework.c2.NativeTemplateIds;
import artframework.core.HostCapabilities;

import java.util.Collections;

/** Compile-only consumer fixture for the documented ART public API + HostAssets packs. */
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
        useHostAssetsPacks();
    }

    /** Milestone 19.6: pack register + resolve must remain linkable for consumers. */
    public static void useHostAssetsPacks() {
        HostAssets assets = ArtFramework.assets();
        assets.loadMinimalVanillaCatalog();
        AssetPack pack =
                AssetPack.builder("beauty")
                        .priority(10)
                        .domain(AssetDomain.CARD)
                        .entry(ResourceIds.cardArt("Strike_R"), "pack:beauty/Strike_R.png")
                        .build();
        assets.registerPack(pack);
        assets.enablePack("beauty", true);
        assets.setPackOrder(Collections.singletonList("beauty"));
        AssetResolveResult hit = assets.resolve(ResourceIds.cardArt("Strike_R"));
        if (hit == null || !hit.found || !"beauty".equals(hit.packId)) {
            throw new IllegalStateException("pack resolve failed: " + hit);
        }
    }
}
