package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Repository;
import io.github.jwharm.javagi.model.Record;

public class CairoPatch implements Patch {

    @Override
    public void patch(Repository repo) {

        // Remove the glib:get-type attributes from the cairo types.
        // The cairo bindings don't have the get-type methods.
        for (var entry : repo.namespace.registeredTypeMap.entrySet()) {
            var registeredType = entry.getValue();
            if (registeredType instanceof Record rec) {
                rec.getType = null;
            }
        }
    }
}
