package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.PatchSet;
import io.github.jwharm.javagi.model.Repository;

public class GstPatch implements PatchSet {

    @Override
    public void patch(Repository repo) {
        // According to the gir file, the size parameter is an out parameter, but it isn't
        removeMethod(repo, "TypeFind", "peek");
    }
}
