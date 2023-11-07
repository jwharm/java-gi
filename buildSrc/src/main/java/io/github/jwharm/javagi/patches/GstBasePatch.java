package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;

public class GstBasePatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Add virtual methods as instance methods
        addInstanceMethod(repo, "BaseSink", "query");
        addInstanceMethod(repo, "BaseSrc", "query");
    }
}
