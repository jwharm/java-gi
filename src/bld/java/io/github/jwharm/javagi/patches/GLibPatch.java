package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;
import io.github.jwharm.javagi.model.Type;

public class GLibPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Incompletely defined
        removeFunction(repo, "clear_error");

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

        inject(repo, "Type", """
                        
            private static final long G_TYPE_FUNDAMENTAL_SHIFT = 2;
            public static final Type G_TYPE_INVALID = new Type(0L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_NONE = new Type(1L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_INTERFACE = new Type(2L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_CHAR = new Type(3L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_UCHAR = new Type(4L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_BOOLEAN = new Type(5L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_INT = new Type(6L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_UINT = new Type(7L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_LONG = new Type(8L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_ULONG = new Type(9L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_INT64 = new Type(10L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_UINT64 = new Type(11L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_ENUM = new Type(12L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_FLAGS = new Type(13L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_FLOAT = new Type(14L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_DOUBLE = new Type(15L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_STRING = new Type(16L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_POINTER = new Type(17L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_BOXED = new Type(18L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_PARAM = new Type(19L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_OBJECT = new Type(20L << G_TYPE_FUNDAMENTAL_SHIFT);
            public static final Type G_TYPE_VARIANT = new Type(21L << G_TYPE_FUNDAMENTAL_SHIFT);
            
        """);
    }
}
