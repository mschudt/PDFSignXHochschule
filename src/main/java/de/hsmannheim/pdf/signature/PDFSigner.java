package de.hsmannheim.pdf.signature;

import de.hsmannheim.pdf.signature.examples.signature.CreateSignature;
import de.hsmannheim.pdf.signature.examples.signature.validation.AddValidationInformation;
import org.apache.pdfbox.pdmodel.encryption.SecurityProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;

public class PDFSigner {
    private final SignatureConfig signatureConfig;

    public PDFSigner(SignatureConfig signatureConfig) {
        this.signatureConfig = signatureConfig;
    }

    public void sign(String pdfInputFilename,
                     String pdfOutputFilename,
                     String pkcs12Filename,
                     String password) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keystore = KeyStore.getInstance("PKCS12");

        keystore.load(new FileInputStream(pkcs12Filename), password.toCharArray());

        CreateSignature signing = new CreateSignature(keystore, password.toCharArray());

        File inFile = new File(pdfInputFilename);
        String name = inFile.getName();
        String substring = name.substring(0, name.lastIndexOf('.'));

        File tmpOutFile = new File(inFile.getParent(), substring + "_tmp.pdf");

        signing.signDetached(inFile, tmpOutFile, signatureConfig);

        File signedOutFile;
        if (pdfOutputFilename == null) {
            signedOutFile = new File(inFile.getParent(), substring + "_signed.pdf");
        } else {
            signedOutFile = new File(pdfOutputFilename);
        }

        // Add Long Term Validation data to PDF/A file.
        Security.addProvider(SecurityProvider.getProvider());
        AddValidationInformation addOcspInformation = new AddValidationInformation();
        addOcspInformation.validateSignature(tmpOutFile, signedOutFile);

        // Cleanup tmp file.
        Files.delete(Paths.get(tmpOutFile.getAbsolutePath()));
    }

}
