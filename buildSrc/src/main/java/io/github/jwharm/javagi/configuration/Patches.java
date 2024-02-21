package io.github.jwharm.javagi.configuration;

import io.github.jwharm.javagi.patches.*;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

public class Patches {
    public static final List<Patch> PATCHES = List.of(
            new AdwPatch(),
            new GLibPatch(),
            new GioPatch(),
            new GObjectPatch(),
            new GstAudioPatch(),
            new GstBasePatch(),
            new GtkPatch(),
            new HarfBuzzPatch(),
            new PangoPatch(),
            new SoupPatch(),
            new WebKitPatch()
    );
}
