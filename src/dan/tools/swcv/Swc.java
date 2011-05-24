/*
 * Copyright (C) 2011 by Daniel Anderson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dan.tools.swcv;

import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Represents an Actionscript 3 SWC library.
 * 
 * This class manages extracting exports and dependency information
 * from a SWC's catalog.xml file. The export and dependencies are stored
 * in this class, grouped by package.
 */
public class Swc {
    private Hashtable<String, Package> pacakges;

    public Swc(String swcFile) throws Exception {
        ZipFile zf = null;
        ZipEntry ze;

        pacakges = new Hashtable<String, Package>();

        try {
            zf = new ZipFile(new File(swcFile));
            ze = zf.getEntry("catalog.xml");

            if (ze == null) {
                throw new Exception(swcFile
                        + " does not contain a catalog.xml.");
            }

            processCatalogXml(zf.getInputStream(ze));
        } finally {
            try {
                zf.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public Iterable<Package> packagesIterator() {
        return pacakges.values();
    }

    public Package getPackage(String packageName) {
        return pacakges.get(packageName);
    }

    private Node findChildElement(Node n, String childNodeName) {
        if (n != null && n.hasChildNodes()) {
            NodeList children = n.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                if (child.getNodeType() == Node.ELEMENT_NODE
                        && childNodeName.equals(child.getNodeName())) {
                    return child;
                }
            }
        }

        return null;
    }

    private void processCatalogXml(InputStream inputStream) throws Exception {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputStream);

        Vector<Node> libraryNodes = getLibraryNodes(doc);

        if (libraryNodes.isEmpty()) {
            throw new Exception("Cannot find library information in swc.");
        }

        for (Node library : libraryNodes) {
            processLibraryNode(library);
        }
    }

    private void processLibraryNode(Node library) throws Exception {
        if (!library.hasChildNodes()) {
            return;
        }

        NodeList children = library.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "script".equals(child.getNodeName())) {
                processScriptNode(child);
            }
        }
    }

    private void processScriptNode(Node script) throws Exception {
        if (!script.hasChildNodes()) {
            return;
        }

        NodeList children = script.getChildNodes();
        Symbol def = null;

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);

            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "def".equals(child.getNodeName())) {
                String attr = getAttribute(child, "id");

                if (attr != null) {
                    def = createSymbol(attr);
                } else {
                    throw new Exception("No id attribute for def: "
                            + child.getNodeName());
                }
                break;
            }
        }

        if (def == null) {
            throw new Exception("No def for script: " + script.getNodeName());
        }

        String packageName = def.getPackageName();
        Package pkg = pacakges.get(packageName);

        if (pkg == null) {
            pkg = new Package(def.getPackageName());
            pacakges.put(packageName, pkg);
        }

        pkg.addExport(def);

        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "dep".equals(child.getNodeName())) {
                String attr = getAttribute(child, "id");

                if (attr != null) {
                    pkg.addDependency(createSymbol(attr));
                } else {
                    throw new Exception("No id attribute for def: "
                            + child.getNodeName());
                }
            }
        }
    }

    private Symbol createSymbol(String id) {
        String s[] = id.split(":");
        String name;
        String pkg;

        if (s.length == 1) {
            name = s[0];
            pkg = "default";
        } else if (s.length == 2) {
            pkg = s[0];
            name = s[1];
        } else {
            // error?
            pkg = "";
            name = "";
        }

        return new Symbol(name, pkg);
    }

    private Vector<Node> getLibraryNodes(Document doc) {
        Node swc = findChildElement(doc, "swc");
        Node libraries = findChildElement(swc, "libraries");
        Vector<Node> nodes = new Vector<Node>();

        if (libraries == null || !libraries.hasChildNodes()) {
            return nodes;
        }

        NodeList children = libraries.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "library".equals(child.getNodeName())) {
                nodes.add(child);
            }
        }

        return nodes;
    }

    private String getAttribute(Node n, String attributeName) {
        if (n.hasAttributes()) {
            Node attr = n.getAttributes().getNamedItem(attributeName);

            if (attr != null) {
                return attr.getNodeValue();
            }
        }

        return null;
    }
}
