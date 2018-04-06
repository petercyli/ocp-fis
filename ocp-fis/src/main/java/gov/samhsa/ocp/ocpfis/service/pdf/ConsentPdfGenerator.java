package gov.samhsa.ocp.ocpfis.service.pdf;

import gov.samhsa.c2s.common.pdfbox.enhance.HexPdf;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;

import java.io.IOException;

public interface ConsentPdfGenerator {
    String getConsentTitle(String pdfType);

    void drawConsentTitle(HexPdf document, String consentTitle);

    void setPageFooter(HexPdf document, String consentTitle);

    void drawPatientInformationSection(HexPdf document, ConsentDto consent);

    byte[] generateConsentPdf(ConsentDto consent) throws IOException;

}
