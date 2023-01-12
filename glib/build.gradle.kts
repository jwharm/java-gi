import ext.*
import io.github.jwharm.javagi.generator.PatchSet.*

plugins {
    id("java-gi.library-conventions")
}

setupGenSources {
    moduleInfo = """
        module org.glib {
            requires static org.jetbrains.annotations;
            exports io.github.jwharm.javagi;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gtk.glib", true, "glib-2.0") { repo ->
        // This method has parameters that jextract does not support
        removeFunction(repo, "assertion_message_cmpnum")
        // Incompletely defined
        removeFunction(repo, "clear_error")
        // Old typo
        removeEnumMember(repo, "UnicodeBreakType", "close_paranthesis")

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
    source("GObject-2.0", "org.gtk.gobject", true, "gobject-2.0") { repo ->
        // This is an alias for Callback type
        removeType(repo, "VaClosureMarshal")
        removeType(repo, "SignalCVaMarshaller")
        removeFunction(repo, "signal_set_va_marshaller")
        // Override with different return type
        renameMethod(repo, "TypeModule", "use", "use_type_module")
        // These functions have two Callback parameters, this isn't supported yet
        removeFunction(repo, "signal_new_valist")
        removeFunction(repo, "signal_newv")
        removeFunction(repo, "signal_new")
        removeFunction(repo, "signal_new_class_handler")

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
            template("boolean", "org.gtk.glib.Type.G_TYPE_BOOLEAN", "setBoolean(arg)")
            template("byte", "org.gtk.glib.Type.G_TYPE_CHAR", "setSchar(arg)")
            template("double", "org.gtk.glib.Type.G_TYPE_DOUBLE", "setDouble(arg)")
            template("float", "org.gtk.glib.Type.G_TYPE_FLOAT", "setFloat(arg)")
            template("int", "org.gtk.glib.Type.G_TYPE_INT", "setInt(arg)")
            template("long", "org.gtk.glib.Type.G_TYPE_LONG", "setLong(arg)")
            template("String", "org.gtk.glib.Type.G_TYPE_STRING", "setString(arg)")
            template("Enumeration", "org.gtk.glib.Type.G_TYPE_ENUM", "setEnum(arg.getValue())")
            template("Bitfield", "org.gtk.glib.Type.G_TYPE_FLAGS", "setFlags(arg.getValue())")
            template("org.gtk.gobject.GObject", "org.gtk.glib.Type.G_TYPE_OBJECT", "setObject(arg)")
            template("org.gtk.glib.Type", "org.gtk.gobject.GObjects.gtypeGetType()", "setGtype(arg)")
            template("Struct", "org.gtk.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) arg.handle())")
            template("MemoryAddress", "org.gtk.glib.Type.G_TYPE_POINTER", "setPointer(arg)")
            template("ParamSpec", "org.gtk.glib.Type.G_TYPE_PARAM", "setParam(arg)")
            template("Proxy", "org.gtk.glib.Type.G_TYPE_OBJECT", "setObject((org.gtk.gobject.GObject) arg)")
            template("byte[]", "org.gtk.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) Interop.allocateNativeArray(arg, true, MemorySession.openImplicit()))")
            template("String[]", "org.gtk.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) Interop.allocateNativeArray(arg, true, MemorySession.openImplicit()))")
            template("MemoryAddress[]", "org.gtk.glib.Type.G_TYPE_BOXED", "setBoxed((MemoryAddress) Interop.allocateNativeArray(arg, true, MemorySession.openImplicit()))")
        }.toString().replaceIndent("    ") + "\n")
    }
    source("Gio-2.0", "org.gtk.gio", true, "gio-2.0") { repo ->
        // Override with different return type
        renameMethod(repo, "BufferedInputStream", "read_byte", "read_int")
        // g_async_initable_new_finish is a method declaration in the interface AsyncInitable.
        // It is meant to be implemented as a constructor (actually, a static factory method).
        // However, Java does not allow a (non-static) method to be implemented/overridden by a static method.
        // The current solution is to remove the method from the interface. It is still available in the implementing classes.
        removeMethod(repo, "AsyncInitable", "new_finish")}
    source("GModule-2.0", "org.gtk.gmodule", true)
}
