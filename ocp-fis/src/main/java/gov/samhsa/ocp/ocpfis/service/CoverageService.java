package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;
import java.util.Optional;

public interface CoverageService {

    void createCoverage(CoverageDto coverageDto);

    List<ReferenceDto> getSubscriberOptions(String patientId);

    PageDto<CoverageDto> getCoverages(String patientId, Optional<Integer> pageNumber, Optional<Integer> pageSize);
}
