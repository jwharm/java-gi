package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Class;
import io.github.jwharm.javagi.model.Package;
import io.github.jwharm.javagi.model.Record;
import io.github.jwharm.javagi.model.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

public class GirParser extends DefaultHandler {

    private final SAXParser parser;
    private StringBuilder chars;
    private GirElement current;
    private String skip;

    @Override
    public void startDocument() {
        chars = new StringBuilder();
        current = null;
        skip = null;
    }

    @Override
    public void endDocument() {
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) {
        if (skip != null) {
            return;
        }

        switch (qName) {
            case "alias" -> {
                Alias newAlias = new Alias(current, attr.getValue("name"));
                current.aliasList.add(newAlias);
                current = newAlias;
            }
            case "array" -> {
                Array newArray = new Array(current, attr.getValue("name"), attr.getValue("c:type"),
                        attr.getValue("zero-terminated"), attr.getValue("fixed-size"));
                current.array = newArray;
                current = newArray;
            }
            case "attribute" -> {
                Attribute newAttribute = new Attribute(current, attr.getValue("name"),
                        attr.getValue("type"));
                current.attributeList.add(newAttribute);
                current = newAttribute;
            }
            case "bitfield" -> {
                Bitfield newBitfield = new Bitfield(current, attr.getValue("name"));
                current.bitfieldList.add(newBitfield);
                current = newBitfield;

            }
            case "callback" -> {
                Callback newCallback = new Callback(current, attr.getValue("name"));
                if (current instanceof Namespace ns) {
                    ns.callbackList.add(newCallback);
                } else if (current instanceof Field f) {
                    f.callback = newCallback;
                }
                current = newCallback;
            }
            case "class" -> {
                io.github.jwharm.javagi.model.Class newClass = new Class(current, attr.getValue("name"), attr.getValue("parent"));
                current.classList.add(newClass);
                current = newClass;
            }
            case "constant" -> {
                Constant newConstant = new Constant(current, attr.getValue("name"),
                        attr.getValue("value"), attr.getValue("c:type"));
                current.constantList.add(newConstant);
                current = newConstant;
            }
            case "constructor" -> {
                Constructor newConstructor = new Constructor(current, attr.getValue("name"),
                        attr.getValue("c:identifier"), attr.getValue("deprecated"),
                        attr.getValue("throws"));
                current.constructorList.add(newConstructor);
                current = newConstructor;
            }
            case "docsection" -> {
                Docsection newDocsection = new Docsection(current, attr.getValue("name"));
                current.docsectionList.add(newDocsection);
                current = newDocsection;
            }
            case "doc" -> {
                Doc newDoc = new Doc(current, attr.getValue("xml:space"));
                current.doc = newDoc;
                current = newDoc;
            }
            case "doc-deprecated" -> {
                DocDeprecated newDocDeprecated = new DocDeprecated(current, attr.getValue("xml:space"));
                current.docDeprecated = newDocDeprecated;
                current = newDocDeprecated;
            }
            case "doc-version" -> {
                DocVersion newDocVersion = new DocVersion(current, attr.getValue("xml:space"));
                current.docVersion = newDocVersion;
                current = newDocVersion;
            }
            case "enumeration" -> {
                Enumeration newEnumeration = new Enumeration(current, attr.getValue("name"));
                current.enumerationList.add(newEnumeration);
                current = newEnumeration;
            }
            case "field" -> {
                Field newField = new Field(current, attr.getValue("name"), attr.getValue("readable"),
                        attr.getValue("private"));
                current.fieldList.add(newField);
                current = newField;
            }
            case "function" -> {
                Function newFunction = new Function(current, attr.getValue("name"),
                        attr.getValue("c:identifier"), attr.getValue("deprecated"),
                        attr.getValue("throws"));
                if (current instanceof Namespace ns) {
                    ns.functionList.add(newFunction);
                } else if (current instanceof RegisteredType c) {
                    c.functionList.add(newFunction);
                }
                current = newFunction;
            }
            case "function-macro" -> {
                FunctionMacro newFunctionMacro = new FunctionMacro(current, attr.getValue("name"),
                        attr.getValue("c:identifier"), attr.getValue("introspectable"),
                        attr.getValue("deprecated"), attr.getValue("throws"));
                current.functionMacroList.add(newFunctionMacro);
                current = newFunctionMacro;
            }
            case "implements" -> {
                Implements newImplements = new Implements(current, attr.getValue("name"));
                current.implementsList.add(newImplements);
                current = newImplements;
            }
            case "instance-parameter" -> {
                InstanceParameter newInstanceParameter = new InstanceParameter(current, attr.getValue("name"),
                        attr.getValue("transfer-ownership"));
                ((Parameters) current).parameterList.add(newInstanceParameter);
                current = newInstanceParameter;
            }
            case "interface" -> {
                Interface newInterface = new Interface(current, attr.getValue("name"));
                current.interfaceList.add(newInterface);
                current = newInterface;
            }
            case "member" -> {
                Member newMember = new Member(current, attr.getValue("name"),
                        attr.getValue("c:identifier"), attr.getValue("value"));
                current.memberList.add(newMember);
                current = newMember;
            }
            case "method" -> {
                Method newMethod = new Method(current, attr.getValue("name"),
                        attr.getValue("c:identifier"), attr.getValue("deprecated"),
                        attr.getValue("throws"));
                current.methodList.add(newMethod);
                current = newMethod;
            }
            case "namespace" -> {
                Namespace newNamespace = new Namespace(current, attr.getValue("name"));
                ((Repository) current).namespace = newNamespace;
                current = newNamespace;
            }
            case "package" -> {
                io.github.jwharm.javagi.model.Package newPackage = new Package(current, attr.getValue("name"));
                ((Repository) current).package_ = newPackage;
                current = newPackage;
            }
            case "parameter" -> {
                Parameter newParameter = new Parameter(current, attr.getValue("name"),
                        attr.getValue("transfer-ownership"), attr.getValue("nullable"),
                        attr.getValue("allow-none"), attr.getValue("direction"));
                ((Parameters) current).parameterList.add(newParameter);
                current = newParameter;
            }
            case "parameters" -> {
                Parameters newParameters = new Parameters(current);
                ((CallableType) current).setParameters(newParameters);
                current = newParameters;
            }
            case "prerequisite" -> {
                Prerequisite newPrerequisite = new Prerequisite(current, attr.getValue("name"));
                ((Interface) current).prerequisite = newPrerequisite;
                current = newPrerequisite;
            }
            case "property" -> {
                Property newProperty = new Property(current, attr.getValue("name"),
                        attr.getValue("transfer-ownership"), attr.getValue("getter"));
                current.propertyList.add(newProperty);
                current = newProperty;
            }
            case "record" -> {
                Record newRecord = new Record(current, attr.getValue("name"),
                        attr.getValue("c:type"), attr.getValue("glib:is-gtype-struct-for"));
                current.recordList.add(newRecord);
                current = newRecord;
            }
            case "repository" -> {
                current = new Repository();
            }
            case "return-value" -> {
                ReturnValue newReturnValue = new ReturnValue(current, attr.getValue("transfer-ownership"));
                ((CallableType) current).setReturnValue(newReturnValue);
                current = newReturnValue;
            }
            case "glib:signal" -> {
                Signal newSignal = new Signal(current, attr.getValue("name"), attr.getValue("when"),
                        attr.getValue("deprecated"), attr.getValue("throws"));
                current.signalList.add(newSignal);
                current = newSignal;
            }
            case "type" -> {
                Type newType = new Type(current, attr.getValue("name"), attr.getValue("c:type"));
                current.type = newType;
                current = newType;
            }
            case "union" -> {
                Union newUnion = new Union(current, attr.getValue("name"));
                current.unionList.add(newUnion);
                current = newUnion;
            }
            case "varargs" -> {
                ((Parameter) current).varargs = true;
                skip = qName;
            }
            case "virtual-method" -> {
                VirtualMethod newVirtualMethod = new VirtualMethod(current, attr.getValue("name"),
                        attr.getValue("deprecated"), attr.getValue("throws"));
                current.virtualMethodList.add(newVirtualMethod);
                current = newVirtualMethod;
            }
            case "glib:boxed", "include", "c:include", "source-position" -> {
                // Ignored
                skip = qName;
            }
            default -> {
                System.out.println("WARNING: Unhandled gir element: " + qName);
                skip = qName;
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (!(chars.isEmpty() || chars.toString().isBlank())) {
            String characters = chars.toString();
            chars.setLength(0);
            if (current instanceof Doc doc) {
                doc.contents = characters;
            }
        }
        if (qName.equals(skip)) {
            skip = null;
        } else if ((skip == null) && (! (current instanceof Repository))) {
            current = current.parent;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        chars.append(ch, start, length);
    }

    public GirParser() throws ParserConfigurationException, SAXException {
        parser = SAXParserFactory.newInstance().newSAXParser();
    }

    public Repository parse(String uri) throws IOException, SAXException {
        if (! new File(uri).exists()) {
            throw new IOException("Specified GIR file does not exist: " + uri);
        }
        parser.parse(uri, this);
        return (Repository) current;
    }
}
