package io.github.jwharm.javagi.gtk.util;

import io.github.jwharm.javagi.base.ObjectProxy;
import io.github.jwharm.javagi.base.Out;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.gio.File;
import org.gnome.glib.Bytes;
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

    private static <T extends Widget> MemoryLayout getTemplateInstanceLayout(Class<T> cls, String typeName) {
        MemoryLayout parentLayout = getLayout(cls.getSuperclass());

        ArrayList<MemoryLayout> elements = new ArrayList<>();
        elements.add(parentLayout.withName("parent_instance"));

        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(GtkChild.class)) {
                if (field.getClass().isPrimitive()) {
                    System.out.println("Not supported yet");
                }
                elements.add(Interop.valueLayout.ADDRESS.withName(field.getName()));
            }
        }

        MemoryLayout[] layouts = elements.toArray(new MemoryLayout[0]);
        return MemoryLayout.structLayout(layouts).withName(typeName);
    }

    private static <T extends Widget> Consumer<TypeClass> getTemplateClassInit(Class<T> cls, MemoryLayout layout) {
        var annotation = cls.getAnnotation(GtkTemplate.class);
        String ui = annotation.ui();

        return (typeClass) -> {
            Widget.WidgetClass widgetClass = new Widget.WidgetClass(typeClass.handle());

            Out<byte[]> bytes = new Out<>();
            File file = File.newForPath(ui);
            try {
                file.loadContents(null, bytes, null);
            } catch (Exception gerror) {
                throw new RuntimeException(gerror);
            }

            widgetClass.setTemplate(new Bytes(bytes.get()));

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
                                "Cannot get template child %s in class %s: %s",
                                field.getName(), cls.getName(), e.getMessage());
                    }
                }
            }
        };
    }

    public static <T extends Widget> Type registerTemplate(Class<T> cls) {
        try {
            String typeName = getName(cls);
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
                    "Cannot register type %s: %s",
                    cls == null ? "null" : cls.getName(), e.getMessage());
            return null;
        }
    }

    public static <T extends GObject> Type register(Class<T> cls) {
        return io.github.jwharm.javagi.util.Types.register(cls);
    }

    public static <T extends ObjectProxy> Type register(
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
