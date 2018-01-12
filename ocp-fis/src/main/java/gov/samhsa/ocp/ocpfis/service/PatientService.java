package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchPatientDto;

import java.util.Set;

public interface PatientService {

    Set<PatientDto> getPatients();

    Set<PatientDto> searchPatient(SearchPatientDto searchPatientDto);

    Set<PatientDto> getPatientsByValue(String value, String type, boolean showInactive);
}
