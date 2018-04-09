package gov.samhsa.ocp.ocpfis.service.pdf;

import gov.samhsa.c2s.common.pdfbox.enhance.pdfbox.util.PdfBoxHandler;
import gov.samhsa.ocp.ocpfis.config.PdfProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.PdfConfigMissingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConsentPdfGeneratorWithHexPdfImpl implements ConsentPdfGenerator {
    private static final String DATE_FORMAT_PATTERN = "MMM dd, yyyy";
    private static final String CONSENT_PDF = "consent-pdf";
    private static final String TELECOM_EMAIL = "EMAIL";
    private  static final String userNameKey = "ATTESTER_FULL_NAME";

    private  static final String CONSENT_TERM = "I, " + userNameKey +", understand that my records are protected under the federal regulations governing Confidentiality of"
    +" Alcohol and Drug Abuse Patient Records, 42 CFR part 2, and cannot be disclosed without my written"
    +" permission or as otherwise permitted by 42 CFR part 2. I also understand that I may revoke this consent at any"
    +" time except to the extent that action has been taken in reliance on it, and that any event this consent expires"
    +" automatically as follows:";

    private static final String NEWLINE_CHARACTER = "\n";
    private static final String NEWLINE_AND_LIST_PREFIX = "\n- ";

    private final PdfProperties pdfProperties;

    @Autowired
    public ConsentPdfGeneratorWithHexPdfImpl(PdfProperties pdfProperties) {
        this.pdfProperties = pdfProperties;
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
    public void setPageFooter(HexPDF document, String consentTitle) {
        document.setFooter(Footer.defaultFooter);
        // Change center text in footer
        document.getFooter().setCenterText(consentTitle);
        // Use footer also on first page
        document.getFooter().setOMIT_FIRSTPAGE(false);
    }

    public void drawConsentTitle(HexPDF document, String consentTitle) {
        // Add a main title, centered in shiny colours
        document.title1Style();
        document.setTextColor(Color.black);
        document.drawText(consentTitle + NEWLINE_CHARACTER, HexPDF.CENTER);
    }

    @Override
    public void drawPatientInformationSection(HexPDF document, ConsentDto consentdto) {
        String patientFullName = consentdto.getPatient().getDisplay();
        //String patientBirthDate = PdfBoxHandler.formatLocalDate(patientDto.getBirthDate(), DATE_FORMAT_PATTERN);

        Object[][] patientInfo = {
                {NEWLINE_CHARACTER + "Consent Reference Number: " + consentdto.getLogicalId(), null},
                {NEWLINE_CHARACTER + "Patient Name: " + patientFullName, NEWLINE_CHARACTER}
        };
        float[] patientInfoTableColumnWidth = new float[]{240, 240};
        int[] patientInfoTableColumnAlignment = new int[]{HexPDF.LEFT, HexPDF.LEFT};

        document.drawTable(patientInfo,
                patientInfoTableColumnWidth,
                patientInfoTableColumnAlignment,
                HexPDF.LEFT);

    }


    private void drawAuthorizeToDiscloseSectionTitle(HexPDF document, ConsentDto consentDto) {
        Object[][] title = {
                {"AUTHORIZATION TO DISCLOSE"}
        };
        float[] AuthorizationTitleTableColumnWidth = new float[]{480};
        int[] AuthorizationTitleTableColumnAlignment = new int[]{HexPDF.LEFT};
        document.drawTable(title,
                AuthorizationTitleTableColumnWidth,
                AuthorizationTitleTableColumnAlignment,
                HexPDF.LEFT);
        drawAuthorizationSubSectionHeader(document, NEWLINE_CHARACTER + "Authorizes:" + NEWLINE_CHARACTER);

        if (consentDto.isGeneralDesignation() ) {
            drawTableWithGeneralDesignation(document, consentDto);
        }

        if (consentDto.getFromActor() != null)
            drawActorsTable(document, consentDto.getFromActor());

        drawAuthorizationSubSectionHeader(document, NEWLINE_CHARACTER + "To disclose to:" + NEWLINE_CHARACTER);

        if (consentDto.getToActor() != null)
            drawActorsTable(document, consentDto.getToActor());

    }

    private void drawActorsTable(HexPDF document, List<ReferenceDto> actors) {

        Object[][] tableContentsForPractitioners = new Object[actors.size() + 1][2];
        tableContentsForPractitioners[0][0] = " Name";
        tableContentsForPractitioners[0][1] = "Reference";

        for (int i = 0; i < actors.size(); i++) {
            tableContentsForPractitioners[i + 1][0] = actors.get(i).getDisplay();
            tableContentsForPractitioners[i + 1][1] = actors.get(i).getReference();
        }

        float[] actorTableColumnWidth = new float[]{240, 240};
        int[] providerTableColumnAlignment = new int[]{HexPDF.LEFT, HexPDF.LEFT};

        if (actors.size() > 0)
            document.drawTable(tableContentsForPractitioners,
                    actorTableColumnWidth,
                    providerTableColumnAlignment,
                    HexPDF.LEFT);
    }


    private void drawAuthorizationSubSectionHeader(HexPDF document, String header) {
        document.title2Style();
        document.drawText(header);
        document.normalStyle();
    }

    private void drawTableWithGeneralDesignation(HexPDF document, ConsentDto consentDto) {
        if (consentDto.isGeneralDesignation()) {
            float[] GeneralDesignationTableColumnWidth = new float[]{480};
            int[] GeneralDesignationTableColumnAlignment = new int[]{HexPDF.LEFT};
            Object[][] generalDesignationText = {{"General Designation Consent"}};
            document.drawTable(generalDesignationText,
                    GeneralDesignationTableColumnWidth,
                    GeneralDesignationTableColumnAlignment,
                    HexPDF.LEFT);
        }

    }

    @Override
    public byte[] generateConsentPdf(ConsentDto consent) throws IOException {
        Assert.notNull(consent, "Consent is required.");

        String consentTitle = getConsentTitle(CONSENT_PDF);

        HexPDF document = new HexPDF();

        setPageFooter(document, "");

        // Create the first page
        document.newPage();

        // Set document title
        drawConsentTitle(document, consentTitle);

        // Typeset everything else in boring black
        document.setTextColor(Color.black);

        document.normalStyle();

        drawPatientInformationSection(document, consent);

        drawAuthorizeToDiscloseSectionTitle(document, consent);

        drawHealthInformationToBeDisclosedSection(document, consent);

        drawConsentTermsSection(document,consent);

        drawEffectiveAndExspireDateSection(document, consent);

        // Get the document
        return document.getDocumentAsBytArray();
    }

    private void drawHealthInformationToBeDisclosedSection(HexPDF document, ConsentDto consentDto) {
        document.drawText(NEWLINE_CHARACTER);

        Object[][] title = {
                {"HEALTH INFORMATION TO BE DISCLOSED"}
        };
        float[] healthInformationTaleWidth = new float[]{480};
        int[] healthInformationTaleAlignment = new int[]{HexPDF.LEFT};
        document.drawTable(title,
                healthInformationTaleWidth,
                healthInformationTaleAlignment,
                HexPDF.LEFT);

        String sensitivityCategoriesLabel = "To SHARE the following medical information:";
        String subLabel = "Sensitivity Categories:";
        String sensitivityCategories = consentDto.getCategory().stream()
                                       .map(valueSet -> valueSet.getDisplay()).collect(Collectors.joining(NEWLINE_AND_LIST_PREFIX));

        String sensitivityCategoriesStr = sensitivityCategoriesLabel
                .concat(NEWLINE_CHARACTER).concat(subLabel)
                .concat(NEWLINE_AND_LIST_PREFIX).concat(sensitivityCategories);

        String purposeLabel = "To SHARE for the following purpose(s):";

        String purposes = consentDto.getPurpose().stream()
                .map(valueSet -> valueSet.getDisplay()).collect(Collectors.joining(NEWLINE_AND_LIST_PREFIX));
        String purposeOfUseStr = purposeLabel.concat(NEWLINE_AND_LIST_PREFIX).concat(purposes);

        Object[][] healthInformationHeaders = {
                {sensitivityCategoriesStr, purposeOfUseStr}
        };
        float[] healthInformationTableColumnWidth = new float[]{240, 240};
        int[] healthInformationTableColumnAlignment = new int[]{HexPDF.LEFT, HexPDF.LEFT};

        document.drawTable(healthInformationHeaders,
                healthInformationTableColumnWidth,
                healthInformationTableColumnAlignment,
                HexPDF.LEFT);
    }

    private void drawConsentTermsSection(HexPDF document, ConsentDto consentDto) {

        Object[][] title = {
                {"CONSENT TERMS"}
        };
        float[] consentTermsColumnWidth = new float[]{480};
        int[] consentTermsColumnAlignment = new int[]{HexPDF.LEFT};
        document.drawTable(title,
                consentTermsColumnWidth,
                consentTermsColumnAlignment,
                HexPDF.LEFT);

        String termsWithAttestedName = CONSENT_TERM.replace(userNameKey, consentDto.getPatient().getDisplay().toUpperCase());

        document.drawText(termsWithAttestedName);
    }

    private void drawEffectiveAndExspireDateSection(HexPDF document, ConsentDto consent) {
        // Prepare table content
        String effectiveDateContent = "Effective Date: ".concat(PdfBoxHandler.formatLocalDate(consent.getPeriod().getStart(), DATE_FORMAT_PATTERN));
        String expirationDateContent = "Expiration Date: ".concat(PdfBoxHandler.formatLocalDate(consent.getPeriod().getEnd(), DATE_FORMAT_PATTERN));

        Object[][] title = {
                {effectiveDateContent, expirationDateContent}
        };
        document.drawText(NEWLINE_CHARACTER);
        document.drawText(NEWLINE_CHARACTER);

        float[] consentDurationTableColumnWidth = new float[]{240, 240};
        int[] consentDurationTableColumnAlignment = new int[]{HexPDF.LEFT, HexPDF.LEFT};
        document.drawTable(title,
                consentDurationTableColumnWidth,
                consentDurationTableColumnAlignment,
                HexPDF.LEFT);
    }

}
