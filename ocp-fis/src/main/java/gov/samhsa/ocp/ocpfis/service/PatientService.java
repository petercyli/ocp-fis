package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;

import java.util.List;
import java.util.Optional;

public interface PatientService {

    List<PatientDto> getPatients();

    PageDto<PatientDto> getPatientsByValue(Optional<String> key, Optional<String> value, Optional<String> filterKey, Optional<String> organization, Optional<String> practitioner, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize,Optional<Boolean> showAll);

    List<PatientDto> getPatientsByPractitioner(Optional<String> practitioner, Optional<String> searchKey, Optional<String> searchValue);

    PageDto<PatientDto> getPatientsByPractitioner(Optional<String> practitioner, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void createPatient(PatientDto patientDto, Optional<String> loggedInUser);

    void updatePatient(PatientDto patientDto, Optional<String> loggedInUser);

    PatientDto getPatientById(String patientId);
}
