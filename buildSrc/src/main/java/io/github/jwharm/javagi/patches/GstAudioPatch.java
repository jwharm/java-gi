package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Method;
import io.github.jwharm.javagi.model.Parameter;
import io.github.jwharm.javagi.model.Repository;

public class GstAudioPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Override with different return type (BaseSink.stop() returns boolean)
        setReturnType(findVirtualMethod(repo, "AudioSink", "stop"), "gboolean", "gboolean", "1", "always %TRUE");
        // A GstFraction cannot automatically be put into a GValue
        removeProperty(repo, "AudioAggregator", "output-buffer-duration-fraction");

        // Macos version of AudioDecoder.parse has these attributes
        Method m = findVirtualMethod(repo, "AudioDecoder", "parse");
        Parameter p = m.parameters.parameterList.get(2);
        p.direction = "out";
        p.callerAllocates = "0";
        p.transferOwnership = "full";
        p = m.parameters.parameterList.get(3);
        p.direction = "out";
        p.callerAllocates = "0";
        p.transferOwnership = "full";
    }
}
