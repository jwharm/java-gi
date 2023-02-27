import ext.*
import io.github.jwharm.javagi.generator.PatchSet.*

plugins {
    id("java-gi.library-conventions")
}

setupGenSources {
    moduleInfo = """
        module org.gnome.glib {
            requires static org.jetbrains.annotations;
            exports io.github.jwharm.javagi.annotations;
            exports io.github.jwharm.javagi.base;
            exports io.github.jwharm.javagi.interop;
            exports io.github.jwharm.javagi.pointer;
            exports io.github.jwharm.javagi.util;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gnome.glib", true, "glib-2.0") { repo ->
        // Incompletely defined
        removeFunction(repo, "clear_error")

        // These calls return floating references
        setReturnFloating(findConstructor(repo, "Variant", "new_array"))
        setReturnFloating(findConstructor(repo, "Variant", "new_boolean"))
        setReturnFloating(findConstructor(repo, "Variant", "new_byte"))
        setReturnFloating(findConstructor(repo, "Variant", "new_bytestring"))
        setReturnFloating(findConstructor(repo, "Variant", "new_bytestring_array"))
        setReturnFloating(findConstructor(repo, "Variant", "new_dict_entry"))
        setReturnFloating(findConstructor(repo, "Variant", "new_double"))
        setReturnFloating(findConstructor(repo, "Variant", "new_fixed_array"))
        setReturnFloating(findConstructor(repo, "Variant", "new_from_bytes"))
        setReturnFloating(findConstructor(repo, "Variant", "new_handle"))
        setReturnFloating(findConstructor(repo, "Variant", "new_int16"))
        setReturnFloating(findConstructor(repo, "Variant", "new_int32"))
        setReturnFloating(findConstructor(repo, "Variant", "new_int64"))
        setReturnFloating(findConstructor(repo, "Variant", "new_maybe"))
        setReturnFloating(findConstructor(repo, "Variant", "new_object_path"))
        setReturnFloating(findConstructor(repo, "Variant", "new_objv"))
        setReturnFloating(findConstructor(repo, "Variant", "new_parsed"))
        setReturnFloating(findConstructor(repo, "Variant", "new_parsed_va"))
        setReturnFloating(findConstructor(repo, "Variant", "new_printf"))
        setReturnFloating(findConstructor(repo, "Variant", "new_signature"))
        setReturnFloating(findConstructor(repo, "Variant", "new_string"))
        setReturnFloating(findConstructor(repo, "Variant", "new_strv"))
        setReturnFloating(findConstructor(repo, "Variant", "new_take_string"))
        setReturnFloating(findConstructor(repo, "Variant", "new_tuple"))
        setReturnFloating(findConstructor(repo, "Variant", "new_uint16"))
        setReturnFloating(findConstructor(repo, "Variant", "new_uint32"))
        setReturnFloating(findConstructor(repo, "Variant", "new_uint64"))
        setReturnFloating(findConstructor(repo, "Variant", "new_va"))
        setReturnFloating(findConstructor(repo, "Variant", "new_variant"))

        inject(repo, "Type", """
                        
            public static final long G_TYPE_FUNDAMENTAL_SHIFT = 2;
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
        """.replaceIndent("    ") + "\n")
    }
    source("GObject-2.0", "org.gnome.gobject", true, "gobject-2.0") { repo ->
        // This is an alias for Callback type
        removeType(repo, "VaClosureMarshal")
        removeType(repo, "SignalCVaMarshaller")
        removeFunction(repo, "signal_set_va_marshaller")

        // Override with different return type
        renameMethod(repo, "TypeModule", "use", "use_type_module")

        // Make GWeakRef a generic class
        makeGeneric(repo, "WeakRef");

        // Add a static factory method for GObject
        inject(repo, "Object", """
            
            public static <T extends GObject> T newInstance(org.gnome.glib.Type objectType) {
                var _result = constructNew(objectType, null);
                T _object = (T) InstanceCache.get(_result, true, org.gnome.gobject.GObject::new);
                if (_object != null) {
                    _object.ref();
                }
                return _object;
            }
        """.replaceIndent("    ") + "\n")
        
        fun StringBuilder.template(javatype: String, gtype: String, method: String) = appendLine("""
                            
                    /**
                     * Create a {@link Value} with the provided {@code $javatype} value.
                     * @param  arg the initial value to set
                     * @return the new {@link Value}
                     */
                    public static Value create($javatype arg) {
                        Value v = allocate();
                        v.init($gtype);
                        v.$method;
                        return v;
                    }
                """.trimIndent())

        inject(repo, "Value", StringBuilder().run {
            template("boolean", "org.gnome.glib.Type.G_TYPE_BOOLEAN", "setBoolean(arg)")
            template("byte", "org.gnome.glib.Type.G_TYPE_CHAR", "setSchar(arg)")
            template("double", "org.gnome.glib.Type.G_TYPE_DOUBLE", "setDouble(arg)")
            template("float", "org.gnome.glib.Type.G_TYPE_FLOAT", "setFloat(arg)")
            template("int", "org.gnome.glib.Type.G_TYPE_INT", "setInt(arg)")
            template("long", "org.gnome.glib.Type.G_TYPE_LONG", "setLong(arg)")
            template("String", "org.gnome.glib.Type.G_TYPE_STRING", "setString(arg)")
            template("Enumeration", "org.gnome.glib.Type.G_TYPE_ENUM", "setEnum(arg.getValue())")
            template("Bitfield", "org.gnome.glib.Type.G_TYPE_FLAGS", "setFlags(arg.getValue())")
            template("org.gnome.gobject.GObject", "org.gnome.glib.Type.G_TYPE_OBJECT", "setObject(arg)")
            template("org.gnome.glib.Type", "org.gnome.gobject.GObjects.gtypeGetType()", "setGtype(arg)")
            template("StructProxy", "org.gnome.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) arg.handle())")
            template("MemoryAddress", "org.gnome.glib.Type.G_TYPE_POINTER", "setPointer(arg)")
            template("ParamSpec", "org.gnome.glib.Type.G_TYPE_PARAM", "setParam(arg)")
            template("Proxy", "org.gnome.glib.Type.G_TYPE_OBJECT", "setObject((org.gnome.gobject.GObject) arg)")
            template("byte[]", "org.gnome.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) Interop.allocateNativeArray(arg, true, MemorySession.openImplicit()))")
            template("String[]", "org.gnome.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) Interop.allocateNativeArray(arg, true, MemorySession.openImplicit()))")
            template("MemoryAddress[]", "org.gnome.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) Interop.allocateNativeArray(arg, true, MemorySession.openImplicit()))")
        }.toString().replaceIndent("    ") + "\n")
    }
    source("Gio-2.0", "org.gnome.gio", true, "gio-2.0") { repo ->
        // Override with different return type
        renameMethod(repo, "BufferedInputStream", "read_byte", "read_int")
        renameMethod(repo, "IOModule", "load", "load_module")
        
        setReturnType(repo, "ActionGroup", "activate_action", "gboolean", "gboolean", "true", "always %TRUE")
        
        // Override of static method
        removeVirtualMethod(repo, "SocketControlMessage", "get_type")

        // g_async_initable_new_finish is a method declaration in the interface AsyncInitable.
        // It is meant to be implemented as a constructor (actually, a static factory method).
        // However, Java does not allow a (non-static) method to be implemented/overridden by a static method.
        // The current solution is to remove the method from the interface. It is still available in the implementing classes.
        removeMethod(repo, "AsyncInitable", "new_finish")

        // Let these classes implement the AutoCloseable interface, so they can be used in try-with-resources blocks.
        makeAutoCloseable(repo, "IOStream")
        makeAutoCloseable(repo, "InputStream")
        makeAutoCloseable(repo, "OutputStream")
    }
    source("GModule-2.0", "org.gnome.gmodule", true)
}
