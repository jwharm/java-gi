package io.github.jwharm.javagi.configuration;

import io.github.jwharm.javagi.patches.*;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

public class Patches {
    public static final List<Patch> PATCHES = List.of(
            new GLibPatch(),
            new GObjectRemoveVaMarshal(),
            new GObjectTypeModuleUse(),
            new GioWindowsFileDescriptorBased(),
            new HBWindowsBitfields(),
            new HBRemoveTrailingT()
    );
}
