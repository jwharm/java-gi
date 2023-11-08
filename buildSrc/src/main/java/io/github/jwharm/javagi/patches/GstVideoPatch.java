package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;

public class GstVideoPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        setReturnType(repo, "VideoOverlay", "set_render_rectangle", "none", "void", null, null);
    }
}
