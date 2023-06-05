package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.base.Out;
import org.gnome.gio.File;
import org.gnome.gio.FileCreateFlags;
import org.gnome.gio.Gio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test byte[] marshaling by reading and writing bytes to and from a file
 */
public class FileTest {

    @TempDir
    public static Path tempDir;

    @Test
    public void createFile() {
        String output, input;
        try {
            // explicitly trigger initialization
            Gio.javagi$ensureInitialized();

            // create file
            File file = File.newForPath(tempDir.resolve("test.txt").toString());

            // write to file
            input = "test string";
            try (var stream = file.create(FileCreateFlags.REPLACE_DESTINATION, null)) {
                stream.write(input.getBytes(StandardCharsets.UTF_8), null);
            }

            // read from file
            try (var stream = file.openReadwrite(null)) {
                Out<byte[]> buffer = new Out<>(new byte[input.length()]);
                stream.getInputStream().read(buffer, null);
                byte[] result = buffer.get();
                output = new String(result);
            }

            assertEquals(output, input);
        } catch (GErrorException | IOException e) {
            fail(e);
        }
    }
}
