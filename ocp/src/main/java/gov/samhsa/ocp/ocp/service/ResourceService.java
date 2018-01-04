package gov.samhsa.ocp.ocp.service;


public interface ResourceService {

    String getFhirResourceByPatientIdentifier(String patientIdentifierSystem, String patientIdentifierValue) ;

}
