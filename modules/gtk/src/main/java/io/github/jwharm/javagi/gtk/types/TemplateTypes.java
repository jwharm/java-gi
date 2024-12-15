/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.gtk.types;

import io.github.jwharm.javagi.base.FunctionPointer;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.gobject.InstanceCache;
import io.github.jwharm.javagi.gobject.annotations.Namespace;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gobject.types.*;
import io.github.jwharm.javagi.gtk.annotations.GtkChild;
import io.github.jwharm.javagi.gtk.annotations.GtkTemplate;
import io.github.jwharm.javagi.gtk.util.BuilderJavaScope;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.TypeClass;
import org.gnome.gobject.TypeFlags;
import org.gnome.gobject.TypeInstance;
import org.gnome.gtk.Widget;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;
import static io.github.jwharm.javagi.gobject.types.Types.*;
import static java.util.Objects.requireNonNull;

/**
 * This class contains functionality to register a Java class as a Gtk
 * composite template class.
 * <p>
 * To register a Java class as a "regular" GObject class, see 
 * {@link io.github.jwharm.javagi.gobject.types.Types#register(Class)}
 */
public class TemplateTypes {

    /**
     * Get the {@code name} parameter of the {@code GtkTemplate} annotation, or
     * if it is not defined, fallback to the {@code name} parameter of the
     * {@code RegisteredType} annotation, or if that is also not defined, the
     * package and class name will be used as the new GType name (with all
     * characters except a-z and A-Z converted to underscores).
     *
     * @param  cls the class that is registered as a new GType
     * @return the name
     */
    private static String getName(Class<?> cls) {
        // Default type name: fully qualified Java class name
        String typeNameInput = cls.getName();
        String namespace = "";

        // Check for a Namespace annotation on the package
        if (cls.getPackage().isAnnotationPresent(Namespace.class)) {
            var annotation = cls.getPackage().getAnnotation(Namespace.class);
            namespace = annotation.name();
            typeNameInput = namespace + cls.getSimpleName();
        }

        // Check if the GtkTemplate annotation overrides the type name
        var gtkTemplate = cls.getAnnotation(GtkTemplate.class);
        if (! "".equals(gtkTemplate.name())) {
            typeNameInput = namespace + gtkTemplate.name();
        }

        // Check for a RegisteredType annotation that overrides the name
        else if (cls.isAnnotationPresent(RegisteredType.class)) {
            var registeredType = cls.getAnnotation(RegisteredType.class);
            if (! "".equals(registeredType.name())) {
                typeNameInput = namespace + registeredType.name();
            }
        }

        // Replace all characters except a-z or A-Z with underscores
        return typeNameInput.replaceAll("[^a-zA-Z]", "_");
    }

    /**
     * Get the {@code name} parameter of the {@code GtkChild} annotation, or
     * if it is not defined, fallback to the name of the field.
     *
     * @param  field a GtkChild-annotated field
     * @return the name
     */
    private static String getChildName(Field field) {
        if (!field.isAnnotationPresent(GtkChild.class))
            throw new IllegalArgumentException();
        String name = field.getAnnotation(GtkChild.class).name();
        return "".equals(name) ? field.getName() : name;
    }

    /**
     * Generate a memory layout for an instance struct.
     * @param  cls      the class from which the fields will be used to
     *                  generate the memory layout
     * @param  typeName the name of the struct
     * @return the generated memory layout
     */
    private static MemoryLayout getTemplateInstanceLayout(Class<?> cls,
                                                          String typeName) {

        MemoryLayout parentLayout = getLayout(cls.getSuperclass());
        requireNonNull(parentLayout,
                "No memory layout for class " + cls.getSimpleName());

        ArrayList<MemoryLayout> elements = new ArrayList<>();
        long size = add(parentLayout.withName("parent_instance"), elements, 0);

        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(GtkChild.class)) {

                // Determine the name of the struct field.
                String fieldName = getChildName(field);

                // Add the memory layout of the field to the struct.
                if (GObject.class.isAssignableFrom(field.getType()))
                    size = add(ValueLayout.ADDRESS.withName(fieldName),
                               elements, size);
                else
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_ERROR,
                            "GtkChild field %s of type '%s' is not derived from GObject\n",
                            fieldName, field.getType().getSimpleName());
            }
        }

        MemoryLayout[] layouts = elements.toArray(new MemoryLayout[0]);
        return MemoryLayout.structLayout(layouts).withName(typeName);
    }

    /**
     * Add a memory layout to the list, with padding if necessary.
     *
     * @param  layout   the layout to add
     * @param  elements the list of layouts so far, to which the layout will be
     *                  added
     * @param  oldSize  the total length of the layouts so far
     * @return the new length of the layouts
     */
    private static long add(MemoryLayout layout,
                            ArrayList<MemoryLayout> elements,
                            long oldSize) {
        long size = oldSize;
        long s = layout.byteSize();
        if (size % s % 8 > 0) {
            long padding = (s - (size % s)) % 8; // in bytes (since JDK 21)
            elements.add(MemoryLayout.paddingLayout(padding));
            size += padding;
        }
        elements.add(layout);
        return size + s;
    }

    /**
     * Return a lambda that will:
     * <ul>
     *   <li>load the ui file, and set it as template,
     *   <li>override the dispose() method to dispose the template,
     *   <li>install a JavaBuilderScope for signal handling,
     *   <li>bind @GtkChild-annotated fields to the template.
     * </ul>
     * The lambda will be run during class initialization.
     */
    private static <T extends Widget>
    Consumer<GObject.ObjectClass> getTemplateClassInit(Class<T> cls,
                                                       MemoryLayout layout) {

        var annotation = cls.getAnnotation(GtkTemplate.class);
        String ui = annotation.ui();

        return (typeClass) -> {
            var widgetClass = new Widget.WidgetClass(typeClass.handle());

            // The ui parameter must refer to a registered GResource
            widgetClass.setTemplateFromResource(ui);

            // Override GObject.dispose() to dispose the template
            overrideDispose(widgetClass, (object) -> {
                ((Widget) object).disposeTemplate(typeClass.readGType());

                /*
                 * Chain up to the parent (GObject) dispose function. The Java
                 * binding is a protected method, so we call the C function
                 * directly.
                 */
                try {
                    var parent = GObject.ObjectClass.getMemoryLayout();
                    var func = Overrides.lookupVirtualMethodParent(
                                    object.handle(), parent, "dispose");
                    var desc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
                    var downcall = Interop.downcallHandle(func, desc);
                    downcall.invokeExact(object.handle());
                } catch (Throwable _err) {
                    throw new AssertionError("Unexpected exception occurred: ", _err);
                }
            }, Arena.global());

            // Install BuilderJavaScope to call Java signal handler methods
            widgetClass.setTemplateScope(BuilderJavaScope.newInstance());

            for (Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(GtkChild.class)) {
                    var name = getChildName(field);
                    var path = MemoryLayout.PathElement.groupElement(name);
                    var offset = layout.byteOffset(path);
                    widgetClass.bindTemplateChildFull(name, false, offset);
                }
            }
        };
    }

    /**
     * Return a lambda that will:
     * <ul>
     *   <li>call gtk_widget_init_template
     *   <li>for all @GtkChild-annotated fields, get the template child object,
     *       and assign it to the field.
     * </ul>
     * The lambda will be run during instance initialization.
     */
    private static <T extends Widget>
    Consumer<T> getTemplateInstanceInit(Class<T> cls) {

        return (widget) -> {
            widget.initTemplate();
            for (Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(GtkChild.class)) {
                    try {
                        setField(field, widget);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Cannot get template child %s in class %s: %s\n",
                                field.getName(), cls.getName(), e.getMessage());
                    }
                }
            }
        };
    }

    /**
     * Assign the widget from the template to the field
     */
    private static void setField(Field field, Widget widget) throws Exception {
        Type gtype = widget.readGClass().readGType();
        String name = getChildName(field);
        GObject child = widget.getTemplateChild(gtype, name);
        field.set(widget, child);
    }

    /**
     * Register a class as a Gtk composite template class.
     * <p>
     * To register a Java class as a "regular" GObject class, see
     * {@link io.github.jwharm.javagi.gobject.types.Types#register(Class)}
     * @param  cls a @GtkTemplate-annotated class
     * @param  <W> the class must extend GtkWidget
     * @return the new GType that has been registered
     */
    private static <W extends Widget> Type registerTemplate(Class<W> cls) {
        try {
            String name = getName(cls);
            MemoryLayout instanceLayout = getTemplateInstanceLayout(cls, name);
            Class<?> parentClass = cls.getSuperclass();
            Type parentType = TypeCache.getType(parentClass);
            MemoryLayout classLayout = generateClassLayout(cls, name);
            Function<MemorySegment, W> constructor = getAddressConstructor(cls);
            Set<TypeFlags> flags = getTypeFlags(cls);

            // Chain template class init with user-defined class init function
            var overridesInit = Overrides.overrideClassMethods(cls);
            var propertiesInit = new Properties().installProperties(cls);
            var signalsInit = Signals.installSignals(cls);
            var templateClassInit = getTemplateClassInit(cls, instanceLayout);
            var userDefinedClassInit = getClassInit(cls);

            // Override virtual methods, install properties and signals, and
            // then install the template before running a user-defined class
            // init.
            Consumer<TypeClass> classInit = typeClass -> {
                var widgetClass = (Widget.WidgetClass) typeClass;
                applyIfNotNull(overridesInit, widgetClass);
                applyIfNotNull(propertiesInit, widgetClass);
                applyIfNotNull(signalsInit, widgetClass);
                templateClassInit.accept(widgetClass);
                applyIfNotNull(userDefinedClassInit, widgetClass);
            };

            // Chain template instance init with user-defined init function
            Consumer<W> templateInit = getTemplateInstanceInit(cls);
            Consumer<W> userDefinedInit = getInstanceInit(cls);
            Consumer<TypeInstance> instanceInit = typeInstance -> {
                @SuppressWarnings("unchecked") // Class will always be a Widget
                var widget = (W) typeInstance;
                templateInit.accept(widget);
                applyIfNotNull(userDefinedInit, widget);
            };

            // Register and return the GType
            return Types.register(
                    parentType,
                    cls,
                    name,
                    classLayout,
                    classInit,
                    instanceLayout,
                    instanceInit,
                    constructor,
                    flags
            );

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot register type %s: %s\n",
                    cls == null ? "null" : cls.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * This will call {@link TemplateTypes#registerTemplate(Class)} when
     * {@code cls} is a {@code Widget.class} with {@link GtkTemplate}
     * annotation, and {@link Types#register(Class)} for all other
     * (GObject-derived) classes.
     *
     * @param  cls the class to register as a new GType
     * @param  <T> the class must extend {@link GObject}
     * @return the new GType
     */
    @SuppressWarnings("unchecked")
    public static <T extends GObject, W extends Widget>
    Type register(Class<T> cls) {
        if (Widget.class.isAssignableFrom(cls)
                && cls.isAnnotationPresent(GtkTemplate.class)) {
            return registerTemplate((Class<W>) cls);
        } else {
            return io.github.jwharm.javagi.gobject.types.Types.register(cls);
        }
    }

    private static void overrideDispose(Proxy instance, DisposeCallback dispose, Arena _arena) {
        GObject.ObjectClass.getMemoryLayout().varHandle(MemoryLayout.PathElement.groupElement("dispose"))
                .set(instance.handle(), 0, (dispose == null ? MemorySegment.NULL : dispose.toCallback(_arena)));
    }

    @FunctionalInterface
    private interface DisposeCallback extends FunctionPointer {
        void run(GObject object);

        @SuppressWarnings("unused") // called from foreign function
        default void upcall(MemorySegment object) {
            run((GObject) InstanceCache.getForType(object, GObject::new, false));
        }

        default MemorySegment toCallback(Arena arena) {
            FunctionDescriptor _fdesc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
            MethodHandle _handle = Interop.upcallHandle(MethodHandles.lookup(), DisposeCallback.class, _fdesc);
            return Linker.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, arena);
        }
    }
}
