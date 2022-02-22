/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.hsmannheim.pdf.signature.examples.signature;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Calendar;

import de.hsmannheim.pdf.signature.SignatureConfig;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

/**
 * An example for signing a PDF with bouncy castle.
 * A keystore can be created with the java keytool, for example:
 * <p>
 * {@code keytool -genkeypair -storepass 123456 -storetype pkcs12 -alias test -validity 365
 * -v -keyalg RSA -keystore keystore.p12 }
 *
 * @author Thomas Chojecki
 * @author Vakhtang Koroghlishvili
 * @author John Hewson
 */
public class CreateSignature extends CreateSignatureBase {

    /**
     * Initialize the signature creator with a keystore and certificate password.
     *
     * @param keystore the pkcs12 keystore containing the signing certificate
     * @param pin      the password for recovering the key
     * @throws KeyStoreException         if the keystore has not been initialized (loaded)
     * @throws NoSuchAlgorithmException  if the algorithm for recovering the key cannot be found
     * @throws UnrecoverableKeyException if the given password is wrong
     * @throws CertificateException      if the certificate is not valid as signing time
     * @throws IOException               if no certificate could be found
     */
    public CreateSignature(KeyStore keystore, char[] pin)
            throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException {
        super(keystore, pin);
    }


    public void signDetached(File inFile, File outFile, SignatureConfig signatureConfig) throws IOException {
        if (inFile == null || !inFile.exists()) {
            throw new FileNotFoundException("Document for signing does not exist");
        }

        setTsaUrl(signatureConfig.timestampAuthorityUrl);

        FileOutputStream fos = new FileOutputStream(outFile);

        // sign
        PDDocument doc = null;
        try {
            doc = PDDocument.load(inFile);

            signDetached(doc, fos, signatureConfig);
        } finally {
            IOUtils.closeQuietly(doc);
            IOUtils.closeQuietly(fos);
        }
    }

    public void signDetached(PDDocument document, OutputStream output, SignatureConfig signatureConfig)
            throws IOException {
        int accessPermissions = SigUtils.getMDPPermission(document);
        if (accessPermissions == 1) {
            throw new IllegalStateException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
        }

        // create signature dictionary
        PDSignature signature = new PDSignature();

        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

        signature.setName(signatureConfig.name);
        signature.setLocation(signatureConfig.location);
        signature.setReason(signatureConfig.reason);
        signature.setContactInfo(signatureConfig.contactInfo);

        // the signing date, needed for valid signature
        signature.setSignDate(Calendar.getInstance());

        // Optional: certify
        if (accessPermissions == 0) {
            // MDPPermissions = 1 means "No changes to the document are permitted; any change to the docu-ment invalidates the signature."
            // See: http://www.verypdf.com/document/pdf-format-reference/pg_0733.htm
            SigUtils.setMDPPermission(document, signature, 1);
        }

        if (isExternalSigning()) {
            document.addSignature(signature);
            ExternalSigningSupport externalSigning =
                    document.saveIncrementalForExternalSigning(output);
            // invoke external signature service
            byte[] cmsSignature = sign(externalSigning.getContent());
            // set signature bytes received from the service
            externalSigning.setSignature(cmsSignature);
        } else {
            SignatureOptions signatureOptions = new SignatureOptions();

            // Size can vary, but should be enough for purpose.
            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);

            // register signature dictionary and sign interface
            document.addSignature(signature, this, signatureOptions);

            // write incremental (only for signing purpose)
            document.saveIncremental(output);
        }
    }

}
