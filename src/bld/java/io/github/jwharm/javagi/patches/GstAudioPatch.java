package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.PatchSet;
import io.github.jwharm.javagi.model.Repository;

public class GstAudioPatch implements PatchSet {

    @Override
    public void patch(Repository repo) {
        // Override with different return type
        setReturnType(repo, "AudioSink", "stop", "gboolean", "gboolean", "true", "always %TRUE");
        // A GstFraction cannot automatically be put into a GValue
        removeProperty(repo, "AudioAggregator", "output-buffer-duration-fraction");
    }
}
