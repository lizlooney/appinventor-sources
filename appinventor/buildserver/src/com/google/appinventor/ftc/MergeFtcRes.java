package com.google.appinventor.ftc;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource; 
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class MergeFtcRes {
  public static void main(String[] args) {
    try {
      String output = args[0];
      File outputFile = new File(output);

      File[] inputFiles = new File[args.length - 1];
      for (int i = 0; i < args.length - 1; i++) {
        String input = args[i + 1];
        inputFiles[i] = new File(input);
        if (!inputFiles[i].exists()) {
          throw new RuntimeException("The input file " + input + " does not exist.");
        }
      }

      mergeFiles(inputFiles, outputFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void mergeFiles(File[] inputFiles, File outputFile) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    Node firstResourcesNode = null;
    Map<String, Map<String, Node>> items = new TreeMap<String, Map<String, Node>>();

    // Read in multiple documents.
    for (File inputFile : inputFiles) {
      Document document = documentBuilderFactory.newDocumentBuilder().parse(inputFile);
      Node resourcesNode = getResourcesNode(document);
      if (firstResourcesNode == null) {
        firstResourcesNode = resourcesNode;
      }
      collectItems(resourcesNode, items);
    }

    // Create a new document.
    Document newDocument = documentBuilderFactory.newDocumentBuilder().newDocument();
    if (firstResourcesNode != null) {
      Node newResourcesNode = newDocument.appendChild(newDocument.adoptNode(firstResourcesNode.cloneNode(false)));
      for (String tagName : items.keySet()) {
        for (Map.Entry<String, Node> entry : items.get(tagName).entrySet()) {
          // String name = entry.getKey();
          Node node = entry.getValue();
          newResourcesNode.appendChild(newDocument.adoptNode(node));
        }
      }
    }

    // Create a new document.
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.transform(
        new DOMSource(newDocument),
        new StreamResult(outputFile));
  }

  private static Node getResourcesNode(Document document) throws Exception {
    NodeList resources = document.getElementsByTagName("resources");
    if (resources.getLength() != 1) {
      throw new IllegalStateException(
          "Error: resources.getLength() is " + resources.getLength() + ". Expected 1.");
    }
    return resources.item(0);
  }

  private static void collectItems(Node resourcesNode, Map<String, Map<String, Node>> items)
      throws Exception {
    NodeList nodeList = resourcesNode.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node instanceof Element) {
        Element element = (Element) node;
        String tagName = element.getNodeName();
        String name = element.getAttribute("name");

        Map<String, Node> nameToNode = items.get(tagName);
        if (nameToNode == null) {
          nameToNode = new TreeMap<String, Node>();
          items.put(tagName, nameToNode);
        }
        if (!nameToNode.containsKey(name)) {
          nameToNode.put(name, node);
          //System.out.println("Adding " + tagName + " named \"" + name + "\"");
        } else {
          //System.out.println("Skipping " + tagName + " named \"" + name + "\"");
        }
      }
    }
  }
}
