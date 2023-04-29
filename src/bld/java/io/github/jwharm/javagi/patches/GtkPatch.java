package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.PatchSet;
import io.github.jwharm.javagi.model.Repository;

public class GtkPatch implements PatchSet {

    @Override
    public void patch(Repository repo) {
        // Override with different return type
        renameMethod(repo, "ApplicationWindow", "get_id", "get_window_id");
        renameMethod(repo, "MenuButton", "get_direction", "get_arrow_direction");
        renameMethod(repo, "PrintSettings", "get", "get_string");
        renameMethod(repo, "PrintUnixDialog", "get_settings", "get_print_settings");
        renameMethod(repo, "Widget", "activate", "activate_widget");

        // These calls return floating references
        setReturnFloating(findMethod(repo, "FileFilter", "to_gvariant"));
        setReturnFloating(findMethod(repo, "PageSetup", "to_gvariant"));
        setReturnFloating(findMethod(repo, "PaperSize", "to_gvariant"));
        setReturnFloating(findMethod(repo, "PrintSettings", "to_gvariant"));
    }
}
