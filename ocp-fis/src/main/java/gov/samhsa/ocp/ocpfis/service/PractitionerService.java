package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;

import java.util.List;

public interface PractitionerService {
    List<PractitionerDto> getAllPractitioners();
    List<PractitionerDto> searchPractitioners(String searchValue);
}
