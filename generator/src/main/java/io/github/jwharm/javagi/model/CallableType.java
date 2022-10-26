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
                                        boolean isInterface,
                                        boolean isStatic) throws IOException {
        // Documentation
        if (doc != null) {
            doc.generate(writer, 1);
        }

        if (this instanceof Method m && "1".equals(m.deprecated)) {
        	writer.write("    @Deprecated\n");
        }
        
        if (isInterface && !isStatic) {
            // Default interface methods
            writer.write("    default ");
        } else {
            // Visibility
            writer.write("    public ");
        }

        // Static methods
        if (isStatic) {
            writer.write("static ");
        }

        // Annotations
        if ((getReturnValue().type != null && !getReturnValue().type.isPrimitive && !getReturnValue().type.isVoid()) || getReturnValue().array != null) {
            writer.write(getReturnValue().nullable ? "@Nullable " : "@NotNull ");
        }

        // Return type
        writer.write(getReturnValue().getReturnType());

        // Method name
        String methodName = Conversions.toLowerCaseJavaName(name);
        if (isInterface) { // Overriding toString() in a default method is not allowed.
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
                       // We don't support out parameter arrays with unknown length
                    || (p.isOutParameter() && p.array != null && p.array.size() == null)
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
        
        // Check for signals with out parameters or arrays
        if (this instanceof Signal && ps != null) {
            if (ps.parameterList.stream().anyMatch(Parameter::isOutParameter)) {
                return false;
            }
            if (ps.parameterList.stream().anyMatch(p -> p.array != null)) {
                return false;
            }
        }

        if (rv.type == null) return true;

        // Check for type without name
        if (rv.type.name == null) return false;

        // We don't support unions and callbacks yet
        if (rv.type.isUnion() || rv.type.isCallback()) return false;

        return true;
    }
}
