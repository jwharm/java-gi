package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;

import java.util.ArrayList;
import java.util.List;

import static io.github.jwharm.javagi.util.CollectionUtils.listOfNonNull;

/**
 * This patch will apply to all GIR files
 */
public class BasePatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        /*
         * Do not generate record types named "...Private", except for GPrivate
         */
        if (element instanceof Record rec
                && (! "GPrivate".equals(rec.cType()))
                && rec.name() != null
                && rec.name().endsWith("Private"))
            return element.withAttribute("java-gi-skip", "1");

        /*
         * Enumerations cannot have methods in GObject-Introspection, but this
         * is allowed in Java. We change all functions where the first parameter
         * is of the enclosing enumeration into methods with an instance
         * parameter.
         */
        if (element instanceof Enumeration e
                && e.children().stream().anyMatch(Function.class::isInstance)) {

            // Create a new list of nodes for the enumeration/bitfield,
            // replacing functions by methods when possible.
            List<Node> children = new ArrayList<>();
            for (Node child : e.children()) {
                if (child instanceof Function func
                        && func.parameters() != null
                        && func.parameters().parameters().getFirst().anyType() instanceof Type t
                        && e.name().equals(t.name())) {

                    // Create new <parameters> element for method
                    var pList = func.parameters().parameters();
                    var pFirst = pList.getFirst();
                    var pOthers = pList.subList(1, pList.size());
                    var pInstance = new InstanceParameter(pFirst.attributes(), pFirst.children());
                    ArrayList<Node> paramList = new ArrayList<>(pOthers);
                    paramList.addFirst(pInstance);

                    // Construct <method> element
                    Method method = new Method(
                            func.attributes(),
                            listOfNonNull(
                                    func.infoElements().doc(),
                                    func.infoElements().sourcePosition(),
                                    func.returnValue(),
                                    new Parameters(paramList)),
                            func.platforms());

                    // Add method to enumeration
                    children.add(method);
                } else {
                    // Add node unmodified to enumeration
                    children.add(child);
                }
            }
            return e.withChildren(children);
        }

        return element;
    }
}
