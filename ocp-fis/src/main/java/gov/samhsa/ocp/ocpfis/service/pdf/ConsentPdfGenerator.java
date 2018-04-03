package gov.samhsa.ocp.ocpfis.service.pdf;

import gov.samhsa.c2s.common.pdfbox.enhance.HexPdf;

public interface ConsentPdfGenerator {
    String getConsentTitle(String pdfType);

    void drawConsentTitle(HexPdf document, String consentTitle);

    void setPageFooter(HexPdf document, String consentTitle);

}
