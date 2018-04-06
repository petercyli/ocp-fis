package gov.samhsa.ocp.ocpfis.service.pdf;

import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;

import java.io.IOException;

public interface ConsentPdfGenerator {
    String getConsentTitle(String pdfType);

    void drawConsentTitle(HexPDF document, String consentTitle);

    void setPageFooter(HexPDF document, String consentTitle);

    void drawPatientInformationSection(HexPDF document, ConsentDto consent);

    byte[] generateConsentPdf(ConsentDto consent) throws IOException;

}
