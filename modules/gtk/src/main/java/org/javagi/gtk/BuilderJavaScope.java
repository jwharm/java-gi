/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package org.javagi.gtk;

import org.javagi.base.GErrorException;
import org.javagi.gtk.annotations.GtkCallback;
import org.javagi.gobject.JavaClosure;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;
import org.gnome.gtk.*;

import java.lang.reflect.Method;
import java.util.Set;

import static org.javagi.base.Constants.LOG_DOMAIN;

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
public final class BuilderJavaScope extends BuilderCScope
        implements BuilderScope {

    static {
        Gtk.javagi$ensureInitialized();
    }

    /**
     * Default constructor
     */
    public BuilderJavaScope() {
        super();
    }

    /**
     * Called by a GtkBuilder to create a {@link Closure} from the name that
     * was specified in an attribute of a UI file. The {@code function}
     * should refer to a method in the Java class (a {@link Buildable}
     * instance). If that fails, as a fallback mechanism the
     * {@link BuilderCScope#createClosure(GtkBuilder, String, Set, GObject)}
     * is called and the result of that function is returned.
     *
     * @param  builder  the GtkBuilder instance
     * @param  function the function name for which a {@link Closure} will be
     *                  returned
     * @param  flags    options for creating the closure
     * @param  object   unused
     * @return a new {@link JavaClosure} instance for the requested
     *         {@code function}
     * @throws GErrorException when an error occurs
     */
    @Override
    public Closure createClosure(GtkBuilder builder,
                                 String function,
                                 Set<BuilderClosureFlags> flags,
                                 GObject object) throws GErrorException {

        // Get the instance object
        GObject currentObject = builder.getCurrentObject();
        if (currentObject == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot create closure for handler %s: Current object not set\n",
                    function);
            return asParent().createClosure(builder, function, flags, object);
        }

        try {
            // Find method with the right name
            Class<? extends GObject> cls = currentObject.getClass();
            Method method = getMethodForName(cls, function);
            return new JavaClosure(currentObject, method).ignoreFirstParameter();
        } catch (NoSuchMethodException e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find method %s in class %s\n",
                    function, currentObject.getClass().getName());
            return asParent().createClosure(builder, function, flags, object);
        }
    }

    /**
     * Return a method with annotation {@code "@GtkCallback name=functionName"},
     * or else a method with the exact name specified with {@code functionName}.
     *
     * @param  cls          the class to search in
     * @param  functionName the name to search for
     * @return the method (if found)
     * @throws NoSuchMethodException when the method cannot be found
     */
    private Method getMethodForName(Class<?> cls, String functionName)
            throws NoSuchMethodException {

        // Find method with GtkCallback annotation
        for (Method m : cls.getDeclaredMethods()) {
            if (m.isAnnotationPresent(GtkCallback.class)) {
                String name = m.getAnnotation(GtkCallback.class).name();
                if (functionName.equals(name))
                    return m;
            }
        }
        // Find method using reflection
        return cls.getDeclaredMethod(functionName);
    }

    /**
     * See {@link BuilderCScope#getTypeFromFunction(GtkBuilder, String)}
     *
     * @param builder      the GtkBuilder instance
     * @param functionName the name of the function that will return a GType
     * @return the GType returned by {@code functionName}
     */
    @Override
    public Type getTypeFromFunction(GtkBuilder builder, String functionName) {
        return asParent().getTypeFromFunction(builder, functionName);
    }

    /**
     * See {@link BuilderCScope#getTypeFromName(GtkBuilder, String)}
     *
     * @param builder  the GtkBuilder instance
     * @param typeName the name of the GType
     * @return the requested GType
     */
    @Override
    public Type getTypeFromName(GtkBuilder builder, String typeName) {
        return asParent().getTypeFromName(builder, typeName);
    }
}
