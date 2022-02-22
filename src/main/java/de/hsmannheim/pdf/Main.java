package de.hsmannheim.pdf;

import com.moandjiezana.toml.Toml;
import de.hsmannheim.pdf.pdfa.PDFAFormatValidator;
import de.hsmannheim.pdf.pdfa.ToPDFAConverter;
import de.hsmannheim.pdf.signature.PDFSignatureValidator;
import de.hsmannheim.pdf.signature.PDFSigner;
import de.hsmannheim.pdf.signature.SignatureConfig;
import de.hsmannheim.pdf.xhochschule.HochschulabschlusszeugnisData;
import de.hsmannheim.pdf.xhochschule.XHochschuleGenerator;
import de.hsmannheim.pdf.xhochschule.XHochschuleValidator;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        new Main().doMain(args);
    }

    private void printUsage() {
        System.err.println("Usage: java -jar PDFSignXHochschule.jar exportDir=student_123456");
    }

    public void doMain(String[] args) {
        Stream<String> argsStream = Arrays.stream(args);

        Optional<String> exportDirOption = argsStream.filter((arg) -> arg.startsWith("exportDir=")).findFirst();

        if (exportDirOption.isEmpty()) {
            printUsage();
            System.exit(1);
        }


        String workingDir = System.getProperty("user.dir") + File.separator;
        String filesDir = workingDir + File.separator + "files" + File.separator;
        String configDir = filesDir + File.separator + "config" + File.separator;
        String exportDir = workingDir + exportDirOption.get().split("=")[1] + File.separator;

        Toml tomlGeneralConfig = new Toml().read(new File(configDir + "general_config.toml"));

        boolean runConvertDocxToPdfaStep = tomlGeneralConfig.getBoolean("runConvertDocxToPdfaStep");
        boolean runSignPdfaStep = tomlGeneralConfig.getBoolean("runSignPdfaStep");
        boolean runValidateSignatureStep = tomlGeneralConfig.getBoolean("runValidateSignatureStep");
        boolean runValidatePdfaStep = tomlGeneralConfig.getBoolean("runValidatePdfaStep");
        boolean runGenerateXHochschuleStep = tomlGeneralConfig.getBoolean("runGenerateXHochschuleStep");
        boolean runValidateXHochschuleStep = tomlGeneralConfig.getBoolean("runValidateXHochschuleStep");


        String generatedPdfaFile = exportDir + tomlGeneralConfig.getString("generatedPdfaFile");
        String docxInputFile = workingDir + tomlGeneralConfig.getString("docxInputFile");

        if (runConvertDocxToPdfaStep) {
            // Convert DOCX to PDF/A-2B.
            ToPDFAConverter toPdfaConverter = new ToPDFAConverter();

            boolean success = toPdfaConverter.fileToPdfa(docxInputFile, generatedPdfaFile);

            if (success) {
                System.out.println("✅ Successfully converted DOCX file " + docxInputFile + " to PDF/A-2B file at " + generatedPdfaFile);
            } else {
                System.out.println("❌ Error while converting DOCX file " + docxInputFile + " to PDF/A-2B file at " + generatedPdfaFile);
                System.exit(1);
            }
        }

        String signedPdfaPath = exportDir + tomlGeneralConfig.getString("signedPdfaFileName");

        if (runSignPdfaStep) {
            Toml tomlSignatureConfig = new Toml().read(new File(configDir + "signature_config.toml"));
            SignatureConfig signatureConfig = tomlSignatureConfig.to(SignatureConfig.class);
            PDFSigner pdfSigner = new PDFSigner(signatureConfig);

            String keystoreFile = workingDir + tomlGeneralConfig.getString("keystoreFile");
            String keystorePassword = tomlGeneralConfig.getString("keystorePassword");

            try {
                pdfSigner.sign(generatedPdfaFile, signedPdfaPath, keystoreFile, keystorePassword);
                System.out.println("✅ Signed PDF/A-2B file at " + generatedPdfaFile + " with PKCS#12 archive " + keystoreFile);

            } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException |
                    UnrecoverableKeyException e) {
                e.printStackTrace();
                System.out.println("❌ Error while signing PDF/A-2B file at " + generatedPdfaFile + " with PKCS#12 archive " + keystoreFile);

                System.exit(1);
            }

            boolean deleteGeneratedUnsignedPdfaFile = tomlGeneralConfig.getBoolean("deleteGeneratedUnsignedPdfaFile");
            if (deleteGeneratedUnsignedPdfaFile) {
                // Cleanup unsigned pdfa file.
                try {
                    Files.delete(Paths.get(generatedPdfaFile));
                } catch (IOException ignored) {
                }
            }
        }

        if (runValidateSignatureStep) {
            String caCertificatesDirectory = workingDir + tomlGeneralConfig.getString("caCertificatesDirectory");
            String signingCertificatePath = workingDir + tomlGeneralConfig.getString("signingCertificateFile");


            PDFSignatureValidator pdfSignatureValidator = new PDFSignatureValidator(caCertificatesDirectory);

            boolean isValid = pdfSignatureValidator.validate(signedPdfaPath, signingCertificatePath);

            if (isValid) {
                System.out.println("✅ All signatures of file " + signedPdfaPath + " are valid.");
            } else {
                System.out.println("❌ At least one signature of file " + signedPdfaPath + " could not be validated.");

                System.exit(1);
            }
        }

        if (runValidatePdfaStep) {
            String veraPdfExecutablePath = tomlGeneralConfig.getString("verapdfExecutable");

            PDFAFormatValidator pdfaFormatValidator = new PDFAFormatValidator(veraPdfExecutablePath);

            boolean isValid;
            try {
                isValid = pdfaFormatValidator.validatePdfa(signedPdfaPath);
            } catch (IOException e) {
                e.printStackTrace();
                isValid = false;
            }

            if (isValid) {
                System.out.println("✅ The file " + signedPdfaPath + " is a valid PDF/A-2b file.");
            } else {
                System.out.println("❌ The file " + signedPdfaPath + " is NOT a valid PDF/A-2b file.");

                System.exit(1);
            }
        }

        String[] xhochschuleElementTypes = {"hochschulabschlusszeugnis",
        };

        if (runGenerateXHochschuleStep) {
            for (String type : xhochschuleElementTypes) {
                String hochschulabschlusszeugnisTemplateFile = tomlGeneralConfig.getString("hochschulabschlusszeugnisTemplateFile");

                XHochschuleGenerator xHochschuleGenerator = new XHochschuleGenerator(type, hochschulabschlusszeugnisTemplateFile);

                HochschulabschlusszeugnisData hochschulabschlusszeugnisData = new HochschulabschlusszeugnisData();

                String generatedXmlFilePath = exportDir + type + ".xml";
                String xhochschuleLanguageCode = tomlGeneralConfig.getString("xhochschuleDocumentLanguageCode");
                try {
                    xHochschuleGenerator.saveFilledOutXmlDocument(hochschulabschlusszeugnisData, xhochschuleLanguageCode, generatedXmlFilePath);
                    System.out.println("✅ Generated and saved " + type + " XHochschule document.");
                } catch (XPathExpressionException | SAXException | TransformerException | IOException | ParserConfigurationException | IllegalAccessException e) {
                    e.printStackTrace();
                    System.out.println("❌ Could not generate and save " + type + " XHochschule document.");
                    System.exit(1);
                }


                if (runValidateXHochschuleStep) {
                    String xhochschulePath = tomlGeneralConfig.getString("xhochschuleSchema");
                    XHochschuleValidator xHochschuleValidator = new XHochschuleValidator(xhochschulePath);

                    boolean isValid;
                    try {
                        isValid = xHochschuleValidator.validateXmlFile(generatedXmlFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        isValid = false;
                    }

                    if (isValid) {
                        System.out.println("✅ Successfully validated " + type + " XHochschule document.");
                    } else {
                        System.out.println("❌ " + type + "is NOT a valid XHochschule document.");

                        System.exit(1);
                    }
                }

            }

        }


    }

}
