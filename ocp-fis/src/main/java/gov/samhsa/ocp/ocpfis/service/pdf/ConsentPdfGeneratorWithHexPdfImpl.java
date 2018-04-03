package gov.samhsa.ocp.ocpfis.service.pdf;

import gov.samhsa.c2s.common.pdfbox.enhance.Footer;
import gov.samhsa.c2s.common.pdfbox.enhance.HexPdf;
import gov.samhsa.ocp.ocpfis.config.PdfProperties;
import gov.samhsa.ocp.ocpfis.service.exception.PdfConfigMissingException;
import org.springframework.beans.factory.annotation.Autowired;

public class ConsentPdfGeneratorWithHexPdfImpl implements ConsentPdfGenerator{
    private static final String DATE_FORMAT_PATTERN = "MMM dd, yyyy";
    private static final String CONSENT_PDF = "consent-pdf";
    private static final String TELECOM_EMAIL = "EMAIL";

    private static final String NEWLINE_CHARACTER = "\n";
    private static final String NEWLINE_AND_LIST_PREFIX = "\n- ";

    private final PdfProperties pdfProperties;

    private HexPdf document;


    @Autowired
    public ConsentPdfGeneratorWithHexPdfImpl(PdfProperties pdfProperties) {
        this.pdfProperties = pdfProperties;
    }

    private static String filterNullAddressValue(String value) {
        final String commaPattern = ", ";
        if (value == null) {
            return "";
        } else {
            return commaPattern.concat(value);
        }
    }

    @Override
    public String getConsentTitle(String pdfType) {
        return pdfProperties.getPdfConfigs().stream()
                .filter(pdfConfig -> pdfConfig.type.equalsIgnoreCase(pdfType))
                .map(PdfProperties.PdfConfig::getTitle)
                .findAny()
                .orElseThrow(PdfConfigMissingException::new);
    }

    @Override
    public void setPageFooter(HexPdf document, String consentTitle) {
        document.setFooter(Footer.noFooter);
        // Change center text in footer
        document.getFooter().setCenterText(consentTitle);
        // Use footer also on first page
        document.getFooter().setOMIT_FIRSTPAGE(false);
    }

    public void drawConsentTitle(HexPdf document, String consentTitle) {
        // Add a main title, centered in shiny colours
        document.title1Style();
        document.drawText(consentTitle + NEWLINE_CHARACTER, HexPdf.CENTER);
    }
}
