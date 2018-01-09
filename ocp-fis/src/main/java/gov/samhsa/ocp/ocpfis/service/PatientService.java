package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;

import java.util.List;

public interface PatientService {

    List<PatientDto> getPatients();
}
