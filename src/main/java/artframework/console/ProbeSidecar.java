package artframework.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Writes probe snapshots to the local file and the device-lab shared file. */
public final class ProbeSidecar {
    private static final String LOCAL_NAME = "art_probe_latest.log";
    private static final String HEARTBEAT_LOCAL_NAME = "art_heartbeat_latest.log";
    private static final String EXTERNAL_PATH = "Android/data/io.stamethyst/files/sts/art_probe_latest.log";
    private static final String COMMAND_LOCAL_NAME = "art_command_latest.log";
    private static final String COMMAND_EXTERNAL_PATH =
            "Android/data/io.stamethyst/files/sts/art_command_latest.log";
    private static final String HEARTBEAT_EXTERNAL_PATH =
            "Android/data/io.stamethyst/files/sts/art_heartbeat_latest.log";

    private ProbeSidecar() {}

    public static boolean write(String line) {
        boolean local = false;
        boolean external = false;
        try { local = writeAtomically(Gdx.files.local(LOCAL_NAME), line); } catch (Throwable ignored) {}
        try { external = writeAtomically(Gdx.files.external(EXTERNAL_PATH), line); } catch (Throwable ignored) {}
        return local || external;
    }

    public static void writeCommand(String line) {
        try { writeAtomically(Gdx.files.local(COMMAND_LOCAL_NAME), line); } catch (Throwable ignored) {}
        try { writeAtomically(Gdx.files.external(COMMAND_EXTERNAL_PATH), line); } catch (Throwable ignored) {}
    }

    public static boolean writeHeartbeat(String line) {
        boolean local = false;
        boolean external = false;
        try { local = writeAtomically(Gdx.files.local(HEARTBEAT_LOCAL_NAME), line); } catch (Throwable ignored) {}
        try { external = writeAtomically(Gdx.files.external(HEARTBEAT_EXTERNAL_PATH), line); } catch (Throwable ignored) {}
        return local || external;
    }

    static boolean writeBoth(FileHandle first, FileHandle second, String line) {
        boolean firstWritten = writeAtomically(first, line);
        boolean secondWritten = writeAtomically(second, line);
        return firstWritten || secondWritten;
    }

    static boolean writeAtomically(FileHandle file, String line) {
        FileHandle temporary = null;
        try {
            temporary = file.sibling(file.name() + ".tmp");
            temporary.writeString(line + "\n", false, "UTF-8");
            Files.move(temporary.file().toPath(), file.file().toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (temporary != null) {
                try {
                    if (temporary.exists()) temporary.delete();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
