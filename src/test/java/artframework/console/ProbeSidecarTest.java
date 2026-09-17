package artframework.console;

import com.badlogic.gdx.files.FileHandle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class ProbeSidecarTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test public void sameDirectoryAtomicMoveRepeatedlyReplacesExistingFile() throws Exception {
        File destination = temporaryFolder.newFile("probe.log");
        FileHandle file = new FileHandle(destination);
        file.writeString("old\n", false, "UTF-8");

        for (int i = 0; i < 5; i++) {
            String line = "snapshot-" + i;
            assertTrue(ProbeSidecar.writeAtomically(file, line));
            assertEquals(line + "\n", file.readString("UTF-8"));
            assertFalse(new File(destination.getParentFile(), "probe.log.tmp").exists());
        }
    }

    @Test public void oneMountSuccessMakesCombinedPublicationSuccessful() throws Exception {
        FileHandle valid = new FileHandle(temporaryFolder.newFile("external.log"));

        assertTrue(ProbeSidecar.writeBoth(null, valid, "line"));
        assertEquals("line\n", valid.readString("UTF-8"));
    }

    @Test public void failedAtomicMoveDoesNotTruncateExistingDestination() throws Exception {
        File destination = temporaryFolder.newFolder("existing-destination");
        File marker = new File(destination, "marker.txt");
        FileHandle markerHandle = new FileHandle(marker);
        markerHandle.writeString("keep", false, "UTF-8");

        assertFalse(ProbeSidecar.writeAtomically(new FileHandle(destination), "replacement"));
        assertEquals("keep", markerHandle.readString("UTF-8"));
        assertFalse(new File(destination.getParentFile(), "existing-destination.tmp").exists());
    }
}
