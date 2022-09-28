package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public interface CallableType {

    Parameters getParameters();
    void setParameters(Parameters ps);

    ReturnValue getReturnValue();
    void setReturnValue(ReturnValue rv);

    default void writeMethodDeclaration(Writer writer,
                                        Doc doc,
                                        String name,
                                        String throws_,
                                        boolean isDefault,
                                        boolean isStatic) throws IOException {
        // Documentation
        if (doc != null) {
            doc.generate(writer, 1);
        }

        // Visibility
        writer.write("    public ");

        // Default interface methods
        if (isDefault) {
            writer.write("default ");
        }

        // Static methods
        if (isStatic) {
            writer.write("static ");
        }

        // Return type
        writer.write(getReturnValue().getReturnType());

        // Method name
        String methodName = Conversions.toLowerCaseJavaName(name);
        if (isDefault) { // Overriding toString() in a default method is not allowed.
            methodName = Conversions.replaceJavaObjectMethodNames(methodName);
        }
        writer.write(" ");
        writer.write(methodName);

        // Parameters
        if (getParameters() != null) {
            writer.write("(");
            getParameters().generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }

        // Exceptions
        if (throws_ != null) {
            writer.write( " throws io.github.jwharm.javagi.GErrorException");
        }
    }

    /** Performs a series of checks to determine if this call can be mapped to C. */
    default boolean isSafeToBind() {
        Parameters ps = getParameters();
        ReturnValue rv = getReturnValue();

        if (ps != null) {

            if (ps.parameterList.stream().anyMatch(p ->

                       // We don't support parameters without type
                       (p.array == null && p.type == null)
                       // We don't support types without a name
                    || (p.type != null && p.type.name == null)
            )) {
                return false;
            }

            // We don't support methods with a callback parameter but no user_data parameter
            if (ps.parameterList.stream().anyMatch(Parameter::isCallbackParameter)
                    && ps.parameterList.stream().noneMatch(Parameter::isUserDataParameter)
            ) {
                return false;
            }
        }

        if (
            // Check for type without name
              (rv.type != null && rv.type.name == null)
            // We don't support unions and callbacks yet
            || (rv.type != null && rv.type.isUnion())
            || (rv.type != null && rv.type.isCallback())
        ) {
            return false;
        }

        return true;
    }
}
