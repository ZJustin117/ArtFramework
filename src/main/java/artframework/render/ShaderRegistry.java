package artframework.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registers GLSL resource paths. Compilation is deferred to runtime GL host.
 * Pure registry is unit-testable without LibGDX.
 */
public final class ShaderRegistry {

    public static final class ShaderDef {
        public final String id;
        public final String vertexClasspath;
        public final String fragmentClasspath;
        private boolean compiled;
        private boolean compileFailed;
        private String compileMessage = "";

        public ShaderDef(String id, String vertexClasspath, String fragmentClasspath) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("shader id required");
            }
            this.id = id;
            this.vertexClasspath = vertexClasspath != null ? vertexClasspath : "";
            this.fragmentClasspath = fragmentClasspath != null ? fragmentClasspath : "";
        }

        public boolean isCompiled() {
            return compiled;
        }

        public boolean isCompileFailed() {
            return compileFailed;
        }

        public String compileMessage() {
            return compileMessage;
        }

        public void markCompiled() {
            compiled = true;
            compileFailed = false;
            compileMessage = "";
        }

        public void markFailed(String message) {
            compiled = false;
            compileFailed = true;
            compileMessage = message != null ? message : "compile failed";
        }

        public void resetCompileState() {
            compiled = false;
            compileFailed = false;
            compileMessage = "";
        }
    }

    private final Map<String, ShaderDef> shaders = new LinkedHashMap<String, ShaderDef>();

    public void register(String id, String vertexClasspath, String fragmentClasspath) {
        shaders.put(id, new ShaderDef(id, vertexClasspath, fragmentClasspath));
    }

    public ShaderDef get(String id) {
        return id == null ? null : shaders.get(id);
    }

    public boolean contains(String id) {
        return id != null && shaders.containsKey(id);
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(shaders.keySet());
    }

    public void clear() {
        shaders.clear();
    }
}
