package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Constructor extends Method {

    public Constructor(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent, name, cIdentifier, deprecated, throws_, null, null, null);
    }

    public void generate(SourceWriter writer) throws IOException {
        String privateMethodName = "construct" + Conversions.toCamelCase(name, true);
        writer.write("\n");

        // Docs
        if (doc != null) {
            doc.generate(writer, false);
        }

        // @Deprecated
        if ("1".equals(deprecated)) {
            writer.write("@Deprecated\n");
        }

        // Name
        writer.write("public ");
        writer.write(((RegisteredType) parent).javaName);

        // Parameters
        writer.write("(");
        if (parameters != null) {
            parameters.generateJavaParameters(writer, false);
        }
        writer.write(")");

        // Throws
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }

        // Unsupported platforms
        if (doPlatformCheck() || parent.platforms.size() < 3) {
            writer.write((throws_ != null ? ", " : " throws ") + "UnsupportedPlatformException");
        }

        writer.write(" {\n");
        writer.increaseIndent();

        // Call super(constructNew())
        writer.write("super(" + privateMethodName + "(");
        if (parameters != null) {
            parameters.generateJavaParameterNames(writer);
        }
        writer.write("));\n");

        // Add instance to cache
        writer.write("InstanceCache.put(handle(), this);\n");

        writer.decreaseIndent();
        writer.write("}\n");

        // Generate constructor helper
        generateConstructorHelper(writer, privateMethodName);
    }

    public void generateNamed(SourceWriter writer) throws IOException {
        String privateMethodName = "construct" + Conversions.toCamelCase(name, true);
        RegisteredType constructed = (RegisteredType) parent;

        // Return value should always be the constructed type, but it is often specified in the GIR as
        // one of its parent types. For example, button#newWithLabel returns a Widget instead of a Button.
        // We override this for constructors, to always return the constructed type.
        returnValue.type = new Type(returnValue, constructed.name, constructed.cType + "*");
        returnValue.type.girElementInstance = constructed;
        returnValue.type.girElementType = constructed.getClass().getSimpleName();
        returnValue.type.init(constructed.name);

        writer.write("\n");

        // Docs
        if (doc != null) {
            doc.generate(writer, false);
        }

        // @Deprecated
        if ("1".equals(deprecated)) {
            writer.write("@Deprecated\n");
        }

        // Name
        writer.write("public static " + constructed.javaName + " " + Conversions.toLowerCaseJavaName(name));

        // Parameters
        writer.write("(");
        if (parameters != null) {
            parameters.generateJavaParameters(writer, false);
        }
        writer.write(")");

        // Throws
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }

        // Unsupported platforms
        if (doPlatformCheck() || parent.platforms.size() < 3) {
            writer.write((throws_ != null ? ", " : " throws ") + "UnsupportedPlatformException");
        }

        writer.write(" {\n");
        writer.increaseIndent();

        // Check for unsupported platforms
        generatePlatformCheck(writer);

        // Call native constructor function
        writer.write("var _result = " + privateMethodName + "(");
        if (parameters != null) {
            parameters.generateJavaParameterNames(writer);
        }
        writer.write(");\n");

        // Return statement
        returnValue.generateReturnStatement(writer);

        writer.decreaseIndent();
        writer.write("}\n");

        // Constructor helper
        generateConstructorHelper(writer, privateMethodName);
    }

    // Because constructors sometimes throw exceptions, we need to allocate a GError segment before
    // calling "super(..., _gerror)", which is not allowed - the super() call must
    // be the first statement in the constructor. Therefore, we always generate a private method that
    // prepares a GError memorysegment if necessary, calls the C API and throws the GErrorException
    // (if necessary). The "real" constructor just calls super(private_method());
    private void generateConstructorHelper(SourceWriter writer, String methodName) throws IOException {

        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Helper function for the (@code " + name + "} constructor\n");
        writer.write(" */\n");
        writer.write("private static MemorySegment " + methodName);

        // Parameters
        writer.write("(");
        if (parameters != null) {
            parameters.generateJavaParameters(writer, false);
        }
        writer.write(")");

        // Exceptions
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }

        // Check for unsupported platforms
        if (doPlatformCheck() || parent.platforms.size() < 3) {
            writer.write((throws_ != null ? ", " : " throws ") + "UnsupportedPlatformException");
        }

        writer.write(" {\n");
        writer.increaseIndent();

        // Check for unsupported platforms
        generatePlatformCheck(writer);

        // FunctionDescriptor of the native function signature
        writer.write("FunctionDescriptor _fdesc = ");
        boolean varargs = generateFunctionDescriptor(writer);
        writer.write(";\n");

        // Generate try-with-resources?
        boolean hasScope = allocatesMemory();
        if (hasScope) {
            writer.write("try (Arena _arena = Arena.openConfined()) {\n");
            writer.increaseIndent();
        }

        // Generate preprocessing statements for all parameters
        if (parameters != null) {
            parameters.generatePreprocessing(writer);
        }

        // Allocate GError pointer
        if (throws_ != null) {
            writer.write("MemorySegment _gerror = _arena.allocate(ValueLayout.ADDRESS);\n");
        }
        
        // Generate the return type
        writer.write("MemorySegment _result;\n");
        
        // The method call is wrapped in a try-catch block
        writer.write("try {\n");
        writer.increaseIndent();

        // Invoke to the method handle
        writer.write("_result = (MemorySegment) Interop.downcallHandle(\"" + cIdentifier + "\", _fdesc, " + varargs + ").invokeExact(");

        // Marshall the parameters to the native types
        if (parameters != null) {
            parameters.marshalJavaToNative(writer, throws_);
        }
        writer.write(");\n");
        
        // If something goes wrong in the invokeExact() call
        writer.decreaseIndent();
        writer.write("} catch (Throwable _err) {\n");
        writer.write("    throw new AssertionError(\"Unexpected exception occured: \", _err);\n");
        writer.write("}\n");

        // Throw GErrorException
        if (throws_ != null) {
            writer.write("if (GErrorException.isErrorSet(_gerror)) {\n");
            writer.write("    throw new GErrorException(_gerror);\n");
            writer.write("}\n");
        }

        // Generate post-processing actions for parameters
        if (parameters != null) {
            parameters.generatePostprocessing(writer);
        }
        
        writer.write("return _result;\n");

        // End of memory allocation scope
        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
