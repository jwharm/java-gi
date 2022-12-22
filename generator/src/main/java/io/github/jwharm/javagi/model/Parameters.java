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

    public void generateJavaParameters(Writer writer, boolean pointerForArray) throws IOException {
        int counter = 0;
        for (Parameter p : parameterList) {
            if (p.isInstanceParameter() || p.isUserDataParameter()) {
                continue;
            }
            if (counter++ > 0) {
                writer.write(", ");
            }
            p.writeTypeAndName(writer, pointerForArray);
        }
    }

    public void generateJavaParameterNames(Writer writer) throws IOException {
        int counter = 0;
        for (Parameter p : parameterList) {
            if (p.isInstanceParameter() || p.isUserDataParameter()) {
                continue;
            }
            if (counter++ > 0) {
                writer.write(", ");
            }
            writer.write(p.varargs ? "varargs" : p.name);
        }
    }

    public void generateCParameters(Writer writer, String throws_) throws IOException {
        int counter = 0;

        for (Parameter p : parameterList) {
            if (counter++ > 0) {
                writer.write(",");
            }
            writer.write("\n                    ");

            // Generate null-check. But don't null-check parameters that are hidden from the Java API, or primitive values
            if (p.checkNull()) {
                writer.write("(Addressable) (" + (p.varargs ? "varargs" : p.name) + " == null ? MemoryAddress.NULL : ");
            }

            // this
            if (p.isInstanceParameter()) {
                writer.write("handle()");

            // user_data
            } else if (p.isUserDataParameter()) {
                writer.write("(Addressable) MemoryAddress.NULL");

            // Varargs
            } else if (p.varargs) {
                writer.write("varargs");

            // Custom interop
            } else {
                p.marshalJavaToNative(writer, p.name, false);
            }

            // Closing parentheses for null-check
            if (p.checkNull()) {
                writer.write(")");
            }
        }

        // GError
        if (throws_ != null) {
            writer.write(",\n                    (Addressable) GERROR");
        }
    }

    /**
     * Generate preprocessing statements for all parameters
     * @param writer The source code file writer
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generatePreprocessing(Writer writer, int indent) throws IOException {
        for (Parameter p : parameterList) {
            p.generatePreprocessing(writer, indent);
        }
    }
    
    /**
     * Generate postprocessing statements for all parameters
     * @param writer The source code file writer
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generatePostprocessing(Writer writer, int indent) throws IOException {
        // First the regular (non-array) out-parameters. These could include an out-parameter with 
        // the length of an array out-parameter, so we have to process these first.
        for (Parameter p : parameterList) {
            if (p.array == null) {
                p.generatePostprocessing(writer, indent);
            }
        }
        // Secondly, process the array out parameters
        for (Parameter p : parameterList) {
            if (p.array != null) {
                p.generatePostprocessing(writer, indent);
            }
        }
    }
}
