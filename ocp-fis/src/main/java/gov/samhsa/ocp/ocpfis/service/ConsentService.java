package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.GeneralConsentRelatedFieldDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PdfDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;
import java.util.Optional;

public interface ConsentService {

    PageDto<ConsentDto> getConsents(Optional<String> patient, Optional<String> practitioner, Optional<String> status, Optional<Boolean> generalDesignation, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void createConsent(ConsentDto consentDto);

    void updateConsent(String consentId,ConsentDto consentDto);

    ConsentDto getConsentsById(String consentId);

    GeneralConsentRelatedFieldDto getGeneralConsentRelatedFields(String patient);

    PdfDto createConsentPdf(String consentId);

    void attestConsent(String consentId);

    List<ReferenceDto> getActors(String name, Optional<List<String>> actorsAlreadyAssigned);
}
