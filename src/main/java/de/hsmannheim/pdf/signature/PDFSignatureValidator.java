package de.hsmannheim.pdf.signature;

import eu.europa.esig.dss.detailedreport.DetailedReport;
import eu.europa.esig.dss.detailedreport.jaxb.XmlSignature;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PDFSignatureValidator {
    private final List<CertificateToken> caCertificates;

    public PDFSignatureValidator(String caCertificatesDirectory) {
        List<CertificateToken> caCertificates = new ArrayList<>();

        // Load all CA certificates in caCertificatesDirectory.
        try {
            Stream<Path> paths = Files.walk(Paths.get(caCertificatesDirectory));
            paths
                    .filter(Files::isRegularFile)
                    .forEach((path -> caCertificates.add(DSSUtils.loadCertificate(new File(String.valueOf(path))))));
        } catch (IOException e) {
            System.err.println("Failed reading CA certificates from " + caCertificatesDirectory);
        }

        this.caCertificates = caCertificates;
    }


    public boolean validate(String inputFilePath, String signingCertificatePath) {
        CertificateSource certSource = new CommonCertificateSource();

        // Signing certificate.
        certSource.addCertificate(DSSUtils.loadCertificate(new File(signingCertificatePath)));

        CertificateVerifier cv = new CommonCertificateVerifier();

        // Add CA certificates to trust chain.
        if (caCertificates != null) {
            for (CertificateToken certificate : caCertificates) {
                certSource.addCertificate(certificate);
            }
        }

        CommonTrustedCertificateSource trustedCertSource = new CommonTrustedCertificateSource();

        trustedCertSource.importAsTrusted(certSource);

        cv.addTrustedCertSources(trustedCertSource);

        FileDocument document = new FileDocument(inputFilePath);

        PDFDocumentValidator documentValidator = new PDFDocumentValidator(document);

        documentValidator.setSigningCertificateSource(certSource);

        documentValidator.setCertificateVerifier(cv);

        documentValidator.setTokenExtractionStrategy(TokenExtractionStrategy.EXTRACT_CERTIFICATES_AND_TIMESTAMPS);

        documentValidator.setValidationLevel(ValidationLevel.ARCHIVAL_DATA);

        Reports reports = documentValidator.validateDocument();

        DetailedReport detailedReport = reports.getDetailedReport();

//        System.out.println(reports.getXmlDetailedReport());

        // Make sure that all timestamps are valid (passed or total passed).
        for (String timestampId : detailedReport.getTimestampIds()) {
            Indication timestampIndication = detailedReport.getTimestampValidationIndication(timestampId);

            if (timestampIndication != Indication.TOTAL_PASSED && timestampIndication != Indication.PASSED) {
                return false;
            }
        }

        // Make sure that all signatures are valid (passed or total passed).
        for (XmlSignature signature : detailedReport.getSignatures()) {
            Indication longTermIndication = detailedReport.getLongTermValidationIndication(signature.getId());
            Indication archiveDataIndication = detailedReport.getArchiveDataValidationIndication(signature.getId());

            if ((longTermIndication != Indication.PASSED && longTermIndication != Indication.TOTAL_PASSED) ||
                    (archiveDataIndication != Indication.PASSED && archiveDataIndication != Indication.TOTAL_PASSED)) {
                return false;
            }
        }


        return true;
    }

    public static void main(String[] args) throws IOException {
        String path = "/Users/markus/Desktop/Bachelorarbeit/java/files/zeugnis_pdfa_signed.pdf";

        String caCertificatesDirectory = "/Users/markus/Desktop/Bachelorarbeit/java/files/certificates/ca";
        String signingCertificatePath = "/Users/markus/Desktop/Bachelorarbeit/java/files/certificates/cert.pem";

        PDFSignatureValidator validator = new PDFSignatureValidator(caCertificatesDirectory);

        boolean isSignatureValid = validator.validate(path, signingCertificatePath);

        System.out.println("Is signature valid? " + isSignatureValid);
    }

}
