package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

/**
 * Write Strings to a provided {@link Writer} instance, adding indentation
 * to each new line.
 */
public class SourceWriter implements AutoCloseable {

    private final Writer writer;
    private String indent = "";
    private boolean writeIndent = true;

    /**
     * Create a new SourceWriter with indentation level 0.
     * @param writer the writer to write String values to.
     */
    public SourceWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Increase the indentation level by 4 spaces.
     */
    public void increaseIndent() {
        indent += "    ";
    }

    /**
     * Decrease the indentation level by 4 spaces.
     * @throws IndexOutOfBoundsException when indentation level was already 0
     */
    public void decreaseIndent() {
        indent = indent.substring(4);
    }

    /**
     * Write the provided String. When {@code str} ends with a newline ({@code \n}),
     * the next line will be indented with the current indentation level.
     * @param str
     * @throws IOException
     */
    public void write(String str) throws IOException {
        if (writeIndent) {
            writer.write(indent);
            writeIndent = false;
        }

        writer.write(str);

        if (str.endsWith("\n")) {
            writeIndent = true;
        }
    }

    /**
     * Close the {@code Writer} instance
     * @throws IOException thrown while calling {@link Writer#close()}
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
}
