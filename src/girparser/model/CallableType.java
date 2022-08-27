package girparser.model;

import girparser.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public interface CallableType {

    public Parameters getParameters();
    public void setParameters(Parameters ps);

    public ReturnValue getReturnValue();
    public void setReturnValue(ReturnValue rv);

    default void writeMethodDeclaration(Writer writer, Doc doc, String name, String throws_) throws IOException {
        // Documentation
        if (doc != null) {
            doc.generate(writer, 1);
        }

        // Visibility and returntype
        writer.write("    public ");
        if (getReturnValue().type.isBitfield()) {
            writer.write("int");
        } else {
            writer.write(getReturnValue().type.qualifiedJavaType);
        }

        // Method name
        writer.write(" ");
        writer.write(Conversions.toLowerCaseJavaName(name));

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
                       // We don't support callback functions yet
                    || (p.array == null && p.type.isCallback())
            )) {
                return false;
            }
        }
        if (
            // Check for return value without type (probably arrays)
               (rv.type == null)
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
