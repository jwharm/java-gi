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
        if (getReturnValue().type.isPrimitive && getReturnValue().type.isPointer()) {
            writer.write("Pointer" + Conversions.primitiveClassName(getReturnValue().type.simpleJavaType));
        } else if (getReturnValue().type.cType != null && getReturnValue().type.cType.endsWith("**")) {
            writer.write("PointerResource<" + getReturnValue().type.qualifiedJavaType + ">");
        } else {
            writer.write(getReturnValue().type.qualifiedJavaType);
        }

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
            getParameters().generateJavaParameters(writer);
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
                       // We don't support out parameters of type enum yet
                    || (p.direction != null && p.direction.contains("out")
                               && p.type != null && "Enumeration".equals(p.type.girElementType))
                       // We don't support arrays of enum types yet
                    || (p.array != null && p.array.type != null && "Enumeration".equals(p.array.type.girElementType))
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
            // Check for return value without type (probably arrays)
               (rv.type == null)
            // Check for type without name
            || (rv.type.name == null)
            // We don't support unions and callbacks yet
            || rv.type.isUnion()
            || rv.type.isCallback()
        ) {
            return false;
        }

        return true;
    }
}
