package io.github.jwharm.javagi.gtk.util;

import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.gtk.annotations.GtkChild;
import io.github.jwharm.javagi.gtk.annotations.GtkTemplate;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.TypeClass;
import org.gnome.gobject.TypeFlags;
import org.gnome.gtk.Widget;

import java.lang.foreign.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.jwharm.javagi.util.Types.*;

public class Types {

    private static final String LOG_DOMAIN = "java-gi";

    public static String getTemplateName(Class<?> cls) {
        var annotation = cls.getAnnotation(GtkTemplate.class);
        String name = annotation.name();
        if (! "".equals(name)) {
            return name;
        }

        return getName(cls);
    }

    /**
     * Generate a memory layout for an instance struct
     * @param cls the class from which the fields will be used to generate the memory layout
     * @param typeName the name of the struct
     * @return the generated memory layout
     */
    private static MemoryLayout getTemplateInstanceLayout(Class<?> cls, String typeName) {
        MemoryLayout parentLayout = getLayout(cls.getSuperclass());
        if (parentLayout == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find memory layout of class %s\n",
                    cls.getSimpleName());
            return null;
        }

        ArrayList<MemoryLayout> elements = new ArrayList<>();
        long size = add(parentLayout.withName("parent_instance"), elements, 0);

        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(GtkChild.class)) {
                // Determine the name of the struct field
                String fieldName = field.getAnnotation(GtkChild.class).name();
                if ("".equals(fieldName)) {
                    fieldName = field.getName();
                }

                if (field.getType().equals(boolean.class)) {
                    size = add(Interop.valueLayout.C_BOOLEAN.withName(fieldName), elements, size);
                } else if (field.getType().equals(byte.class)) {
                    size = add(Interop.valueLayout.C_BYTE.withName(fieldName), elements, size);
                } else if (field.getType().equals(char.class)) {
                    size = add(Interop.valueLayout.C_CHAR.withName(fieldName), elements, size);
                } else if (field.getType().equals(double.class)) {
                    size = add(Interop.valueLayout.C_DOUBLE.withName(fieldName), elements, size);
                } else if (field.getType().equals(float.class)) {
                    size = add(Interop.valueLayout.C_FLOAT.withName(fieldName), elements, size);
                } else if (field.getType().equals(int.class)) {
                    size = add(Interop.valueLayout.C_INT.withName(fieldName), elements, size);
                } else if (field.getType().equals(long.class)) {
                    size = add(Interop.valueLayout.C_LONG.withName(fieldName), elements, size);
                } else if (field.getType().equals(short.class)) {
                    size = add(Interop.valueLayout.C_SHORT.withName(fieldName), elements, size);
                } else if (Proxy.class.isAssignableFrom(field.getType())) {
                    size = add(Interop.valueLayout.ADDRESS.withName(fieldName), elements, size);
                } else {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Unsupported type '%s' of field %s\n",
                            field.getType().getSimpleName(), fieldName);
                }
            }
        }

        MemoryLayout[] layouts = elements.toArray(new MemoryLayout[0]);
        return MemoryLayout.structLayout(layouts).withName(typeName);
    }

    /**
     * Add a memory layout to the list, with padding if necessary
     * @param layout the layout to add
     * @param elements the list of layouts so far, to which the layout will be added
     * @param oldSize the total length of the layouts so far
     * @return the new length of the layouts
     */
    private static long add(MemoryLayout layout, ArrayList<MemoryLayout> elements, long oldSize) {
        long size = oldSize;
        long s = layout.byteSize();
        if (size % s % 64 > 0) {
            long padding = (s - (size % s)) % 64;
            elements.add(MemoryLayout.paddingLayout(padding));
            size += padding;
        }
        elements.add(layout);
        return size + s;
    }

    private static <T extends Widget> Consumer<TypeClass> getTemplateClassInit(Class<T> cls, MemoryLayout layout) {
        var annotation = cls.getAnnotation(GtkTemplate.class);
        String ui = annotation.ui();

        return (typeClass) -> {
            Widget.WidgetClass widgetClass = new Widget.WidgetClass(typeClass.handle());

            // The ui parameter must refer to a registered GResource
            widgetClass.setTemplateFromResource(ui);

            widgetClass.overrideDispose((object) -> {
                ((Widget) object).disposeTemplate(typeClass.readGType());
                object.dispose(); // This should call the parent class dispose
            });

            // Install BuilderJavaScope to call Java signal handler methods
            widgetClass.setTemplateScope(new BuilderJavaScope());

            for (Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(GtkChild.class)) {
                    String name = field.getName();
                    long offset = layout.byteOffset(MemoryLayout.PathElement.groupElement(name));
                    widgetClass.bindTemplateChildFull(name, false, offset);
                }
            }
        };
    }

    private static <T extends Widget> Consumer<T> getTemplateInstanceInit(Class<T> cls) {
        return (widget) -> {
            widget.initTemplate();

            for (Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(GtkChild.class)) {
                    GObject child = widget.getTemplateChild(widget.readGClass().readGType(), field.getName());
                    try {
                        field.set(widget, child);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Cannot get template child %s in class %s: %s\n",
                                field.getName(), cls.getName(), e.getMessage());
                    }
                }
            }
        };
    }

    public static <T extends Widget> Type registerTemplate(Class<T> cls) {
        try {
            String typeName = getTemplateName(cls);
            MemoryLayout instanceLayout = getTemplateInstanceLayout(cls, typeName);
            Class<?> parentClass = cls.getSuperclass();
            Type parentType = getGType(parentClass);
            MemoryLayout classLayout = getClassLayout(cls, typeName);
            Function<Addressable, T> constructor = getAddressConstructor(cls);
            TypeFlags flags = getTypeFlags(cls);

            // Chain template class init with user-defined class init function
            Consumer<TypeClass> classInit = getTemplateClassInit(cls, instanceLayout);
            Consumer<TypeClass> userDefinedClassInit = getClassInit(cls);
            if (userDefinedClassInit != null)
                classInit = classInit.andThen(userDefinedClassInit);

            // Chain template instance init with user-defined init function
            Consumer<T> instanceInit = getTemplateInstanceInit(cls);
            Consumer<T> userDefinedInit = getInstanceInit(cls);
            if (userDefinedInit != null)
                instanceInit = instanceInit.andThen(userDefinedInit);

            // Register and return the GType
            return register(
                    parentType,
                    typeName,
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

    public static <T extends GObject> Type register(Class<T> cls) {
        return io.github.jwharm.javagi.util.Types.register(cls);
    }

    public static <T extends GObject> Type register(
            org.gnome.glib.Type parentType,
            String typeName,
            MemoryLayout classLayout,
            Consumer<TypeClass> classInit,
            MemoryLayout instanceLayout,
            Consumer<T> instanceInit,
            Function<Addressable, T> constructor,
            TypeFlags flags
    ) {
        return io.github.jwharm.javagi.util.Types.register(
                parentType, typeName, classLayout, classInit, instanceLayout, instanceInit, constructor, flags);
    }
}
