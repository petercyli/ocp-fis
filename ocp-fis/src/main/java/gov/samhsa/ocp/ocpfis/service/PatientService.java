package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;

import java.util.List;
import java.util.Optional;

public interface PatientService {

    List<PatientDto> getPatients();

    PageDto<PatientDto> getPatientsByValue(String value, String type, boolean showInactive, Optional<Integer> page, Optional<Integer> size);
}
