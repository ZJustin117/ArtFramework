import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.UiProbe;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.assets.AssetDomain;
import artframework.assets.AssetPack;
import artframework.assets.AssetResolveResult;
import artframework.assets.HostAssets;
import artframework.assets.ResourceIds;
import artframework.c2.EntityPresent;
import artframework.c2.EntitySnapshot;
import artframework.component.NativeTemplateIds;
import artframework.context.CardRef;
import artframework.context.CardView;
import artframework.context.CardZone;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.MapView;
import artframework.context.PresentProjection;
import artframework.context.RewardItemView;
import artframework.context.RewardView;
import artframework.context.SurfaceIds;
import artframework.core.HostCapabilities;
import artframework.sts1.PresentLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        useFramesAndPresent();
        useEntityPresent();
        useProbe();
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

    /** Milestone 23.3: frames / projection / present surface ids / CardRef. */
    public static void useFramesAndPresent() {
        PresentLevel level = PresentLevel.FULL;
        if (level == null) {
            throw new IllegalStateException("PresentLevel missing");
        }
        List<CardView> cards = new ArrayList<CardView>();
        CardRef ref = new CardRef("inst-1", "Strike_R");
        cards.add(
                new CardView(
                        ref,
                        CardZone.HAND,
                        0,
                        null,
                        true,
                        false,
                        false,
                        false,
                        ResourceIds.cardArt("Strike_R"),
                        ResourceIds.cardFrame("red")));
        ContextFrame frame =
                ContextFrame.of(
                        1L,
                        1L,
                        "combat",
                        cards,
                        ControlsView.combat(3, 1, 10, 0, 0, true, true),
                        MapView.empty(),
                        null);
        ArtFramework.publishFrame(frame);
        PresentProjection proj = ArtFramework.projection();
        if (proj == null || !proj.isAvailable()) {
            throw new IllegalStateException("projection unavailable");
        }
        if (ArtFramework.component(SurfaceIds.COMBAT_HAND) == null) {
            throw new IllegalStateException("hand surface missing");
        }
        if (ArtFramework.component(SurfaceIds.REWARD_COMBAT) == null) {
            throw new IllegalStateException("reward surface missing");
        }
        UiOpResult play = ArtFramework.ops().playHandCardRef(ref, null);
        if (play == null) {
            throw new IllegalStateException("playHandCardRef null");
        }
        RewardView.empty();
    }

    public static void useEntityPresent() {
        EntityPresent entities = ArtFramework.entities();
        entities.attach("co-op-p1", "player", "ironclad");
        entities.sync("co-op-p1", EntitySnapshot.playerChrome("Ironclad", 70, 80, 5));
        entities.layout("co-op-p1", 100f, 200f, 1f);
        if (entities.size() != 1) {
            throw new IllegalStateException("entity slot missing");
        }
    }

    public static void useProbe() {
        UiProbe probe = ArtFramework.probe();
        Map<String, Object> map = probe.asMap();
        if (map == null || !Integer.valueOf(UiProbe.SCHEMA_VERSION).equals(map.get("schemaVersion"))) {
            throw new IllegalStateException("probe schema");
        }
        if (map.get("backend") == null || map.get("entities") == null) {
            throw new IllegalStateException("probe missing backend/entities");
        }
    }
}
