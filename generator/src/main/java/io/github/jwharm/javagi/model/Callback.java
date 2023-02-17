package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Callback extends RegisteredType implements CallableType, Closure {

    public ReturnValue returnValue;
    public Parameters parameters;

    public Callback(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(SourceWriter writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);
        generateFunctionalInterface(writer, javaName);
    }

    @Override
    public String getInteropString(String paramName, boolean isPointer) {
        return "(Addressable) " + paramName + ".toCallback()";
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Parameters ps) {
        this.parameters = ps;
    }

    @Override
    public ReturnValue getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(ReturnValue rv) {
        this.returnValue = rv;
    }

    @Override
    public Doc getDoc() {
        return doc;
    }

    @Override
    public String getThrows() {
        return null;
    }
    
    public String getConstructorString() {
        return this.javaName + "::new";
    }
}
