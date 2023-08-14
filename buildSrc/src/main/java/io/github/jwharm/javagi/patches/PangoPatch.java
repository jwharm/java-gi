package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;

public class PangoPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Return type defined as "Language" but should be "Language*"
        removeMethod(repo, "Font", "get_languages");
    }

}
