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
        String output, input;
        try {
            // explicitly trigger initialization
            Gio.javagi$ensureInitialized();

            // create file
            File file = File.newForPath("test.txt");

            // write to file
            input = "test string";
            try (var stream = file.openReadwrite(null)) {
                stream.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8), null);
            }

            // read from file
            try (var stream = file.openReadwrite(null)) {
                Out<byte[]> buffer = new Out<>(new byte[input.length()]);
                stream.getInputStream().read(buffer, null);
                byte[] result = buffer.get();
                output = new String(result);
            }

            // delete file
            file.delete(null);

            assertEquals(output, input);
        } catch (GErrorException | IOException ignored) {}
    }
}
