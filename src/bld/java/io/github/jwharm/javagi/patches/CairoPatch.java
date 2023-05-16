package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;

public class CairoPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Incompletely defined
        removeFunction(repo, "image_surface_create");
    }
}
