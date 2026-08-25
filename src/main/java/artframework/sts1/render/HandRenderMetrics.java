package artframework.sts1.render;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, machine-readable hand render quality and frame-time evidence. */
public final class HandRenderMetrics {
    public static final int MIN_SAMPLE_FRAMES = 30;
    public static final long P95_BUDGET_NANOS = 16_666_667L;
    private static final long[] SAMPLES = new long[120];
    private static int sampleCount;
    private static int cursor;
    private static int projected;
    private static int rendered;
    private static int missingArt;
    private static int invalidBounds;

    private HandRenderMetrics() {}
    public static long begin() { return System.nanoTime(); }

    public static synchronized void end(long started, int projectedCount, int renderedCount,
            int missing, int invalid) {
        SAMPLES[cursor] = Math.max(0L, System.nanoTime() - started);
        cursor = (cursor + 1) % SAMPLES.length;
        if (sampleCount < SAMPLES.length) sampleCount++;
        projected = projectedCount;
        rendered = renderedCount;
        missingArt = missing;
        invalidBounds = invalid;
    }

    public static synchronized Map<String, Object> probeSlice() {
        long[] sorted = Arrays.copyOf(SAMPLES, sampleCount);
        Arrays.sort(sorted);
        long p95 = sorted.length == 0 ? 0L : sorted[(sorted.length * 95 + 99) / 100 - 1];
        boolean ok = sampleCount >= MIN_SAMPLE_FRAMES && projected == rendered
                && missingArt == 0 && invalidBounds == 0 && p95 <= P95_BUDGET_NANOS;
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("ok", Boolean.valueOf(ok));
        out.put("sampleFrames", Integer.valueOf(sampleCount));
        out.put("projected", Integer.valueOf(projected));
        out.put("rendered", Integer.valueOf(rendered));
        out.put("missingArt", Integer.valueOf(missingArt));
        out.put("invalidBounds", Integer.valueOf(invalidBounds));
        out.put("p95Nanos", Long.valueOf(p95));
        out.put("p95BudgetNanos", Long.valueOf(P95_BUDGET_NANOS));
        out.put("minSampleFrames", Integer.valueOf(MIN_SAMPLE_FRAMES));
        return out;
    }

    public static synchronized void resetForTests() {
        Arrays.fill(SAMPLES, 0L);
        sampleCount = cursor = projected = rendered = missingArt = invalidBounds = 0;
    }
}
