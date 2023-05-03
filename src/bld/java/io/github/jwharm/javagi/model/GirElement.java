package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Platform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class GirElement {

    private static GirElement previouslyCreated = null;

    public final GirElement parent;
    public GirElement next;
    public Array array;
    public Type type = null;
    public String name = null;
    public Doc doc = null;
    public DocDeprecated docDeprecated = null;
    public DocVersion docVersion = null;
    public final List<Member> memberList = new ArrayList<>();
    public final List<Attribute> attributeList = new ArrayList<>();
    public final List<Field> fieldList = new ArrayList<>();
    public final List<Function> functionList = new ArrayList<>();
    public final List<Include> includeList = new ArrayList<>();
    public final List<Implements> implementsList = new ArrayList<>();
    public final List<Method> methodList = new ArrayList<>();
    public final List<Property> propertyList = new ArrayList<>();
    public final List<Signal> signalList = new ArrayList<>();
    public final List<VirtualMethod> virtualMethodList = new ArrayList<>();
    public final List<Constructor> constructorList = new ArrayList<>();
    public final List<Alias> aliasList = new ArrayList<>();
    public final List<Callback> callbackList = new ArrayList<>();
    public final List<Bitfield> bitfieldList = new ArrayList<>();
    public final List<Class> classList = new ArrayList<>();
    public final List<Constant> constantList = new ArrayList<>();
    public final List<Docsection> docsectionList = new ArrayList<>();
    public final List<Enumeration> enumerationList = new ArrayList<>();
    public final List<FunctionMacro> functionMacroList = new ArrayList<>();
    public final List<Interface> interfaceList = new ArrayList<>();
    public final List<Record> recordList = new ArrayList<>();
    public final List<Union> unionList = new ArrayList<>();

    public final Set<Platform> platforms = new HashSet<>();

    public GirElement(GirElement parent) {
        this.parent = parent;

        // Create a link to the previously created element, so we can easily traverse the entire tree later.
        // Don't link from one repository to the next.
        if (! (previouslyCreated == null || this instanceof Repository)) {
            previouslyCreated.next = this;
        }
        previouslyCreated = this;
    }

    public Namespace getNamespace() {
        if (this instanceof Repository r) {
            return r.namespace;
        } else if (this instanceof Namespace ns) {
            return ns;
        } else {
            return this.parent.getNamespace();
        }
    }

    public Module module() {
        return ((Repository) getNamespace().parent).module;
    }

    public boolean isApi() {
        return false;
    }

    public String toString() {
        return this.getClass().getSimpleName() + " " + this.name;
    }
}
