package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Parameters extends GirElement {

    public final List<Parameter> parameterList = new ArrayList<>();

    public Parameters(GirElement parent) {
        super(parent);
    }

    public void generateJavaParameters(Writer writer) throws IOException {
        int counter = 0;
        for (Parameter p : parameterList) {
            if (! (p instanceof InstanceParameter)) {
                if (counter++ > 0) {
                    writer.write(", ");
                }
                p.generateTypeAndName(writer);
            }
        }
    }

    public void generateCParameters(Writer writer, String throws_) throws IOException {
        int counter = 0;
        for (Parameter p : parameterList) {
            if (counter++ > 0) {
                writer.write(", ");
            }
            if (p instanceof InstanceParameter) {
                writer.write("HANDLE()");
            } else {
                p.generateInterop(writer);
            }
        }
        if (throws_ != null) {
            writer.write(", GERROR");
        }
    }
}
