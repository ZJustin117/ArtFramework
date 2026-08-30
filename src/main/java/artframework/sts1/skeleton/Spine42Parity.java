package artframework.sts1.skeleton;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure CPU parity checks for Spine 4.2 attachment expansion.
 *
 * This is intentionally not a GL, batch, or screenshot parity layer. It validates the
 * vertex/coverage contract that the provider can derive on the CPU before any native draw.
 */
public final class Spine42Parity {

    private Spine42Parity() {}

    public static ParityResult region(float[] world, float[] uvs, float packedColor) {
        try {
            float[] vertices = Sts1Spine42Provider.regionVertices(world, uvs, packedColor);
            Orientation orientation = orientation(vertices, 0);
            if (orientation == Orientation.DEGENERATE) {
                return ParityResult.fail(Kind.REGION, Reason.DEGENERATE, "region quad is degenerate", world, uvs, null, null);
            }
            if (orientation != Orientation.CLOCKWISE) {
                return ParityResult.fail(Kind.REGION, Reason.WINDING, "region quad winding must be clockwise for Batch order", world, uvs, null, null);
            }
            return ParityResult.pass(Kind.REGION, vertices, world, uvs, null, null, null);
        } catch (IllegalArgumentException e) {
            return ParityResult.fail(Kind.REGION, Reason.INVALID_DATA, e.getMessage(), world, uvs, null, null);
        }
    }

    public static ParityResult mesh(float[] world, float[] uvs, short[] triangles, float packedColor) {
        if (world == null || uvs == null || triangles == null) {
            return ParityResult.fail(Kind.MESH, Reason.INVALID_DATA, "mesh parity requires world, uvs, and triangles", world, uvs, triangles, null);
        }
        if ((world.length & 1) != 0 || world.length == 0) {
            return ParityResult.fail(Kind.MESH, Reason.INVALID_DATA, "mesh world vertices must contain x/y pairs", world, uvs, triangles, null);
        }
        if (uvs.length < world.length) {
            return ParityResult.fail(Kind.MESH, Reason.INVALID_DATA, "mesh UV count must cover every world vertex", world, uvs, triangles, null);
        }
        if ((triangles.length % 3) != 0) {
            return ParityResult.fail(Kind.MESH, Reason.INVALID_DATA, "mesh triangle indices must be grouped by 3", world, uvs, triangles, null);
        }
        if (triangles.length == 0) {
            return ParityResult.fail(Kind.MESH, Reason.DEGENERATE, "mesh has no triangles", world, uvs, triangles, null);
        }

        int vertexCount = world.length / 2;
        for (int triangle = 0; triangle < triangles.length; triangle += 3) {
            int a = triangles[triangle] & 0xffff;
            int b = triangles[triangle + 1] & 0xffff;
            int c = triangles[triangle + 2] & 0xffff;
            if (a >= vertexCount || b >= vertexCount || c >= vertexCount) {
                return ParityResult.fail(Kind.MESH, Reason.INVALID_DATA, "mesh triangle index outside world vertices at triangle " + (triangle / 3), world, uvs, triangles, triangle / 3);
            }
            if (a == b || b == c || a == c) {
                return ParityResult.fail(Kind.MESH, Reason.DEGENERATE, "mesh triangle is degenerate at triangle " + (triangle / 3), world, uvs, triangles, triangle / 3);
            }
            Orientation orientation = orientation(world, a, b, c);
            if (orientation == Orientation.DEGENERATE) {
                return ParityResult.fail(Kind.MESH, Reason.DEGENERATE, "mesh triangle collapses to zero area at triangle " + (triangle / 3), world, uvs, triangles, triangle / 3);
            }
            if (orientation == Orientation.CLOCKWISE) {
                return ParityResult.fail(Kind.MESH, Reason.WINDING, "mesh triangle winding is clockwise at triangle " + (triangle / 3), world, uvs, triangles, triangle / 3);
            }
        }

        float[] coverage = new float[triangles.length / 3 * 4 * 5];
        int offset = 0;
        for (int triangle = 0; triangle < triangles.length; triangle += 3) {
            float[] quad = Sts1Spine42Provider.meshTriangleVertices(world, uvs, triangles, triangle, packedColor);
            System.arraycopy(quad, 0, coverage, offset, quad.length);
            offset += quad.length;
        }
        return ParityResult.pass(Kind.MESH, coverage, world, uvs, triangles, null, null);
    }

    /** Compares generated five-float Batch vertices against a checked-in expected vector. */
    public static ParityResult compareCoverage(Kind kind, float[] expected, float[] actual, float epsilon) {
        if (kind == null || expected == null || actual == null || epsilon < 0f) {
            return ParityResult.fail(kind, Reason.INVALID_DATA, "coverage comparison requires kind, vectors, and non-negative epsilon", null, null, null, null);
        }
        if (expected.length != actual.length) {
            return ParityResult.fail(kind, Reason.MISMATCH,
                    "coverage vector length differs: expected " + expected.length + ", actual " + actual.length,
                    null, null, null, null);
        }
        for (int index = 0; index < expected.length; index++) {
            if (Math.abs(expected[index] - actual[index]) > epsilon) {
                return ParityResult.fail(kind, Reason.MISMATCH,
                        "coverage value differs at index " + index + ": expected " + expected[index] + ", actual " + actual[index],
                        null, null, null, Integer.valueOf(index));
            }
        }
        return ParityResult.pass(kind, Arrays.copyOf(actual, actual.length), null, null, null, null, null);
    }

    public enum Kind { REGION, MESH }

    public enum Reason { OK, INVALID_DATA, DEGENERATE, WINDING, MISMATCH }

    enum Orientation { COUNTER_CLOCKWISE, CLOCKWISE, DEGENERATE }

    public static final class ParityResult {
        public final Kind kind;
        public final Reason reason;
        public final boolean passed;
        public final String message;
        public final float[] coverageVertices;
        public final float[] world;
        public final float[] uvs;
        public final short[] triangles;
        public final Integer triangleIndex;

        private ParityResult(Kind kind, Reason reason, boolean passed, String message,
                float[] coverageVertices, float[] world, float[] uvs, short[] triangles, Integer triangleIndex) {
            this.kind = kind;
            this.reason = reason;
            this.passed = passed;
            this.message = message;
            this.coverageVertices = coverageVertices;
            this.world = world;
            this.uvs = uvs;
            this.triangles = triangles;
            this.triangleIndex = triangleIndex;
        }

        static ParityResult pass(Kind kind, float[] coverageVertices, float[] world, float[] uvs,
                short[] triangles, Integer triangleIndex, String message) {
            return new ParityResult(kind, Reason.OK, true, message,
                    coverageVertices, clone(world), clone(uvs), clone(triangles), triangleIndex);
        }

        static ParityResult fail(Kind kind, Reason reason, String message, float[] world,
                float[] uvs, short[] triangles, Integer triangleIndex) {
            return new ParityResult(kind, reason, false, message, null, clone(world), clone(uvs), clone(triangles), triangleIndex);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("kind", kind.name());
            out.put("passed", Boolean.valueOf(passed));
            out.put("reason", reason.name());
            out.put("message", message);
            out.put("triangleIndex", triangleIndex);
            out.put("coverageVertexCount", Integer.valueOf(coverageVertices == null ? 0 : coverageVertices.length / 5));
            return out;
        }

        public void assertPassed() {
            if (!passed) {
                throw new AssertionError(format());
            }
        }

        public String format() {
            return kind + " parity " + (passed ? "passed" : "failed") + " [" + reason + "]"
                    + (message == null ? "" : ": " + message);
        }

        private static float[] clone(float[] input) {
            return input == null ? null : Arrays.copyOf(input, input.length);
        }

        private static short[] clone(short[] input) {
            return input == null ? null : Arrays.copyOf(input, input.length);
        }
    }

    static Orientation orientation(float[] vertices, int offset) {
        if (vertices == null || vertices.length < offset + 15) {
            return Orientation.DEGENERATE;
        }
        return orientation(
                vertices[offset], vertices[offset + 1],
                vertices[offset + 5], vertices[offset + 6],
                vertices[offset + 10], vertices[offset + 11]);
    }

    static Orientation orientation(float[] world, int a, int b, int c) {
        if (world == null || world.length < Math.max(a, Math.max(b, c)) * 2 + 2) {
            return Orientation.DEGENERATE;
        }
        return orientation(
                world[a * 2], world[a * 2 + 1],
                world[b * 2], world[b * 2 + 1],
                world[c * 2], world[c * 2 + 1]);
    }

    static Orientation orientation(float ax, float ay, float bx, float by, float cx, float cy) {
        float area2 = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (area2 > 0f) {
            return Orientation.COUNTER_CLOCKWISE;
        }
        if (area2 < 0f) {
            return Orientation.CLOCKWISE;
        }
        return Orientation.DEGENERATE;
    }
}
