/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2025 Jan-Willem Harmannij
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

package org.javagi.util;

import org.gnome.glib.GLib;
import org.javagi.interop.Interop;
import org.javagi.interop.Platform;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.text.MessageFormat;

/**
 * Utility class to translate text to the current locale using GNU Gettext.
 * <p>
 * To use this class, call {@link #bindtextdomain} and {@link #textdomain}
 * at the start of your application, ideally at the top of your {@code main()}
 * method, but in any case before Gtk is initialized. For every text message
 * that is displayed to the users, call one of the {@link #i18n(String)}
 * methods to retrieve a translation in the current locale. When no translation
 * is present, the original text is returned.
 * <p>
 * It is recommended to statically import the {@link #i18n} methods in
 * all classes where it is used.
 * <p>
 * Before marking strings as internationalizable, uses of the string
 * concatenation operator need to be converted to {@link MessageFormat}
 * applications. For example, {@code "file " + filename + " not found"} becomes
 * {@code MessageFormat.format("file {0} not found", filename)}. Only after
 * this is done, can the strings be marked and extracted.
 * <p>
 * This class requires GNU Gettext to be installed. Specifically, it will try
 * to load the {@code libgettextlib} shared library. If that did not work, all
 * methods will silently fallback to return the original (English) messages.
 * <p>
 * Gettext offers tools to extract a message catalogue from your application
 * sources and create and compile per-language translation files. Consult the
 * <a href="https://www.gnu.org/software/gettext">GNU Gettext documentation</a>
 * for details.
 */
public class Intl {

    private static String domain = null;

    static {
        switch (Platform.getRuntimePlatform()) {
            case LINUX -> Interop.loadLibrary("libgettextlib.so");
            case WINDOWS -> Interop.loadLibrary("libgettextlib.dll");
            case MACOS -> Interop.loadLibrary("libgettextlib.dylib");
        }
    }

    // #include <libintl.h>
    // char * bindtextdomain (const char * domainname, const char * dirname);
    private static final MethodHandle bindtextdomain = Interop.downcallHandle(
            "bindtextdomain",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            false);

    // #include <libintl.h>
    // char * textdomain (const char * domainname);
    private static final MethodHandle textdomain = Interop.downcallHandle(
            "textdomain",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            false);

    /**
     * Set directory containing message catalogs for Gettext.
     *
     * @param domainname the message domain
     * @param dirname the base directory of the hierarchy containing message
     *                catalogs for {@code domainname}
     * @return the current base directory for domain {@code domainname}, or
     *         {@code null} if an error occured.
     */
    public static String bindtextdomain(String domainname, String dirname) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment result = (MemorySegment) bindtextdomain.invokeExact(
                    Interop.allocateNativeString(domainname, arena),
                    Interop.allocateNativeString(dirname, arena));
            return Interop.getStringFrom(result);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Set or retrieve the current text domain for Gettext.
     *
     * @param domainname the message domain
     * @return the message domain, or {@code null} if an error occured.
     */
    public static String textdomain(String domainname) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment result = (MemorySegment) textdomain.invokeExact(
                    Interop.allocateNativeString(domainname, arena));
            domain = domainname;
            return Interop.getStringFrom(result);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Translate the string.
     *
     * @param  msgid the string to translate
     * @return the translation, or {@code msgid} if none is found
     * @see    GLib#dgettext
     */
    public static String i18n(String msgid) {
        return GLib.dgettext(domain, msgid);
    }

    /**
     * Translate a string that can be singular or plural.
     *
     * @param  msgid  the string to translate
     * @param  plural the string to translate (plural form)
     * @param  n      determines whether singular or plural translation is chosen
     * @return the translation, or {@code msgid} or {@code plural} if none is found
     * @see    GLib#dngettext
     */
    public static String i18n(String msgid, String plural, int n) {
        return GLib.dngettext(domain, msgid, plural, n);
    }

    /**
     * Translate a string with context.
     *
     * @param  context context of the string to translate
     * @param  msgid   the string to translate
     * @return the translation, or {@code msgid} if none is found
     * @see    GLib#dpgettext2
     */
    public static String i18n(String context, String msgid) {
        return GLib.dpgettext2(domain, context, msgid);
    }
}
