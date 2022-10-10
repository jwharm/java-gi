package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class ReturnValue extends Parameter {

    public ReturnValue(GirElement parent, String transferOwnership, String nullable) {
        super(parent, null, transferOwnership, nullable, null, null);
    }

    public void generateReturnStatement(Writer writer, int indent) throws IOException {
        if (type != null && type.isVoid()) {
            return;
        }
        writer.write(" ".repeat(indent * 4) + "return ");
        generateReverseInterop(writer, "RESULT");
        writer.write(";\n");
    }
}
