package girparser.model;

import girparser.generator.Conversions;

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
        if (getReturnValue().type.isBitfield()) {
            writer.write("int");
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
            writer.write( " throws org.gtk.interop.GErrorException");
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
                       // We don't support pointers to primitive types yet
                    || (p.array == null && p.type.isPrimitive && p.type.cType.endsWith("*"))
                       // GType is just a long, so we don't support pointers to GType either
                    || (p.array == null && p.type.cType != null && p.type.cType.equals("GType*"))
                       // We don't support callback functions yet
                    || (p.array == null && p.type.isCallback())
                       // We don't support out parameters of type enum yet
                    || (p.direction != null && p.direction.contains("out")
                               && p.type != null && "Enumeration".equals(p.type.girElementType))
                       // Check for types without a name
                    || (p.type != null && p.type.name == null)
            )) {
                return false;
            }
        }
        if (
            // Check for return value without type (probably arrays)
               (rv.type == null)
            // Check for type without name
            || (rv.type.name == null)
            // Check for return value that is a pointer to a primitive type
            || (rv.type.isPrimitive && rv.type.cType.endsWith("*"))
            // We don't support unions and callbacks yet
            || rv.type.isUnion()
            || rv.type.isCallback()
        ) {
            return false;
        }

        // We don't support constructors with exceptions (GError) yet
        return (! (this instanceof Constructor ctr && ctr.throws_ != null));
    }
}
