package io.github.jwharm.javagi.base;

import io.github.jwharm.javagi.Constants;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;

public final class Logger {

    public static void debug(String message, Object... varargs) {
        GLib.log(Constants.LOG_DOMAIN, LogLevelFlags.LEVEL_DEBUG, message, varargs);
    }

    // Prevent instantiation
    private Logger() {}
}
