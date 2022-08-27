package girparser.model;

import java.util.ArrayList;
import java.util.List;

public class GirElement {

    private static GirElement previouslyCreated = null;

    public GirElement parent, next;
    public Array array;
    public Type type = null;
    public String name = null;
    public Doc doc = null;
    public DocDeprecated docDeprecated = null;
    public DocVersion docVersion = null;
    public List<Member> memberList = new ArrayList<>();
    public List<Attribute> attributeList = new ArrayList<>();
    public List<Field> fieldList = new ArrayList<>();
    public List<Function> functionList = new ArrayList<>();
    public List<Implements> implementsList = new ArrayList<>();
    public List<Method> methodList = new ArrayList<>();
    public List<Property> propertyList = new ArrayList<>();
    public List<Signal> signalList = new ArrayList<>();
    public List<VirtualMethod> virtualMethodList = new ArrayList<>();
    public List<Constructor> constructorList = new ArrayList<>();
    public List<Alias> aliasList = new ArrayList<>();
    public List<Callback> callbackList = new ArrayList<>();
    public List<Bitfield> bitfieldList = new ArrayList<>();
    public List<Class> classList = new ArrayList<>();
    public List<Constant> constantList = new ArrayList<>();
    public List<Docsection> docsectionList = new ArrayList<>();
    public List<Enumeration> enumerationList = new ArrayList<>();
    public List<FunctionMacro> functionMacroList = new ArrayList<>();
    public List<Interface> interfaceList = new ArrayList<>();
    public List<Record> recordList = new ArrayList<>();
    public List<Union> unionList = new ArrayList<>();


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

    public Repository getRepository() {
        return (Repository) getNamespace().parent;
    }

    public String toString() {
        return this.getClass().getSimpleName() + " " + this.name;
    }
}
