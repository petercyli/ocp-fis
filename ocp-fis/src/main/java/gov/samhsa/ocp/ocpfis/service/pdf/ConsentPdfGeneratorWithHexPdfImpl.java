package gov.samhsa.ocp.ocpfis.service.pdf;

import com.google.common.collect.ImmutableMap;
import gov.samhsa.c2s.common.pdfbox.enhance.Footer;
import gov.samhsa.c2s.common.pdfbox.enhance.HexPdf;
import gov.samhsa.c2s.common.pdfbox.enhance.pdfbox.util.PdfBoxHandler;
import gov.samhsa.ocp.ocpfis.config.PdfProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.exception.PdfConfigMissingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.awt.*;
import java.io.IOException;

@Service
@Slf4j
public class ConsentPdfGeneratorWithHexPdfImpl implements ConsentPdfGenerator{
    private static final String DATE_FORMAT_PATTERN = "MMM dd, yyyy";
    private static final String CONSENT_PDF = "consent-pdf";
    private static final String TELECOM_EMAIL = "EMAIL";

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
    public void setPageFooter(HexPdf document, String consentTitle) {
        document.setFooter(Footer.noFooter);
        // Change center text in footer
        document.getFooter().setCenterText(consentTitle);
        // Use footer also on first page
        document.getFooter().setOMIT_FIRSTPAGE(false);
    }

    public void drawConsentTitle(HexPdf document, String consentTitle) {
        // Add a main title, centered in shiny colours
        document.newPage();
        document.title1Style();
        document.drawText(consentTitle + NEWLINE_CHARACTER, HexPdf.CENTER);
    }

    @Override
    public void drawPatientInformationSection(HexPdf document, ConsentDto consentdto) {
        String patientFullName = consentdto.getPatient().getDisplay();
        //String patientBirthDate = PdfBoxHandler.formatLocalDate(patientDto.getBirthDate(), DATE_FORMAT_PATTERN);

        Object[][] patientInfo = {
                {NEWLINE_CHARACTER + "Consent Reference Number: " + consentdto.getLogicalId(), null},
                {NEWLINE_CHARACTER + "Patient Name: " + patientFullName, NEWLINE_CHARACTER}
        };
        float[] patientInfoTableColumnWidth = new float[]{240, 240};
        int[] patientInfoTableColumnAlignment = new int[]{HexPdf.LEFT, HexPdf.LEFT};

        document.drawTable(patientInfo,
                patientInfoTableColumnWidth,
                patientInfoTableColumnAlignment,
                HexPdf.LEFT);

    }


    private void drawAuthorizeToDiscloseSectionTitle(HexPdf document, ConsentDto consent) {
        Object[][] title = {
                {"AUTHORIZATION TO DISCLOSE"}
        };
        float[] AuthorizationTitleTableColumnWidth = new float[]{480};
        int[] AuthorizationTitleTableColumnAlignment = new int[]{HexPdf.LEFT};
        document.drawTable(title,
                AuthorizationTitleTableColumnWidth,
                AuthorizationTitleTableColumnAlignment,
                HexPdf.LEFT);
        drawAuthorizationSubSectionHeader(document, NEWLINE_CHARACTER + "Authorizes:" + NEWLINE_CHARACTER);

        if (consent.isGeneralDesignation() && consent.getFromActor() != null) {
            drawTableWithGeneralDesignation(document, "General Designation Consent");
        }

        drawAuthorizationSubSectionHeader(document, NEWLINE_CHARACTER + "To disclose to:" + NEWLINE_CHARACTER);
        if (consent.isGeneralDesignation() && consent.getToActor() != null) {
            drawTableWithGeneralDesignation(document, "General Designation Consent");
        }
    }

    private void drawAuthorizationSubSectionHeader(HexPdf document, String header) {
        document.title2Style();
        document.drawText(header);
        document.normalStyle();
    }

    private void drawTableWithGeneralDesignation(HexPdf document, String generalDesignation) {
        float[] GeneralDesignationTableColumnWidth = new float[]{240, 240};
        int[] GeneralDesignationTableColumnAlignment = new int[]{HexPdf.LEFT, HexPdf.LEFT};
        Object[][] generalDesignationText = {{generalDesignation, ""}};

        document.drawTable(generalDesignationText,
                GeneralDesignationTableColumnWidth,
                GeneralDesignationTableColumnAlignment,
                HexPdf.LEFT);
    }

    @Override
    public byte[] generateConsentPdf(ConsentDto consent) throws IOException {
        Assert.notNull(consent, "Consent is required.");

        String consentTitle = getConsentTitle(CONSENT_PDF);

        HexPdf document = new HexPdf();
        // TODO fix content in footer issue the set title: consentTitle
//        setPageFooter(document, "");

        // Create the first page
        document.newPage();

        // Set document title
        drawConsentTitle(document, consentTitle);

       // Typeset everything else in boring black
        document.setTextColor(Color.black);

        document.normalStyle();

        drawPatientInformationSection(document, consent);

        drawAuthorizeToDiscloseSectionTitle(document, consent);
//
        //drawHealthInformationToBeDisclosedSection(consent);
//
        //drawConsentTermsSection(consentTerms, patientDto);
//
       drawEffectiveAndExspireDateSection(document, consent);

        try {
            document.save("C:\\Users\\ming.fan\\Desktop\\temp\\HexPDFfile.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the document
        return document.getDocumentAsBytArray();
    }

    /*private void drawHealthInformationToBeDisclosedSection(ConsentDto consent) {
        document.drawText(NEWLINE_CHARACTER);

        Object[][] title = {
                {"HEALTH INFORMATION TO BE DISCLOSED"}
        };
        float[] healthInformationTaleWidth = new float[]{480};
        int[] healthInformationTaleAlignment = new int[]{HexPdf.LEFT};
        document.drawTable(title,
                healthInformationTaleWidth,
                healthInformationTaleAlignment,
                HexPdf.LEFT);

        String sensitivityCategoriesLabel = "To SHARE the following medical information:";
        String subLabel = "Sensitivity Categories:";
        String sensitivityCategories = consent.getCategory().stream()
                .map(SensitivityCategory::getDisplay)
                .collect(Collectors.joining(NEWLINE_AND_LIST_PREFIX));

        String sensitivityCategoriesStr = sensitivityCategoriesLabel
                .concat(NEWLINE_CHARACTER).concat(subLabel)
                .concat(NEWLINE_AND_LIST_PREFIX).concat(sensitivityCategories);

        String purposeLabel = "To SHARE for the following purpose(s):";

        String purposes = consent.getSharePurposes().stream()
                .map(Purpose::getDisplay)
                .collect(Collectors.joining(NEWLINE_AND_LIST_PREFIX));
        String purposeOfUseStr = purposeLabel.concat(NEWLINE_AND_LIST_PREFIX).concat(purposes);

        Object[][] healthInformationHeaders = {
                {sensitivityCategoriesStr, purposeOfUseStr}
        };
        float[] healthInformationTableColumnWidth = new float[]{240, 240};
        int[] healthInformationTableColumnAlignment = new int[]{HexPdf.LEFT, HexPdf.LEFT};

        document.drawTable(healthInformationHeaders,
                healthInformationTableColumnWidth,
                healthInformationTableColumnAlignment,
                HexPdf.LEFT);
    }*/

    private void drawConsentTermsSection(HexPdf document, String consentTerms, PatientDto patient) {

        Object[][] title = {
                {"CONSENT TERMS"}
        };
        float[] consentTermsColumnWidth = new float[]{480};
        int[] consentTermsColumnAlignment = new int[]{HexPdf.LEFT};
        document.drawTable(title,
                consentTermsColumnWidth,
                consentTermsColumnAlignment,
                HexPdf.LEFT);

        final String userNameKey = "ATTESTER_FULL_NAME";
        String termsWithAttestedName = StrSubstitutor.replace(consentTerms,
                ImmutableMap.of(userNameKey, patient.getName().get(0).getFirstName() + patient.getName().get(0).getFirstName()));


        document.drawText(termsWithAttestedName);
    }

    private void drawEffectiveAndExspireDateSection(HexPdf document, ConsentDto consent) {
        // Prepare table content
        String effectiveDateContent = "Effective Date: ".concat(PdfBoxHandler.formatLocalDate(consent.getPeriod().getStart(), DATE_FORMAT_PATTERN));
        String expirationDateContent = "Expiration Date: ".concat(PdfBoxHandler.formatLocalDate(consent.getPeriod().getEnd(), DATE_FORMAT_PATTERN));

        Object[][] title = {
                {effectiveDateContent, expirationDateContent}
        };
        document.drawText(NEWLINE_CHARACTER);
        document.drawText(NEWLINE_CHARACTER);

        float[] consentDurationTableColumnWidth = new float[]{240, 240};
        int[] consentDurationTableColumnAlignment = new int[]{HexPdf.LEFT, HexPdf.LEFT};
        document.drawTable(title,
                consentDurationTableColumnWidth,
                consentDurationTableColumnAlignment,
                HexPdf.LEFT);
    }

}
