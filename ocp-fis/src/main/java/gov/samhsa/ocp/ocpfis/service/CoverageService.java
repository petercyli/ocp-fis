package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;

public interface CoverageService {

    void createCoverage(CoverageDto coverageDto);

    List<ReferenceDto> getSubscriberOptions(String patientId);
}
