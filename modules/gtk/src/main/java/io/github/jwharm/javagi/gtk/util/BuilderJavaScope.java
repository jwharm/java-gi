/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.gtk.util;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.gtk.annotations.GtkCallback;
import io.github.jwharm.javagi.gobject.JavaClosure;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;
import org.gnome.gtk.*;
import io.github.jwharm.javagi.gtk.types.Types;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;

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

    private static final Type gtype = Types.register(BuilderJavaScope.class);

    static {
        Gtk.javagi$ensureInitialized();
    }

    /**
     * Memory address constructor for instantiating a Java proxy object
     * @param address the memory address of the native object
     */
    public BuilderJavaScope(MemorySegment address) {
        super(address);
    }

    /**
     * Return the GType of {@link BuilderJavaScope}
     * @return the GType
     */
    public static Type getType() {
        return gtype;
    }

    /**
     * Instantiates a new {@link BuilderScope}
     */
    public BuilderJavaScope() {
        super(gtype, null);
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
            // Find method with the right name
            Method method = getMethodForName(currentObject.getClass(), functionName);

            // Signal that returns boolean
            if (method.getReturnType().equals(Boolean.TYPE)) {
                return new JavaClosure((BooleanSupplier) () -> {
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
                return new JavaClosure((Runnable) () -> {
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
     * Return a method with annotation "@GtkCallback name=functionName", or else a method
     * with the exact name "functionName()".
     * @param cls The class to search in
     * @param functionName the name to search for
     * @return the method (if found)
     * @throws NoSuchMethodException when the method cannot be found
     */
    private Method getMethodForName(Class<?> cls, String functionName) throws NoSuchMethodException {
        // Find method with GtkCallback annotation
        for (Method m : cls.getDeclaredMethods()) {
            if (m.isAnnotationPresent(GtkCallback.class)) {
                if (functionName.equals(m.getAnnotation(GtkCallback.class).name())) {
                    return m;
                }
            }
        }
        // Find method using reflection
        return cls.getDeclaredMethod(functionName);
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
