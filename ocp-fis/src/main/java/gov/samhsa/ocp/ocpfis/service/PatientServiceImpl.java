package gov.samhsa.ocp.ocpfis.service;


import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchPatientDto;
import gov.samhsa.ocp.ocpfis.service.exception.PatientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final IGenericClient fhirClient;
    private final IParser iParser;
    private final ModelMapper modelMapper;

    public PatientServiceImpl(IGenericClient fhirClient, IParser iParser, ModelMapper modelMapper) {
        this.fhirClient = fhirClient;
        this.iParser = iParser;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<PatientDto> getPatients() {
        log.debug("Patients Query to FHIR Server: START");
        Bundle response = fhirClient.search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Query to FHIR Server: END");
        return convertBundleToPatientDtos(response, Boolean.FALSE);
    }

    @Override
    public List<PatientDto> searchPatient(SearchPatientDto searchPatientDto) {

        log.debug("Patients Query to FHIR Server: START");
        Bundle response = fhirClient.search()
                .forResource(Patient.class)
                .where(new TokenClientParam("identifier").exactly()
                        .systemAndCode(searchPatientDto.getIdentifierSystem().trim(), searchPatientDto.getIdentifierValue().trim()))
                .where(new StringClientParam("given").matches().value(searchPatientDto.getFirstName().trim()))
                .where(new StringClientParam("family").matches().value(searchPatientDto.getLastName().trim()))
                .where(new TokenClientParam("gender").exactly().code(searchPatientDto.getGenderCode().trim()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        log.debug("Patients Query to FHIR Server: END");
        return convertBundleToPatientDtos(response, Boolean.TRUE);
    }

    private List<PatientDto> convertBundleToPatientDtos(Bundle response, boolean isSearch) {
        List<PatientDto> patientDtos = new ArrayList<>();
        if (null == response || response.isEmpty() || response.getEntry().size() < 1) {
            log.debug("No patients in FHIR Server");
            // Search throw patient not found exception and list just show empty list
            if(isSearch) throw new PatientNotFoundException();
        } else {
            patientDtos = response.getEntry().stream()
                    .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Patient))  //patient entries
                    .map(bundleEntryComponent -> (Patient) bundleEntryComponent.getResource()) // patient resources
                    .peek(patient -> log.debug(iParser.encodeResourceToString(patient)))
                    .map(patient -> modelMapper.map(patient, PatientDto.class))
                    .collect(Collectors.toList());
        }
        log.debug("The no of patients retrieved from FHIR server" + patientDtos.size());
        return patientDtos;
    }

}


