package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;

import java.util.LinkedHashMap;
import java.util.Map;

/** Energy orb present from ControlsView.energy (25.2). */
public final class EnergyDrawPath {

    private EnergyDrawPath() {}

    public static boolean shouldSuppressNativeEnergy() {
        return Sts1RenderPipeline.plan().shouldSuppressNative(SurfaceIds.COMBAT_ENERGY);
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        int energy = ArtFramework.projection().controls().energy;
        m.put("energy", Integer.valueOf(energy));
        m.put("label", String.valueOf(energy));
        m.put("suppressNativeEnergy", Boolean.valueOf(shouldSuppressNativeEnergy()));
        m.put("presentLevel", FullPresentMode.energyLevel().name());
        return m;
    }
}
