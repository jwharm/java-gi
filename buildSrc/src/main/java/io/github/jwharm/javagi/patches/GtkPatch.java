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

package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

public class GtkPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"Gtk".equals(namespace))
            return element;

        /*
         * Gtk.CustomLayout is a convenience class for C code that wants to
         * avoid subclassing Gtk.LayoutManager. It is not supposed to be used
         * by language bindings, and will never work correctly, as it doesn't
         * have the necessary parameters and annotations to manage the lifetime
         * of the callback functions.
         * See also https://github.com/gtk-rs/gtk4-rs/issues/23, especially the
         * first comment.
         */
        if (element instanceof Namespace ns) {
            ns =   remove(ns, Callback.class, "name", "CustomRequestModeFunc");
            ns =   remove(ns, Callback.class, "name", "CustomMeasureFunc");
            ns =   remove(ns, Callback.class, "name", "CustomAllocateFunc");
            ns =   remove(ns, Record.class,   "name", "CustomLayoutClass");
            return remove(ns, Class.class,    "name", "CustomLayout");
        }

        /*
         * ApplicationWindow.getId() overrides Buildable.getId() with a
         * different return type. Rename to getWindowId()
         */
        if (element instanceof Method m
                && "gtk_application_window_get_id".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("name", "get_window_id");

        /*
         * MenuButton.getDirection() overrides Widget.getDirection() with a
         * different return type. Rename to getArrowDirection()
         */
        if (element instanceof Method m
                && "gtk_menu_button_get_direction".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("name", "get_arrow_direction");

        /*
         * PrintSettings.get() overrides GObject.get() with a different return
         * type. Rename to getString()
         */
        if (element instanceof Method m
                && "gtk_print_settings_get".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("name", "get_string");

        /*
         * PrintUnixDialog.getSettings() overrides Widget.getSettings() with a
         * different return type. Rename to getPrintSettings()
         */
        if (element instanceof Method m
                && "gtk_print_unix_dialog_get_settings".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("name", "get_print_settings");

        /*
         * Widget.activate() is problematic for several subclasses that have
         * their own "activate" virtual method: in Java, a child class cannot
         * override a public method with a protected method. Rename
         * Widget.activate() to activateWidget()
         */
        if (element instanceof Method m
                && "gtk_widget_activate".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("name", "activate_widget");

        /*
         * Widget.activateAction() returns boolean.
         * ActionGroup.activateAction() returns void.
         * Class ApplicationWindow extends Widget and implements ActionGroup.
         * This doesn't compile in Java. We rename Widget.activateAction()
         * to activateActionIfExists() to resolve this.
         *
         * Furthermore, Widget.activateAction() is shadowed by
         * Widget.activateActionVariant() so we have to rename the "shadows"
         * attribute too.
         */
        if (element instanceof Method m
                && "gtk_widget_activate_action".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("name", "activate_action_if_exists");

        if (element instanceof Method m
                && "gtk_widget_activate_action_variant".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("shadows", "activate_action_if_exists");

        /*
         * The invoker attribute isn't set automatically in the gir file,
         * because the return values are not the same. Set the attribute
         * anyway.
         */
        if (element instanceof VirtualMethod vm
                && "play".equals(vm.name())
                && "MediaStream".equals(vm.parameters().instanceParameter().type().name()))
            return vm.withAttribute("invoker", "play");

        /*
         * Virtual method Window.activateDefault() and
         * Popover.activateDefault() would be protected in Java, but they
         * override a public method with the same name in Widget. Therefore,
         * they must also be public.
         */
        if (element instanceof VirtualMethod vm
                && "activate_default".equals(vm.name())
                && List.of("Window", "Popover").contains(
                        vm.parameters().instanceParameter().type().name()))
            return vm.withAttribute("java-gi-override-visibility", "PUBLIC");

        /*
         * Same for Dialog.close() overriding Window.close()
         */
        if (element instanceof VirtualMethod vm
                && "close".equals(vm.name())
                && "Dialog".equals(vm.parameters().instanceParameter().type().name()))
            return vm.withAttribute("java-gi-override-visibility", "PUBLIC");

        /*
         * The virtual methods in the GtkBuilderScope interface are not
         * generated automatically, but they are needed by BuilderJavaScope.
         */
        var methods = List.of(
                "get_type_from_name",
                "get_type_from_function",
                "create_closure"
        );
        if (element instanceof VirtualMethod vm
                && methods.contains(vm.name())
                && "BuilderScope".equals(vm.parameters().instanceParameter().type().name()))
            return vm.withAttribute("java-gi-dont-skip", "1");

        /*
         * The "introspectable=0" attribute is set on the function
         * "Gtk::orderingFromCmpfunc" on Windows and macOS, but not on Linux.
         * Make sure that it is the same on every platform.
         */
        if (element instanceof Function f
                && "gtk_ordering_from_cmpfunc".equals(f.callableAttrs().cIdentifier()))
            return f.withAttribute("introspectable", "0");

        /*
         * FontDialog::chooseFontAndFeaturesFinish has different
         * transfer-ownership attributes between platforms. This might be
         * caused by minor version differences. Disable the Java bindings for
         * now.
         */
        if (element instanceof Class c
            && "FontDialog".equals(c.name()))
            return remove(c, Method.class, "name", "choose_font_and_features_finish");

        return element;
    }
}
