package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;

import java.util.List;
import java.util.Optional;

public interface PatientService {

    List<PatientDto> getPatients();

    PageDto<PatientDto> getPatientsByValue(String value, String type, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size);

    List<PatientDto> getPatientsByPractitioner(String practitioner, Optional<String> searchKey, Optional<String> searchValue);

    PageDto<PatientDto> getPatientsByPractitioner(String practitioner, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void createPatient(PatientDto patientDto);

    void updatePatient(PatientDto patientDto);

    PatientDto getPatientById(String patientId);
}
