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
        setReturnFloating(findConstructor(repo, "Variant", "new_array"));
        setReturnFloating(findConstructor(repo, "Variant", "new_boolean"));
        setReturnFloating(findConstructor(repo, "Variant", "new_byte"));
        setReturnFloating(findConstructor(repo, "Variant", "new_bytestring"));
        setReturnFloating(findConstructor(repo, "Variant", "new_bytestring_array"));
        setReturnFloating(findConstructor(repo, "Variant", "new_dict_entry"));
        setReturnFloating(findConstructor(repo, "Variant", "new_double"));
        setReturnFloating(findConstructor(repo, "Variant", "new_fixed_array"));
        setReturnFloating(findConstructor(repo, "Variant", "new_from_bytes"));
        setReturnFloating(findConstructor(repo, "Variant", "new_handle"));
        setReturnFloating(findConstructor(repo, "Variant", "new_int16"));
        setReturnFloating(findConstructor(repo, "Variant", "new_int32"));
        setReturnFloating(findConstructor(repo, "Variant", "new_int64"));
        setReturnFloating(findConstructor(repo, "Variant", "new_maybe"));
        setReturnFloating(findConstructor(repo, "Variant", "new_object_path"));
        setReturnFloating(findConstructor(repo, "Variant", "new_objv"));
        setReturnFloating(findConstructor(repo, "Variant", "new_parsed"));
        setReturnFloating(findConstructor(repo, "Variant", "new_parsed_va"));
        setReturnFloating(findConstructor(repo, "Variant", "new_printf"));
        setReturnFloating(findConstructor(repo, "Variant", "new_signature"));
        setReturnFloating(findConstructor(repo, "Variant", "new_string"));
        setReturnFloating(findConstructor(repo, "Variant", "new_strv"));
        setReturnFloating(findConstructor(repo, "Variant", "new_take_string"));
        setReturnFloating(findConstructor(repo, "Variant", "new_tuple"));
        setReturnFloating(findConstructor(repo, "Variant", "new_uint16"));
        setReturnFloating(findConstructor(repo, "Variant", "new_uint32"));
        setReturnFloating(findConstructor(repo, "Variant", "new_uint64"));
        setReturnFloating(findConstructor(repo, "Variant", "new_va"));
        setReturnFloating(findConstructor(repo, "Variant", "new_variant"));
    }
}
