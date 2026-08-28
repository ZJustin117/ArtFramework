package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.assets.ResourceIds;
import artframework.component.Rect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Energy orb present from ControlsView.energy (25.2). */
public final class EnergyDrawPath {

    public static final class DrawItem {
        public final String id;
        public final String label;
        public final int energy;
        public final String resourceId;
        public final Rect bounds;

        public DrawItem(String id, String label, int energy) {
            this(id, label, energy, ResourceIds.energyOrbLayer("red", 1), null);
        }

        public DrawItem(String id, String label, int energy, String resourceId, Rect bounds) {
            this.id = id;
            this.label = label;
            this.energy = energy;
            this.resourceId = resourceId != null ? resourceId : "";
            this.bounds = bounds;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("label", label);
            m.put("energy", Integer.valueOf(energy));
            m.put("resourceId", resourceId);
            if (bounds != null) {
                m.put("x", Float.valueOf(bounds.x));
                m.put("y", Float.valueOf(bounds.y));
                m.put("width", Float.valueOf(bounds.width));
                m.put("height", Float.valueOf(bounds.height));
            }
            return m;
        }
    }

    private EnergyDrawPath() {}

    public static boolean shouldSuppressNativeEnergy() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_ENERGY);
    }

    public static List<DrawItem> buildFromProjection() {
        List<DrawItem> out = new ArrayList<DrawItem>();
        int energy = ArtFramework.projection().controls().energy;
        float sw = com.megacrit.cardcrawl.core.Settings.WIDTH > 0f
                ? com.megacrit.cardcrawl.core.Settings.WIDTH : 1920f;
        float sh = com.megacrit.cardcrawl.core.Settings.HEIGHT > 0f
                ? com.megacrit.cardcrawl.core.Settings.HEIGHT : 1080f;
        out.add(new DrawItem("energy_orb", String.valueOf(energy), energy,
                ResourceIds.energyOrbLayer("red", 1),
                new Rect(sw * 0.06f, sh * 0.11f, sw * 0.13f, 70f)));
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
        return m;
    }
}
