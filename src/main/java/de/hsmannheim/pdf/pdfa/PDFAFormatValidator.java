package de.hsmannheim.pdf.pdfa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class PDFAFormatValidator {
    private final String veraPdfExecutablePath;

    public PDFAFormatValidator(String veraPdfExecutablePath) {
        this.veraPdfExecutablePath = veraPdfExecutablePath;
    }

    // Not configurable at the moment.
    public static final String PDFA_FLAVOUR = "2b";

    public boolean validatePdfa(String pdfaInputFile) throws IOException {
        String command = veraPdfExecutablePath + " --flavour " + PDFA_FLAVOUR + " " + pdfaInputFile;

        String output;
        try (
                InputStream inputStream = Runtime.getRuntime().exec(command).getInputStream();
                Scanner s = new Scanner(inputStream).useDelimiter("\\A")
        ) {
            output = s.hasNext() ? s.next() : null;
        }

        if (output == null) {
            return false;
        }

        boolean isCompliant = output.contains("isCompliant=\"true\"");

        if (!isCompliant) {
            System.err.println(output);
        }

        return isCompliant;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        PDFAFormatValidator pdfaFormatValidator = new PDFAFormatValidator("/Users/markus/verapdf/verapdf");

        boolean isValid = pdfaFormatValidator.validatePdfa("/Users/markus/Desktop/Bachelorarbeit/java/files/zeugnis_pdfa.pdf");

        System.out.println("isValid? " + isValid);
    }

}
