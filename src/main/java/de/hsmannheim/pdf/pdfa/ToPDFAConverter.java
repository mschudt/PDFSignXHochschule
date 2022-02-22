package de.hsmannheim.pdf.pdfa;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ToPDFAConverter {

    /**
     * Converts DOC, DOCX, or regular PDF files to PDF/A by calling unoconv via command line.
     **/
    public boolean fileToPdfa(String docxInputFile, String pdfaOutputFile) {
        // Timeout if converting has not been finished within 30 seconds.
        long timeout = 30 * 1000;
        // PDF/A-1 = "1", PDF/A-2 = "2", PDF/A-3 = "3".
        String pdfaVersion = "2";

        // Generate a valid PDF/A-2b file.
        boolean finishedSuccessfully;
        try {
            Process process = Runtime.getRuntime().exec("unoconv -f pdf -eSelectPdfVersion=" + pdfaVersion + " -o " + pdfaOutputFile + " " + docxInputFile);

            finishedSuccessfully = process.waitFor(timeout, TimeUnit.MILLISECONDS);

        } catch (IOException ex) {
            ex.printStackTrace();
            finishedSuccessfully = false;
        } catch (InterruptedException e) {
            finishedSuccessfully = false;
        }

        return finishedSuccessfully;
    }

}
