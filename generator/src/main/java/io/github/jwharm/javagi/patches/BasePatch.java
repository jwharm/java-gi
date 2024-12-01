package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;

import java.util.ArrayList;
import java.util.List;

import static io.github.jwharm.javagi.gir.Direction.INOUT;
import static io.github.jwharm.javagi.gir.Direction.OUT;
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
                && (rec.name().endsWith("Private") || rec.name().startsWith("_")))
            return element.withAttribute("java-gi-skip", "1");

        /*
         * Change all functions where the first parameter is of the enclosing
         * type (class, record, enumeration, ...) into methods with an instance
         * parameter.
         */
        if (element instanceof RegisteredType e
            && (! (e instanceof Namespace))
            && e.children().stream().anyMatch(
                child -> child instanceof Function func
                    && func.parameters() != null
                    && isInstanceParameter(func.parameters().parameters().getFirst(),
                                           e, namespace))) {

            // Create a new list of nodes for the type,
            // replacing functions by methods when possible.
            List<Node> children = new ArrayList<>();
            for (Node child : e.children()) {
                if (child instanceof Function func
                        && func.parameters() != null
                        && isInstanceParameter(func.parameters().parameters().getFirst(),
                                               e, namespace)) {

                    // Create new <parameters> element for method
                    var pList = func.parameters().parameters();
                    var pFirst = pList.getFirst();
                    var pOthers = pList.subList(1, pList.size());
                    var pInstance = new InstanceParameter(pFirst.attributes(), pFirst.children());
                    ArrayList<Node> paramList = new ArrayList<>(pOthers);
                    paramList.addFirst(pInstance);

                    // Update references to parameter indexes
                    for (var p : pOthers) {
                        for (var at : p.attributes().keySet())
                            if (at.equals("destroy") || at.equals("closure"))
                                p.attributes().put(at, "" + (p.attrInt(at) - 1));
                        if (p.anyType() instanceof Array array && array.attr("length") != null)
                            array.attributes().put("length", "" + (array.attrInt("length") - 1));
                    }
                    var rt = func.returnValue();
                    for (var at : rt.attributes().keySet())
                        if (at.equals("destroy") || at.equals("closure"))
                            rt.attributes().put(at, "" + (rt.attrInt(at) - 1));
                    if (rt.anyType() instanceof Array array && array.attr("length") != null)
                        array.attributes().put("length", "" + (array.attrInt("length") - 1));

                    // Construct <method> element
                    Method method = new Method(
                            func.attributes(),
                            listOfNonNull(
                                    func.infoElements().doc(),
                                    func.infoElements().sourcePosition(),
                                    rt,
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

        /*
         * Some methods are shadowed by methods that take closures. This makes
         * for a worse API in java-gi, and can also lead to issues with
         * different parameter names when combining virtual methods with their
         * invoker method in one Java method. Remove the shadowing.
         */
        if (element instanceof Method m
                && m.callableAttrs().shadowedBy() != null
                && m.callableAttrs().shadowedBy().endsWith("_with_closures")) {
            return m.withAttribute("shadowed-by", null);
        }
        if (element instanceof Method m
                && m.callableAttrs().shadows() != null
                && m.attr("name").endsWith("_with_closures")) {
            return m.withAttribute("shadows", null);
        }

        /*
         * Remove aliases for void. They are unusable for language bindings.
         */
        if (element instanceof Alias a
                && a.anyType() instanceof Type t && t.isVoid())
            return element.withAttribute("java-gi-skip", "1");

        return element;
    }

    private boolean isInstanceParameter(Parameter param,
                                        RegisteredType rt,
                                        String namespace) {
        return (param.direction() != OUT && param.direction() != INOUT)
                && param.anyType() instanceof Type t
                && (rt.name().equals(t.name())
                    || (namespace + "." + rt.name()).equals(t.name()));
    }
}
