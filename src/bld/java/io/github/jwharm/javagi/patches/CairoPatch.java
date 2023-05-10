package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.PatchSet;
import io.github.jwharm.javagi.model.Repository;

public class CairoPatch implements PatchSet {

    @Override
    public void patch(Repository repo) {
        // Incompletely defined
        removeFunction(repo, "image_surface_create");
    }
}
