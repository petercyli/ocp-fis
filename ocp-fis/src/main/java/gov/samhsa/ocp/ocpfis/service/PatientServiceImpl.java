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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public Set<PatientDto> getPatients() {
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
    public Set<PatientDto> searchPatient(SearchPatientDto searchPatientDto) {

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

    @Override
    public Set<PatientDto> getPatientsByValue(String searchValue) {
        log.debug("Patients Name Query to FHIR Server: START");
        Bundle response = fhirClient.search()
                .forResource(Patient.class)
                .where(new StringClientParam("name").matches().value(searchValue.trim()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Name Query to FHIR Server: END");
        Set<PatientDto> patientNameDtos = convertBundleToPatientDtos(response, Boolean.FALSE);
        log.info("Toal Name search list #" + patientNameDtos.size());
        log.debug("Patients Identifier Value Query to FHIR Server: START");
        response = fhirClient.search()
                .forResource(Patient.class)
                .where(new TokenClientParam("identifier").exactly().code(searchValue.trim()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Identifier Value Query to FHIR Server: END");
        Set<PatientDto> patientIdentifierDtos = convertBundleToPatientDtos(response, Boolean.FALSE);
        log.info("Total Identifier search list #" + patientIdentifierDtos.size());
        log.debug("Patients Query to FHIR Server: END");

        Set<PatientDto> patientDtos = Stream.concat(patientNameDtos.stream(), patientIdentifierDtos.stream())
                .distinct()
                .collect(Collectors.toSet());
        log.info("Total search list #" + patientDtos.size());
        return patientDtos;
    }

    private Set<PatientDto> convertBundleToPatientDtos(Bundle response, boolean isSearch) {
        Set<PatientDto> patientDtos = new HashSet<>();
        if (null == response || response.isEmpty() || response.getEntry().size() < 1) {
            log.info("No patients in FHIR Server");
            // Search throw patient not found exception and list will show empty list
            if (isSearch) throw new PatientNotFoundException();
        } else {
            patientDtos = response.getEntry().stream()
                    .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Patient))  //patient entries
                    .map(bundleEntryComponent -> (Patient) bundleEntryComponent.getResource()) // patient resources
                    .peek(patient -> log.debug(iParser.encodeResourceToString(patient)))
                    .map(patient ->{
                       PatientDto patientDto = modelMapper.map(patient, PatientDto.class);
                       patientDto.setId(patient.getIdElement().getIdPart());
                       return patientDto;
                    })
                    .collect(Collectors.toSet());
        }
        log.info("Total Patients retrieved from Server #" + patientDtos.size());
        return patientDtos;
    }


}


