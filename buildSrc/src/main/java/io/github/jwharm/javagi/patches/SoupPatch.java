package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;

public class SoupPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        setReturnType(repo, "AuthDomain", "challenge", "none", "void", null, null);
    }
}
