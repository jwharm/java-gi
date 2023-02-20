package io.github.jwharm.javagi.gtk.util;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.util.JavaClosure;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;
import org.gnome.gtk.*;

import java.lang.foreign.Addressable;
import java.lang.reflect.Method;

/**
 * The {@code BuilderJavaScope} class can be used with a {@link GtkBuilder} to
 * refer to Java instance methods from a ui file.
 * <p>
 * When a ui file contains, for example, the following element:
 * <p>
 * {@code <signal name="clicked" handler="okButtonClicked"/>}
 * <p>
 * the Java instance method {@code okButtonClicked()} will be called on
 * the widget that is being built with the {@link GtkBuilder}.
 */
public final class BuilderJavaScope extends GObject implements BuilderScope {

    private static final String LOG_DOMAIN = "java-gi";
    private static Type type;

    static {
        Gtk.javagi$ensureInitialized();
    }

    /**
     * Memory address constructor for instantiating a Java proxy object
     * @param address the memory address of the native object
     */
    public BuilderJavaScope(Addressable address) {
        super(address);
    }

    /**
     * Get the gtype of {@link BuilderJavaScope}, or register it as a new gtype
     * if it was not registered yet.
     * @return the {@link Type} that has been registered for {@link BuilderJavaScope}
     */
    public static Type getType() {
        if (type == null) {
            // Register the new gtype
            type = Types.register(BuilderJavaScope.class);

            // Implement the BuilderScope interface
            InterfaceInfo interfaceInfo = InterfaceInfo.allocate();
            interfaceInfo.writeInterfaceInit((iface, data) -> {
                BuilderScopeInterface bsi = new BuilderScopeInterface(iface.handle());
                bsi.overrideCreateClosure((self, builder, functionName, flags, object) -> {
                    try {
                        return self.createClosure(builder, functionName, flags, object);
                    } catch (GErrorException gerror) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Error while creating closure: %s\n", gerror.getMessage());
                        return null;
                    }
                });
                bsi.overrideGetTypeFromName(BuilderScope::getTypeFromName);
                bsi.overrideGetTypeFromFunction(BuilderScope::getTypeFromFunction);
            });
            GObjects.typeAddInterfaceStatic(type, BuilderScope.getType(), interfaceInfo);
        }
        return type;
    }

    /**
     * Instantiates a new {@link BuilderScope}
     */
    public BuilderJavaScope() {
        super(getType(), null);
    }

    /**
     * Called by a GtkBuilder to create a {@link Closure} from the name that was specified in
     * an attribute of a UI file. The {@code functionName} should refer to a method in the
     * Java class (a {@link Buildable} instance). If that fails, as a fallback mechanism the
     * {@link BuilderCScope#createClosure(GtkBuilder, String, BuilderClosureFlags, GObject)} is
     * called and the result of that function is returned.
     * @param builder the GtkBuilder instance
     * @param functionName the function name for which a {@link Closure} will be returned
     * @param flags options for creating the closure
     * @param object unused
     * @return a new {@link JavaClosure} instance for the requested {@code functionName}
     * @throws GErrorException when an error occurs
     */
    @Override
    public Closure createClosure(GtkBuilder builder, String functionName, BuilderClosureFlags flags, GObject object) throws GErrorException {
        // Get the instance object
        GObject currentObject = builder.getCurrentObject();
        if (currentObject == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot create closure for handler %s: Current object not set\n", functionName);
            return new BuilderCScope().createClosure(builder, functionName, flags, object);
        }

        try {
            Method method = currentObject.getClass().getDeclaredMethod(functionName);
            // Signal that returns boolean
            if (method.getReturnType().equals(Boolean.TYPE)) {
                return new JavaClosure(() -> {
                    try {
                        return (boolean) method.invoke(currentObject);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Cannot invoke method %s in class %s: %s\n",
                                functionName, currentObject.getClass().getName(), e.getMessage());
                        return false;
                    }
                });
            // Signal that returns void
            } else {
                return new JavaClosure(() -> {
                    try {
                        method.invoke(currentObject);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Cannot invoke method %s in class %s: %s\n",
                                functionName, currentObject.getClass().getName(), e.getMessage());
                    }
                });
            }
        } catch (NoSuchMethodException e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find method %s in class %s\n",
                    functionName, currentObject.getClass().getName());
            return new BuilderCScope().createClosure(builder, functionName, flags, object);
        }
    }

    /**
     * See {@link BuilderCScope#getTypeFromFunction(GtkBuilder, String)}
     * @param builder the GtkBuilder instance
     * @param functionName the name of the function that will return a GType
     * @return the GType returned by {@code functionName}
     */
    @Override
    public Type getTypeFromFunction(GtkBuilder builder, String functionName) {
        return new BuilderCScope().getTypeFromFunction(builder, functionName);
    }

    /**
     * See {@link BuilderCScope#getTypeFromName(GtkBuilder, String)}
     * @param builder the GtkBuilder instance
     * @param typeName the name of the GType
     * @return the requested GType
     */
    @Override
    public Type getTypeFromName(GtkBuilder builder, String typeName) {
        return new BuilderCScope().getTypeFromName(builder, typeName);
    }
}
