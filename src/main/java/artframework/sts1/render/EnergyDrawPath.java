package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.assets.ResourceIds;
import artframework.component.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Energy orb present from ControlsView.energy (25.2). */
public final class EnergyDrawPath {

    /** Native orb geometry: 128x128 layer textures drawn at scale 1.15 around (198, 190). */
    public static final float ORB_IMAGE_SIZE = 128f;
    public static final float ORB_IMAGE_SCALE = 1.15f;
    public static final float ORB_CENTER_X = 198f;
    public static final float ORB_CENTER_Y = 190f;

    private static final float[] RED_GREEN_SPEEDS = {72f, 8f, -8f, 5f, -5f, 0f};
    private static final float[] BLUE_SPEEDS = {0f, 8f, -8f, 5f, -5f};
    private static final float[] PURPLE_SPEEDS = {0f, 8f, -8f, 5f};

    private static float animationSeconds = 0f;

    /** One native orb layer texture and its current spin. */
    public static final class OrbLayer {
        public final String resourceId;
        public final float rotationDegrees;

        public OrbLayer(String resourceId, float rotationDegrees) {
            this.resourceId = resourceId != null ? resourceId : "";
            this.rotationDegrees = rotationDegrees;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("resourceId", resourceId);
            m.put("rotation", Float.valueOf(rotationDegrees));
            return m;
        }
    }

    public static final class DrawItem {
        public final String id;
        public final String label;
        public final int energy;
        public final String resourceId;
        public final Rect bounds;
        public final List<OrbLayer> layers;

        public DrawItem(String id, String label, int energy) {
            this(id, label, energy, ResourceIds.energyOrbLayer("red", 1), null,
                    Collections.<OrbLayer>emptyList());
        }

        public DrawItem(String id, String label, int energy, String resourceId, Rect bounds) {
            this(id, label, energy, resourceId, bounds, Collections.<OrbLayer>emptyList());
        }

        public DrawItem(
                String id, String label, int energy, String resourceId, Rect bounds,
                List<OrbLayer> layers) {
            this.id = id;
            this.label = label;
            this.energy = energy;
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds;
            this.layers = layers != null ? layers : Collections.<OrbLayer>emptyList();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("label", label);
            m.put("energy", Integer.valueOf(energy));
            m.put("resourceId", resourceId);
            m.put("materializer", artframework.sts1.assets.Sts1AssetMaterializer.energyAttributionProbe());
            if (bounds != null) {
                m.put("x", Float.valueOf(bounds.x));
                m.put("y", Float.valueOf(bounds.y));
                m.put("width", Float.valueOf(bounds.width));
                m.put("height", Float.valueOf(bounds.height));
            }
            List<Map<String, Object>> layerList = new ArrayList<Map<String, Object>>();
            for (OrbLayer layer : layers) {
                layerList.add(layer.toMap());
            }
            m.put("layers", layerList);
            return m;
        }
    }

    private EnergyDrawPath() {}

    public static boolean shouldSuppressNativeEnergy() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_ENERGY);
    }

    /** Native 128x128 scaled orb bounds centered on the native orb center. */
    public static Rect orbBounds(float xScale, float yScale, float scale) {
        float size = ORB_IMAGE_SIZE * ORB_IMAGE_SCALE * (scale > 0f ? scale : 1f);
        float cx = ORB_CENTER_X * (xScale > 0f ? xScale : 1f);
        float cy = ORB_CENTER_Y * (yScale > 0f ? yScale : 1f);
        return new Rect(cx - size / 2f, cy - size / 2f, size, size);
    }

    /** Player class -> orb color. Unknown or absent player falls back to red. */
    public static String orbColor() {
        try {
            com.megacrit.cardcrawl.characters.AbstractPlayer p =
                    com.megacrit.cardcrawl.dungeons.AbstractDungeon.player;
            if (p == null || p.chosenClass == null) {
                return "red";
            }
            String name = p.chosenClass.name();
            if ("THE_SILENT".equals(name)) {
                return "green";
            }
            if ("DEFECT".equals(name)) {
                return "blue";
            }
            if ("WATCHER".equals(name)) {
                return "purple";
            }
            return "red";
        } catch (Throwable ignored) {
            return "red";
        }
    }

    /**
     * Layer stack mirroring native orb rendering. Lit orbs use the bright set; red/green/blue
     * appended dim files replace layers 1..N when out of energy. Rotations follow the native
     * per-layer spin rates, frozen at zero for non-finite or negative-less accumulation.
     */
    public static List<OrbLayer> orbLayers(String color, boolean lit, float seconds) {
        List<OrbLayer> out = new ArrayList<OrbLayer>();
        float t = Float.isFinite(seconds) ? seconds : 0f;
        String c = color != null ? color : "red";
        if (!"red".equals(c) && !"green".equals(c) && !"blue".equals(c) && !"purple".equals(c)) {
            c = "red";
        }
        if ("blue".equals(c)) {
            for (int i = 1; i <= 5; i++) {
                out.add(new OrbLayer(
                        lit ? ResourceIds.energyOrbLayer("blue", i)
                                : ResourceIds.energyOrbDimLayer("blue", i),
                        t * BLUE_SPEEDS[i - 1]));
            }
            return out;
        }
        if ("purple".equals(c)) {
            for (int i = 1; i <= 4; i++) {
                out.add(new OrbLayer(
                        ResourceIds.energyOrbLayer("purple", i), t * PURPLE_SPEEDS[i - 1]));
            }
            return out;
        }
        for (int i = 1; i <= 6; i++) {
            String id = !lit && i <= 5
                    ? ResourceIds.energyOrbDimLayer(c, i)
                    : ResourceIds.energyOrbLayer(c, i);
            out.add(new OrbLayer(id, t * RED_GREEN_SPEEDS[i - 1]));
        }
        return out;
    }

    /** Accumulates positive, finite frame deltas for orb spin. */
    public static void advanceAnimation(float deltaSeconds) {
        if (Float.isFinite(deltaSeconds) && deltaSeconds > 0f) {
            animationSeconds += deltaSeconds;
        }
    }

    public static void resetAnimationForTests() {
        animationSeconds = 0f;
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        int energy = ArtFramework.projection().controls().energy;
        String color = orbColor();
        List<OrbLayer> layers = orbLayers(color, energy > 0, animationSeconds);
        String base = layers.isEmpty() ? "" : layers.get(0).resourceId;
        out.add(new DrawItem("energy_orb", String.valueOf(energy), energy, base,
                orbBounds(com.megacrit.cardcrawl.core.Settings.xScale,
                        com.megacrit.cardcrawl.core.Settings.yScale,
                        com.megacrit.cardcrawl.core.Settings.scale),
                layers));
        return out;
    }

    public static Map<String, Object> probeSlice() {
        List<DrawItem> items = buildFromProjection();
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        int energy = ArtFramework.projection().controls().energy;
        m.put("count", Integer.valueOf(items.size()));
        m.put("energy", Integer.valueOf(energy));
        m.put("label", String.valueOf(energy));
        m.put("suppressNativeEnergy", Boolean.valueOf(shouldSuppressNativeEnergy()));
        m.put("presentLevel", FullPresentMode.energyLevel().name());
        artframework.sts1.FullPresentCapability cap =
                artframework.sts1.input.CombatInputRouter.capability(SurfaceIds.COMBAT_ENERGY);
        m.put("capability", cap.state.name());
        m.put("capabilityReason", cap.reason);
        m.put("drawCount", Integer.valueOf(items.size()));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DrawItem d : items) {
            list.add(d.toMap());
        }
        m.put("items", list);
        m.put("materializerAttribution",
                artframework.sts1.assets.Sts1AssetMaterializer.energyAttributionProbe());
        return m;
    }
}
