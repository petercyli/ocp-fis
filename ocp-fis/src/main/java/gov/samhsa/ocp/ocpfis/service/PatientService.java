package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import org.hl7.fhir.dstu3.model.Patient;


public interface PatientService {

    /* converts UserDto to fhir patient object */
    Patient createFhirPatient(PatientDto patientDto);

    String getPatientResourceId(String patientMrnSystem, String patientMrn);

    void publishFhirPatient(PatientDto patientDto);

    void updateFhirPatient(PatientDto patientDto);


}
