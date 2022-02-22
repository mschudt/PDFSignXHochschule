package de.hsmannheim.pdf.xhochschule;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class XHochschuleGenerator {

    private final String xHochschuleDocumentType;
    private final String xHochschuleTemplatePath;

    public XHochschuleGenerator(String xHochschuleElementType, String xHochschuleTemplatePath) {
        this.xHochschuleDocumentType = xHochschuleElementType;
        this.xHochschuleTemplatePath = xHochschuleTemplatePath;
    }

    public String getFilledOutXmlDocumentAsString(Object data, String languageCode) throws XPathExpressionException, IllegalAccessException, ParserConfigurationException, IOException, SAXException, TransformerException {
        Document document = loadNewTemplateDocument();

        // Set all empty lang attributes (lang="") in the document to the languageCode.
        setAllEmptyAttributes(document, "lang", languageCode);

        // Reflect all fields of class.
        Field[] fields = data.getClass().getDeclaredFields();

        for (Field field : fields) {
            setNodeContentByConfigField(document, data, field);
        }

        return documentToXmlString(document);
    }

    public void saveFilledOutXmlDocument(Object data, String languageCode, String xmlOutputPath) throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, IllegalAccessException, SAXException {
        String filledOutXmlString = getFilledOutXmlDocumentAsString(data, languageCode);

        // Make sure that this dir exists.
        new File(xmlOutputPath).getParentFile().mkdirs();

        Files.writeString(Paths.get(xmlOutputPath), filledOutXmlString);
    }

    private Document loadNewTemplateDocument() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(Files.newInputStream(Paths.get(xHochschuleTemplatePath)));

        document.setXmlStandalone(true);

        return document;
    }

    private void setAllEmptyAttributes(Document doc, String attributeName, String value) {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xPath = xpathFactory.newXPath();

        String nodeWithEmptyAttributeExpression = "//*[@" + attributeName + "='']";
        try {
            NodeList nodes = (NodeList) xPath.compile(nodeWithEmptyAttributeExpression).evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);

                // Set attribute of node = value if it contains contains attributeName in its name.
                // Use contains to set every attribute = value, no matter which prefix (namespace) it has.
                NamedNodeMap attributes = node.getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    Node attributeNode = attributes.item(j);
                    if (attributeNode.getNodeName().contains(attributeName)) {
                        attributeNode.setNodeValue(value);
                    }
                }

            }

        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    private void setNodeContentByConfigField(Document doc, Object config, Field field) throws IllegalAccessException, XPathExpressionException {
        String fieldValue = (String) field.get(config);

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        // XPath is generated from the fields name in the config.
        // Example: config.studierender_geburt_datum -> studierender/geburt/datum
        String xPathString = "/" + xHochschuleDocumentType + "/" + field.getName().replaceAll("_", "/");

        XPathExpression nodeAtPathExpression = xpath.compile(xPathString);
        Node node = (Node) nodeAtPathExpression.evaluate(doc, XPathConstants.NODE);

        node.setTextContent(fieldValue);
    }

    private String documentToXmlString(Document document) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        StringWriter writer = new StringWriter();

        Transformer transformer = tf.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(writer));

        return writer.getBuffer().toString();
    }

    public static void main(String[] args) {
        String xHochschuleElementType = "hochschulabschlusszeugnis";
        String languageCode = "de"; // en-US
        String templateFilePath = "/Users/markus/Desktop/Bachelorarbeit/java/files/hochschulabschlusszeugnis_template.xml";
        String generatedXmlFilePath = "/Users/markus/Desktop/Bachelorarbeit/java/files/hochschulabschlusszeugnis.xml";

        try {
            XHochschuleGenerator xHochschuleGenerator = new XHochschuleGenerator(xHochschuleElementType, templateFilePath);

            HochschulabschlusszeugnisData data = new HochschulabschlusszeugnisData();
            String hochschulabschlusszeugnisXmlString = xHochschuleGenerator.getFilledOutXmlDocumentAsString(data, languageCode);

            System.out.println(hochschulabschlusszeugnisXmlString);

            xHochschuleGenerator.saveFilledOutXmlDocument(data, languageCode, generatedXmlFilePath);

        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException | IllegalAccessException | TransformerException e) {
            e.printStackTrace();
        }

    }


}
