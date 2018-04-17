package gov.samhsa.ocp.ocpfis.service.pdf;

import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;

import java.io.IOException;

public interface ConsentPdfGenerator {
    String getConsentTitle(String pdfType);

    void drawConsentTitle(HexPDF document, String consentTitle);

    void setPageFooter(HexPDF document, String consentTitle);

    void drawPatientInformationSection(HexPDF document, ConsentDto consent, PatientDto patientDto);

    void addConsentSigningDetails(HexPDF document, PatientDto patient, Boolean signedByPatient) throws IOException;

    byte[] generateConsentPdf(ConsentDto consent, PatientDto patientProfile, Boolean operatedByPatient) throws IOException;

}
