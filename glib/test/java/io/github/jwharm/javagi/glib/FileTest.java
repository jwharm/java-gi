package io.github.jwharm.javagi.glib;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.base.Out;
import org.gnome.gio.File;
import org.gnome.gio.Gio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test byte[] marshaling by reading and writing bytes to and from a file
 */
public class FileTest {

    @Test
    public void createFile() {
        String read, write;
        try {
            // explicitly trigger initialization
            Gio.javagi$ensureInitialized();

            // create file
            File file = File.newForPath("test.txt");

            // write to file
            write = "test string";
            try (var stream = file.openReadwrite(null)) {
                stream.getOutputStream().write(write.getBytes(StandardCharsets.UTF_8), null);
            }

            // read from file
            try (var stream = file.openReadwrite(null)) {
                Out<byte[]> buffer = new Out<>(new byte[write.length()]);
                stream.getInputStream().read(buffer, null);
                byte[] result = buffer.get();
                read = new String(result);
            }

            // delete file
            file.delete(null);

            assertEquals(read, write);
        } catch (GErrorException | IOException ignored) {}
    }
}
