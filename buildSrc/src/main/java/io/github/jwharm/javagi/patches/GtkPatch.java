package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.generator.Platform;
import io.github.jwharm.javagi.model.Repository;

public class GtkPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Override with different return type
        renameMethod(repo, "ApplicationWindow", "get_id", "get_window_id");
        renameMethod(repo, "MenuButton", "get_direction", "get_arrow_direction");
        renameMethod(repo, "PrintSettings", "get", "get_string");
        if (repo.module.platform != Platform.WINDOWS)
            renameMethod(repo, "PrintUnixDialog", "get_settings", "get_print_settings");
        renameMethod(repo, "Widget", "activate", "activate_widget");
        renameMethod(repo, "Widget", "activate_action", "activate_action_if_exists");

        setReturnType(findMethod(repo, "MediaStream", "play"), "gboolean", "gboolean", "1", null);

        // These calls return floating references
        setReturnFloating(findMethod(repo, "FileFilter", "to_gvariant"));
        setReturnFloating(findMethod(repo, "PageSetup", "to_gvariant"));
        setReturnFloating(findMethod(repo, "PaperSize", "to_gvariant"));
        setReturnFloating(findMethod(repo, "PrintSettings", "to_gvariant"));

        findVirtualMethod(repo, "BuilderScope", "get_type_from_name").skip = false;
        findVirtualMethod(repo, "BuilderScope", "get_type_from_function").skip = false;
        findVirtualMethod(repo, "BuilderScope", "create_closure").skip = false;

        // Add virtual methods as instance methods
        addInstanceMethod(repo, "Window", "activate_default");
        addInstanceMethod(repo, "Dialog", "close");
        addInstanceMethod(repo, "Popover", "activate_default");
    }
}
