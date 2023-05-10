package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.PatchSet;
import io.github.jwharm.javagi.model.Repository;

public class AdwaitaPatch implements PatchSet {

    @Override
    public void patch(Repository repo) {
        // Override with different return type
        renameMethod(repo, "ActionRow", "activate", "activate_row");
        renameMethod(repo, "SplitButton", "get_direction", "get_arrow_direction");
    }
}
