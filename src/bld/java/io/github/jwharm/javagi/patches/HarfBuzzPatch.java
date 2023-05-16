package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;

public class HarfBuzzPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // This constant has type "language_t" which cannot be instantiated
        removeConstant(repo, "LANGUAGE_INVALID");
    }
}
