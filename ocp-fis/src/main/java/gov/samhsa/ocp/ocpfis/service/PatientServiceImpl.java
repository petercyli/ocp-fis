package gov.samhsa.ocp.ocpfis.service;


import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchType;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.LocationNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.PatientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final IGenericClient fhirClient;
    private final IParser iParser;
    private final ModelMapper modelMapper;
    private final FisProperties fisProperties;

    public PatientServiceImpl(IGenericClient fhirClient, IParser iParser, ModelMapper modelMapper, FisProperties fisProperties) {
        this.fhirClient = fhirClient;
        this.iParser = iParser;
        this.modelMapper = modelMapper;
        this.fisProperties = fisProperties;
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
    public PageDto<PatientDto> getPatientsByValue(String value, String type, boolean showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPatientsPerPage = size.filter(s -> s > 0 &&
                s <= fisProperties.getLocation().getPagination().getMaxSize()).orElse(fisProperties.getPatient().getPagination().getDefaultSize());

        IQuery PatientSearchQuery = fhirClient.search().forResource(Patient.class);
        if (!showInactive) {
            // show only active patients
            PatientSearchQuery.where(new TokenClientParam("active").exactly().code(Boolean.TRUE.toString()));
        }

        if (type.equalsIgnoreCase(SearchType.NAME.name())) {
            PatientSearchQuery.where(new StringClientParam("name").matches().value(value.trim()));
        } else if (type.equalsIgnoreCase(SearchType.IDENTIFIER.name())) {
            PatientSearchQuery.where(new TokenClientParam("identifier").exactly().code(value.trim()));
        } else {
            throw new BadRequestException("Invalid Type Values");
        }

        Bundle firstPagePatientSearchBundle;
        Bundle otherPagePatientSearchBundle;
        boolean firstPage = true;
        log.debug("Patients Search Query to FHIR Server: START");
        firstPagePatientSearchBundle = (Bundle) PatientSearchQuery.count(numberOfPatientsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        log.debug("Patients Search Query to FHIR Server: END");

        otherPagePatientSearchBundle = firstPagePatientSearchBundle;
        if (page.isPresent() && page.get() > 1 && firstPagePatientSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            firstPage = false;
            otherPagePatientSearchBundle = getSearchBundleAfterFirstPage(firstPagePatientSearchBundle, page.get(), numberOfPatientsPerPage);
        }

        //Arrange Page related info
        List<PatientDto> patientDtos = convertBundleToPatientDtos(otherPagePatientSearchBundle, Boolean.FALSE);
        double totalPages = Math.ceil((double) otherPagePatientSearchBundle.getTotal() / numberOfPatientsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(patientDtos, numberOfPatientsPerPage, totalPages, currentPage, patientDtos.size(), otherPagePatientSearchBundle.getTotal());
    }

    private List<PatientDto> convertBundleToPatientDtos(Bundle response, boolean isSearch) {
        List<PatientDto> patientDtos = new ArrayList<>();
        if (null == response || response.isEmpty() || response.getEntry().size() < 1) {
            log.info("No patients in FHIR Server");
            // Search throw patient not found exception and list will show empty list
            if (isSearch) throw new PatientNotFoundException();
        } else {
            patientDtos = response.getEntry().stream()
                    .filter(bundleEntryComponent -> bundleEntryComponent.getResource().getResourceType().equals(ResourceType.Patient))  //patient entries
                    .map(bundleEntryComponent -> (Patient) bundleEntryComponent.getResource()) // patient resources
                    .peek(patient -> log.debug(iParser.encodeResourceToString(patient)))
                    .map(patient -> {
                        PatientDto patientDto = modelMapper.map(patient, PatientDto.class);
                        patientDto.setId(patient.getIdElement().getIdPart());
                        return patientDto;
                    })
                    .collect(Collectors.toList());
        }
        log.info("Total Patients retrieved from Server #" + patientDtos.size());
        return patientDtos;
    }

    private Bundle getSearchBundleAfterFirstPage(Bundle resourceSearchBundle, int page, int size) {
        if (resourceSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            //Assuming page number starts with 1
            int offset = ((page >= 1 ? page : 1) - 1) * size;

            if (offset >= resourceSearchBundle.getTotal()) {
                throw new PatientNotFoundException("No Patients were found in the FHIR server for this page number");
            }

            String pageUrl = fisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + resourceSearchBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + size
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return resourceSearchBundle;
    }
}


