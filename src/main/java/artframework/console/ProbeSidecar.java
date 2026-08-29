package artframework.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/** Writes probe snapshots to the local file and the device-lab shared file. */
public final class ProbeSidecar {
    private static final String LOCAL_NAME = "art_probe_latest.log";
    private static final String EXTERNAL_PATH = "Android/data/io.stamethyst/files/sts/art_probe_latest.log";

    private ProbeSidecar() {}

    public static void write(String line) {
        write(Gdx.files.local(LOCAL_NAME), line);
        write(Gdx.files.external(EXTERNAL_PATH), line);
    }

    private static void write(FileHandle file, String line) {
        try {
            file.writeString(line + "\n", false, "UTF-8");
        } catch (Throwable ignored) {
            // A probe remains useful when one storage mount is unavailable.
        }
    }
}
