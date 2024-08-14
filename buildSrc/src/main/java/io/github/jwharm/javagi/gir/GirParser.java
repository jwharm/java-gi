/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.gir;

import io.github.jwharm.javagi.util.Patch;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import static io.github.jwharm.javagi.configuration.Patches.PATCHES;

/**
 * Parser class to parse the GIR XML and create a GIR tree.
 */
public final class GirParser {

    private static final List<String> SKIP_LIST = List.of(
            "c:include", "function-inline", "function-macro",
            "method-inline", "package"
    );
    private static final GirParser INSTANCE = new GirParser();
    private static final XMLInputFactory XML_INPUT_FACTORY =
            XMLInputFactory.newInstance();

    // Prevent instantiation
    private GirParser() {
    }

    /**
     * Returns a singleton GIRParser instance
     *
     * @return the instance
     */
    public static GirParser getInstance() {
        return INSTANCE;
    }

    // Get the element name as it is in the XML tag
    private static String qname(QName qname) {
        String prefix = qname.getPrefix();
        String localPart = qname.getLocalPart();
        return prefix.isEmpty() ? localPart : prefix + ":" + localPart;
    }

    // Create a map with all attributes of an XML element
    private static Map<String, String> attributes(StartElement element) {
        Map<String, String> attributes = new HashMap<>();
        for (var it = element.getAttributes(); it.hasNext(); ) {
            var attr = it.next();
            attributes.put(qname(attr.getName()), attr.getValue());
        }
        return attributes;
    }

    /**
     * Parse GIR XML and build the GIR model.
     *
     * @param  file       the GIR XML file
     * @param  platform   the platform of the GIR XML file
     * @param  repository an existing GIR model to merge with the new repository
     * @return the GIR model
     * @throws XMLStreamException    if the XML cannot be parsed
     * @throws FileNotFoundException if the specified file is not found
     */
    public Repository parse(File file, int platform, Repository repository)
            throws XMLStreamException, FileNotFoundException {

        if (!file.exists())
            return repository;

        XMLEventReader eventReader = XML_INPUT_FACTORY.createXMLEventReader(
                new FileInputStream(file));

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.isStartElement())
                return (Repository) parseElement(eventReader,
                                                 event.asStartElement(),
                                                 platform,
                                                 repository,
                                                 null);
        }

        throw new IllegalStateException("Invalid XML");
    }

    // Move in the existing GIR model in parallel with the parser in the XML
    private Node walkTree(StartElement elem, Node existingNode) {
        if (existingNode == null)
            return null;

        String elemName = qname(elem.getName());
        if (!List.of("namespace", "alias", "boxed", "callback", "class",
                        "bitfield", "enumeration", "interface", "record",
                        "union").contains(elemName))
            return existingNode;

        // Move from repository to namespace: return the namespace node with
        // the same name attribute.
        String name = attributes(elem).get("name");
        if (existingNode instanceof Repository repo
                && elemName.equals("namespace")) {
            for (Namespace ns : repo.namespaces())
                if (ns.name().equals(name))
                    return ns;
            return existingNode; // namespace not found in existing tree
        }

        // Move from namespace to type: return the type with the same name.
        if (existingNode instanceof Namespace ns)
            return ns.registeredTypes().get(name);

        // Nested struct/union types
        if ("record".equals(elemName))
            return existingNode.children().stream()
                    .filter(Record.class::isInstance)
                    .filter(r -> Objects.equals(name, r.attr("name")))
                    .findFirst()
                    .orElseThrow();
        if ("union".equals(elemName))
            return existingNode.children().stream()
                    .filter(Union.class::isInstance)
                    .filter(r -> Objects.equals(name, r.attr("name")))
                    .findFirst()
                    .orElseThrow();

        return existingNode;
    }

    // Recursive method to parse an XML element and create a GIR tree node.
    private GirElement parseElement(XMLEventReader eventReader,
                                    StartElement elem,
                                    int platform,
                                    Node existingNode,
                                    String nsName)
            throws XMLStreamException {

        var elemName = qname(elem.getName());
        var children = new ArrayList<Node>();
        var attributes = attributes(elem);
        var contents = new StringBuilder();

        while (eventReader.hasNext()) {
            fastForward(eventReader); // skip uninteresting elements
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                Node existingChildNode = walkTree(startElement, existingNode);

                // Remember namespace name
                if (qname(startElement.getName()).equals("namespace"))
                    nsName = startElement.getAttributeByName(
                            new QName("name")).getValue();

                // Create new child node
                Node newNode = parseElement(eventReader, startElement, platform,
                        existingChildNode, nsName);

                // Apply patches
                for (Patch patch : PATCHES)
                    newNode = patch.patch((GirElement) newNode, nsName);

                // Merge child nodes from other platforms into the new node
                if (existingChildNode instanceof Namespace existing
                        && newNode instanceof Namespace created)
                    newNode = created.mergeWith(existing);
                else if (existingChildNode instanceof RegisteredType existing
                        && newNode instanceof RegisteredType created)
                    newNode = created.mergeWith(existing);

                children.add(newNode);
            } else if (event.isCharacters()) {
                contents.append(event.asCharacters().getData());
            } else if (event.isEndElement()
                    && qname(event.asEndElement().getName()).equals(elemName)) {
                break;
            }
        }

        // Create GIR node based on the element name
        return switch (elemName) {
            case "alias"              -> new Alias(attributes, children, platform);
            case "array"              -> new Array(attributes, children);
            case "attribute"          -> new Attribute(attributes);
            case "bitfield"           -> new Bitfield(attributes, children, platform);
            case "glib:boxed"         -> new Boxed(attributes, children, platform);
            case "callback"           -> new Callback(attributes, children, platform);
            case "c:include"          -> new CInclude(attributes);
            case "class"              -> new Class(attributes, children, platform);
            case "constant"           -> new Constant(attributes, children, platform);
            case "constructor"        -> new Constructor(attributes, children, platform);
            case "doc"                -> new Doc(attributes, contents.toString().trim());
            case "docsection"         -> new Docsection(attributes, children);
            case "doc-deprecated"     -> new DocDeprecated(contents.toString().trim());
            case "doc-version"        -> new DocVersion(contents.toString().trim());
            case "enumeration"        -> new Enumeration(attributes, children, platform);
            case "field"              -> new Field(attributes, children);
            case "function"           -> new Function(attributes, children, platform);
            case "function-inline"    -> new FunctionInline();
            case "function-macro"     -> new FunctionMacro();
            case "implements"         -> new Implements(attributes);
            case "include"            -> new Include(attributes);
            case "instance-parameter" -> new InstanceParameter(attributes, children);
            case "interface"          -> new Interface(attributes, children, platform);
            case "member"             -> new Member(attributes, children);
            case "method"             -> new Method(attributes, children, platform);
            case "method-inline"      -> new MethodInline();
            case "namespace"          -> new Namespace(attributes, children, platform, new HashMap<>());
            case "package"            -> new Package(attributes);
            case "parameter"          -> new Parameter(attributes, children);
            case "parameters"         -> new Parameters(children);
            case "prerequisite"       -> new Prerequisite(attributes);
            case "property"           -> new Property(attributes, children, platform);
            case "record"             -> new Record(attributes, children, platform);
            case "repository"         -> new Repository(attributes, children);
            case "return-value"       -> new ReturnValue(attributes, children);
            case "glib:signal"        -> new Signal(attributes, children, platform);
            case "source-position"    -> new SourcePosition(attributes);
            case "type"               -> new Type(attributes, children);
            case "union"              -> new Union(attributes, children, platform);
            case "varargs"            -> new Varargs();
            case "virtual-method"     -> new VirtualMethod(attributes, children, platform);
            default                   -> throw new UnsupportedOperationException("Unsupported element: " + elemName);
        };
    }

    // Skip past <c:include>, <package> and <function-macro> elements
    private void fastForward(XMLEventReader eventReader)
            throws XMLStreamException {

        var event = eventReader.peek();
        if (!event.isStartElement()) return;
        String elemName = qname(event.asStartElement().getName());
        if (!SKIP_LIST.contains(elemName)) return;

        while (eventReader.hasNext()) {
            XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isEndElement()
                    && qname(nextEvent.asEndElement().getName())
                                .equals(elemName)) {
                fastForward(eventReader);
                return;
            }
        }
    }
}
