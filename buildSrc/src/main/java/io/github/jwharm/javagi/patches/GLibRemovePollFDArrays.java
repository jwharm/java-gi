package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

import static java.util.function.Predicate.not;

public class GLibRemovePollFDArrays implements Patch {
    private static final List<String> METHODS_TO_REMOVE = List.of("g_main_context_query", "g_main_context_check");

    @Override
    public GirElement patch(GirElement element) {
        if (element instanceof Record rec && "GMainContext".equals(rec.cType()))
            return new Record(rec.attributes(), rec.children().stream().filter(not(node ->
                node instanceof Method m && METHODS_TO_REMOVE.contains(m.attrs().cIdentifier())
            )).toList(), rec.platforms());

        return element;
    }
}
