package artframework.sts1.render;

import artframework.context.CardEntity;
import artframework.context.CardPose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compare hard-sync projection poses against host-reported geometry (milestone 16.4). Pure math —
 * D1 fixtures feed host samples; JUnit uses scripted samples.
 */
public final class GeometryCompare {

    public static final class Sample {
        public final String instanceId;
        public final float x;
        public final float y;
        public final float rotation;
        public final float scale;

        public Sample(String instanceId, float x, float y, float rotation, float scale) {
            this.instanceId = instanceId != null ? instanceId : "";
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scale = scale;
        }
    }

    public static final class Diff {
        public final String instanceId;
        public final String field;
        public final float expected;
        public final float actual;
        public final float delta;

        public Diff(String instanceId, String field, float expected, float actual) {
            this.instanceId = instanceId;
            this.field = field;
            this.expected = expected;
            this.actual = actual;
            this.delta = Math.abs(expected - actual);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("instanceId", instanceId);
            m.put("field", field);
            m.put("expected", Float.valueOf(expected));
            m.put("actual", Float.valueOf(actual));
            m.put("delta", Float.valueOf(delta));
            return m;
        }
    }

    public static final class Report {
        public final boolean ok;
        public final float tolerance;
        public final List<Diff> diffs;
        public final int compared;
        public final int missingInHost;
        public final int missingInProjection;

        public Report(
                boolean ok,
                float tolerance,
                List<Diff> diffs,
                int compared,
                int missingInHost,
                int missingInProjection) {
            this.ok = ok;
            this.tolerance = tolerance;
            this.diffs = diffs;
            this.compared = compared;
            this.missingInHost = missingInHost;
            this.missingInProjection = missingInProjection;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("ok", Boolean.valueOf(ok));
            m.put("tolerance", Float.valueOf(tolerance));
            m.put("compared", Integer.valueOf(compared));
            m.put("missingInHost", Integer.valueOf(missingInHost));
            m.put("missingInProjection", Integer.valueOf(missingInProjection));
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Diff d : diffs) {
                list.add(d.toMap());
            }
            m.put("diffs", list);
            return m;
        }
    }

    private GeometryCompare() {}

    public static Report compare(List<CardEntity> projection, List<Sample> host, float tolerance) {
        float tol = tolerance > 0f ? tolerance : 0.5f;
        Map<String, Sample> hostById = new LinkedHashMap<String, Sample>();
        if (host != null) {
            for (Sample s : host) {
                if (s != null && !s.instanceId.isEmpty()) {
                    hostById.put(s.instanceId, s);
                }
            }
        }
        List<Diff> diffs = new ArrayList<Diff>();
        int compared = 0;
        int missingInHost = 0;
        if (projection != null) {
            for (CardEntity e : projection) {
                if (e == null || e.pose == null) {
                    continue;
                }
                Sample s = hostById.remove(e.instanceId);
                if (s == null) {
                    missingInHost++;
                    continue;
                }
                compared++;
                addIfOff(diffs, e.instanceId, "x", e.pose.x, s.x, tol);
                addIfOff(diffs, e.instanceId, "y", e.pose.y, s.y, tol);
                addIfOff(diffs, e.instanceId, "rotation", e.pose.rotation, s.rotation, tol);
                addIfOff(diffs, e.instanceId, "scale", e.pose.scale, s.scale, tol);
            }
        }
        int missingInProjection = hostById.size();
        boolean ok = diffs.isEmpty() && missingInHost == 0 && missingInProjection == 0;
        return new Report(ok, tol, diffs, compared, missingInHost, missingInProjection);
    }

    public static Sample fromPose(String instanceId, CardPose pose) {
        if (pose == null) {
            return new Sample(instanceId, 0, 0, 0, 1);
        }
        return new Sample(instanceId, pose.x, pose.y, pose.rotation, pose.scale);
    }

    private static void addIfOff(
            List<Diff> diffs, String id, String field, float expected, float actual, float tol) {
        if (Math.abs(expected - actual) > tol) {
            diffs.add(new Diff(id, field, expected, actual));
        }
    }
}
