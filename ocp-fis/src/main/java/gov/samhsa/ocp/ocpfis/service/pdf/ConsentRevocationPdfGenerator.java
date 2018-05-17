package gov.samhsa.ocp.ocpfis.service.pdf;

import gov.samhsa.ocp.ocpfis.service.dto.DetailedConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;

import java.io.IOException;

public interface ConsentRevocationPdfGenerator {

    byte[] generateConsentRevocationPdf(DetailedConsentDto detailedConsent, PatientDto patient, Boolean revokedByPatient) throws IOException;

}
