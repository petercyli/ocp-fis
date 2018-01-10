package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchPatientDto;

import java.util.List;

public interface PatientService {

    List<PatientDto> getPatients();


    List<PatientDto> searchPatient(SearchPatientDto searchPatientDto);
}
