package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

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

        public DrawItem(String id, String label, int energy) {
            this.id = id;
            this.label = label;
            this.energy = energy;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", id);
            m.put("label", label);
            m.put("energy", Integer.valueOf(energy));
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
        out.add(new DrawItem("energy_orb", String.valueOf(energy), energy));
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
