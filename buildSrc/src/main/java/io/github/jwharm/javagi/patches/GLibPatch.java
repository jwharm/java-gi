package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;
import io.github.jwharm.javagi.model.Type;

public class GLibPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Incompletely defined
        removeFunction(repo, "clear_error");

        // A getType() method is generated already by java-gi
        renameMethod(repo, "Variant", "get_type", "get_variant_type");

        // GPid is defined as gint on linux vs gpointer on windows
        Type pid = repo.namespace.registeredTypeMap.get("Pid").type;
        pid.simpleJavaType = "int";
        pid.qualifiedJavaType = "int";
        pid.isPrimitive = true;

        // These calls return floating references
        setReturnFloating(findConstructor(repo, "Variant", "array"));
        setReturnFloating(findConstructor(repo, "Variant", "boolean"));
        setReturnFloating(findConstructor(repo, "Variant", "byte"));
        setReturnFloating(findConstructor(repo, "Variant", "bytestring"));
        setReturnFloating(findConstructor(repo, "Variant", "bytestring_array"));
        setReturnFloating(findConstructor(repo, "Variant", "dict_entry"));
        setReturnFloating(findConstructor(repo, "Variant", "double"));
        setReturnFloating(findConstructor(repo, "Variant", "fixed_array"));
        setReturnFloating(findConstructor(repo, "Variant", "from_bytes"));
        setReturnFloating(findConstructor(repo, "Variant", "handle"));
        setReturnFloating(findConstructor(repo, "Variant", "int16"));
        setReturnFloating(findConstructor(repo, "Variant", "int32"));
        setReturnFloating(findConstructor(repo, "Variant", "int64"));
        setReturnFloating(findConstructor(repo, "Variant", "maybe"));
        setReturnFloating(findConstructor(repo, "Variant", "object_path"));
        setReturnFloating(findConstructor(repo, "Variant", "objv"));
        setReturnFloating(findConstructor(repo, "Variant", "parsed"));
        setReturnFloating(findConstructor(repo, "Variant", "parsed_va"));
        setReturnFloating(findConstructor(repo, "Variant", "printf"));
        setReturnFloating(findConstructor(repo, "Variant", "signature"));
        setReturnFloating(findConstructor(repo, "Variant", "string"));
        setReturnFloating(findConstructor(repo, "Variant", "strv"));
        setReturnFloating(findConstructor(repo, "Variant", "take_string"));
        setReturnFloating(findConstructor(repo, "Variant", "tuple"));
        setReturnFloating(findConstructor(repo, "Variant", "uint16"));
        setReturnFloating(findConstructor(repo, "Variant", "uint32"));
        setReturnFloating(findConstructor(repo, "Variant", "uint64"));
        setReturnFloating(findConstructor(repo, "Variant", "va"));
        setReturnFloating(findConstructor(repo, "Variant", "variant"));
    }
}
