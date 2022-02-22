package de.hsmannheim.pdf.xhochschule;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class XHochschuleValidator {

    private final String xHochschuleSchemaPath;

    public XHochschuleValidator(String xHochschuleSchemaPath) {
        this.xHochschuleSchemaPath = xHochschuleSchemaPath;
    }

    public boolean validateXmlString(String xmlString) {
        return validateXmlInputStream(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
    }

    public boolean validateXmlFile(String xmlFilePath) throws IOException {
        return validateXmlInputStream(Files.newInputStream(Paths.get(xmlFilePath)));
    }

    private boolean validateXmlInputStream(InputStream inputStream) {
        boolean isValid;

        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(new File(xHochschuleSchemaPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(inputStream));

            isValid = true;
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            isValid = false;
        }

        return isValid;
    }

    public static void main(String[] args) throws IOException {
        String schemaPath = "/Users/markus/Desktop/Bachelorarbeit/java/files/xhochschule/0.7/xhochschule.xsd";
        String xmlInputFilePath = "/Users/markus/Desktop/Bachelorarbeit/java/files/filled_hochschulabschlusszeugnis.xml";

        XHochschuleValidator xHochschuleValidator = new XHochschuleValidator(schemaPath);

        boolean isValid = xHochschuleValidator.validateXmlFile(xmlInputFilePath);

        if (isValid) {
            System.out.println("Valid.");
        } else {
            System.out.println("NOT valid.");
        }
    }

}
