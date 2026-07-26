package spireui.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles {@link ShaderRegistry} entries into LibGDX {@link ShaderProgram}s.
 * Call only on the GL thread after assets/context exist. Failures are recorded on
 * {@link ShaderRegistry.ShaderDef} and never throw to the game loop.
 */
public final class ShaderRuntime {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Map<String, ShaderProgram> programs = new LinkedHashMap<String, ShaderProgram>();

    public ShaderProgram get(String id) {
        return id == null ? null : programs.get(id);
    }

    public boolean has(String id) {
        return id != null && programs.containsKey(id);
    }

    public int programCount() {
        return programs.size();
    }

    public List<String> compiledIds() {
        return Collections.unmodifiableList(new ArrayList<String>(programs.keySet()));
    }

    /**
     * Compile every registered shader. Disposes previous programs for the same ids.
     *
     * @return number of successful compiles
     */
    public int compileAll(ShaderRegistry registry) {
        if (registry == null) {
            return 0;
        }
        int ok = 0;
        for (String id : new ArrayList<String>(registry.ids())) {
            if (compileOne(registry, id)) {
                ok++;
            }
        }
        return ok;
    }

    public boolean compileOne(ShaderRegistry registry, String id) {
        if (registry == null || id == null) {
            return false;
        }
        ShaderRegistry.ShaderDef def = registry.get(id);
        if (def == null) {
            return false;
        }
        dispose(id);
        def.resetCompileState();
        try {
            String vert = readClasspath(def.vertexClasspath);
            String frag = readClasspath(def.fragmentClasspath);
            if (vert.isEmpty() || frag.isEmpty()) {
                def.markFailed("empty shader source for " + id);
                return false;
            }
            ShaderProgram program = new ShaderProgram(vert, frag);
            if (!program.isCompiled()) {
                String log = program.getLog();
                program.dispose();
                def.markFailed(log != null ? log : "compile failed");
                return false;
            }
            programs.put(id, program);
            def.markCompiled();
            return true;
        } catch (Throwable t) {
            def.markFailed(t.getMessage() != null ? t.getMessage() : t.getClass().getName());
            return false;
        }
    }

    /**
     * Test helper: mark def compiled without GL (no real program).
     */
    public void markCompiledForTests(ShaderRegistry registry, String id) {
        if (registry == null || id == null) {
            return;
        }
        ShaderRegistry.ShaderDef def = registry.get(id);
        if (def != null) {
            def.markCompiled();
        }
    }

    public void dispose(String id) {
        ShaderProgram p = programs.remove(id);
        if (p != null) {
            try {
                p.dispose();
            } catch (Throwable ignored) {
            }
        }
    }

    public void disposeAll() {
        List<String> ids = new ArrayList<String>(programs.keySet());
        for (String id : ids) {
            dispose(id);
        }
    }

    private static String readClasspath(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            return "";
        }
        InputStream in = ShaderRuntime.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            // try leading slash stripped
            if (path.startsWith("/")) {
                in = ShaderRuntime.class.getClassLoader().getResourceAsStream(path.substring(1));
            }
        }
        if (in == null) {
            throw new IOException("shader resource not found: " + path);
        }
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) >= 0) {
                buf.write(chunk, 0, n);
            }
            return new String(buf.toByteArray(), UTF_8);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }
}
