/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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

package org.javagi.util;

import org.javagi.configuration.ModuleInfo;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple parser for the ModuleData.xml configuration file.
 */
public class ModuleDataParser {
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private static String getAttr(StartElement elem, String name) {
        return elem.getAttributeByName(new QName(name)).getValue();
    }

    /**
     * Parse the ModuleData.xml configuration file.
     *
     * @return the parsed configuration data
     */
    public Map<String, ModuleInfo.Module> parse() {
        try (InputStream stream = ModuleDataParser.class.getResourceAsStream("/ModuleData.xml")) {
            XMLEventReader eventReader = XML_INPUT_FACTORY.createXMLEventReader(stream);
            return parse(eventReader);
        } catch (IOException | XMLStreamException e) {
            // Don't try to handle exceptions here. A corrupt or missing
            // ModuleData.xml file should bail out immediately.
            throw new AssertionError(e);
        }
    }

    private Map<String, ModuleInfo.Module> parse(XMLEventReader eventReader) throws XMLStreamException {
        Map<String, ModuleInfo.Module> entries = new HashMap<>();
        String name = "";
        String moduleName = "";
        String javaPackage = "";
        String javaModule = "";
        String mavenName = "";
        String docUrlPrefix = "";
        StringBuilder text = new StringBuilder();

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                text = new StringBuilder();

                switch (startElement.getName().getLocalPart()) {
                    case "module" -> {
                        javaModule = getAttr(startElement, "name");
                        mavenName = getAttr(startElement, "maven-module");
                    }
                    case "repository" -> {
                        name = getAttr(startElement, "name");
                        moduleName = getAttr(startElement, "repository-name");
                        javaPackage = getAttr(startElement, "java-package");
                        docUrlPrefix = getAttr(startElement, "doc-url-prefix");
                    }
                }
            }

            else if (event.isCharacters()) {
                text.append(event.asCharacters().getData().trim());
            }

            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (endElement.getName().getLocalPart().equals("repository")) {
                    var description = text.toString();
                    ModuleInfo.Module module = new ModuleInfo.Module(
                            moduleName, javaPackage, javaModule, mavenName, docUrlPrefix, description);
                    entries.put(name, module);
                }
            }
        }

        return entries;
    }
}
