package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.gir.Type;
import io.github.jwharm.javagi.gir.VirtualMethod;
import io.github.jwharm.javagi.util.Patch;

import java.util.Collections;
import java.util.Map;

public class GtkPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"Gtk".equals(namespace))
            return element;

        /*
         * ApplicationWindow.getId() overrides Buildable.getId() with a
         * different return type. Rename to getWindowId()
         */
        if (element instanceof Method m
                && "gtk_application_window_get_id".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "get_window_id");

        /*
         * MenuButton.getDirection() overrides Widget.getDirection() with a
         * different return type. Rename to getArrowDirection()
         */
        if (element instanceof Method m
                && "gtk_menu_button_get_direction".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "get_arrow_direction");

        /*
         * PrintSettings.get() overrides GObject.get() with a different return
         * type. Rename to getString()
         */
        if (element instanceof Method m
                && "gtk_print_settings_get".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "get_string");

        /*
         * PrintUnixDialog.getSettings() overrides Widget.getSettings() with a
         * different return type. Rename to getPrintSettings()
         */
        if (element instanceof Method m
                && "gtk_print_unix_dialog_get_settings".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "get_print_settings");

        /*
         * The invoker attribute isn't set automatically in the gir file,
         * because the return values are not the same. Set the attribute
         * anyway.
         */
        if (element instanceof VirtualMethod vm
                && "play".equals(vm.name())
                && "MediaStream".equals(vm.parameters().instanceParameter().anyType().name()))
            return vm.withAttribute("invoker", "play");

        /*
         * Virtual method Window.activateDefault() would be protected in Java,
         * but it overrides a public method with the same name in Widget.
         * Therefore, it must also be public.
         */
        if (element instanceof VirtualMethod vm
                && "activate_default".equals(vm.name())
                && "Window".equals(vm.parameters().instanceParameter().anyType().name()))
            return vm.withAttribute("java-gi-override-visibility", "PUBLIC");

        return element;
    }
}
