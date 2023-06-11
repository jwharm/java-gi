package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;

public class WebkitGtkPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Override with different return type
        renameMethod(repo, "WebView", "get_settings", "get_web_view_settings");
    }
}
