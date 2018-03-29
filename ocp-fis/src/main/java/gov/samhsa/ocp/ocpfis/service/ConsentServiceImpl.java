package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ConsentServiceImpl implements ConsentService {

    private final IGenericClient fhirClient;
    private final LookUpService lookUpService;
    private final FisProperties fisProperties;
    private final ModelMapper modelMapper;


    @Autowired
    public ConsentServiceImpl(ModelMapper modelMapper,
                              IGenericClient fhirClient,
                              LookUpService lookUpService,
                              FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }

    @Override
    public PageDto<ConsentDto> getConsents(Optional<String> patient, Optional<String> fromActor, Optional<String> toActor, Optional<Boolean> generalDesignation, Optional<String> status, Optional<Integer> pageNumber, Optional<Integer> pageSize) {

        int numberOfConsentsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Consent.name());
        Bundle firstPageConsentBundle;
        Bundle otherPageConsentBundle;

        // Generate the Query Based on Input Variables
        IQuery iQuery = getConsentIQuery(patient, fromActor, toActor, status);

        //Apply Filters Based on Input Variables

        firstPageConsentBundle = PaginationUtil.getSearchBundleFirstPage(iQuery, numberOfConsentsPerPage, Optional.empty());

        if (firstPageConsentBundle == null || firstPageConsentBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Consents were found in the FHIR server.");
        }

        log.info("FHIR Consent(s) bundle retrieved " + firstPageConsentBundle.getTotal() + " Consent(s) from FHIR server successfully");
        otherPageConsentBundle = firstPageConsentBundle;


        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageConsentBundle.getLink(Bundle.LINK_NEXT) != null) {
            otherPageConsentBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageConsentBundle, pageNumber.get(), numberOfConsentsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedConsents = otherPageConsentBundle.getEntry();

        // Map to DTO
        List<ConsentDto> consentDtosList = retrievedConsents.stream().map(this::convertConsentBundleEntryToConsentDto).collect(Collectors.toList());
        return (PageDto<ConsentDto>) PaginationUtil.applyPaginationForSearchBundle(consentDtosList, otherPageConsentBundle.getTotal(), numberOfConsentsPerPage, pageNumber);

    }

    private ConsentDto convertConsentBundleEntryToConsentDto(Bundle.BundleEntryComponent fhirConsentDtoModel) {
        ConsentDto consentDto = modelMapper.map(fhirConsentDtoModel.getResource(), ConsentDto.class);
        return consentDto;
    }

    private IQuery getConsentIQuery(Optional<String> patient, Optional<String> fromActor, Optional<String> toActor, Optional<String> status) {
        IQuery iQuery = fhirClient.search().forResource(Consent.class);

        String dateToday = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

        //Get Sub tasks by parent task id
        if (status.isPresent()) {
            iQuery.where(new TokenClientParam("status").exactly().code("active"));
        } else {
            //query the task and sub-task owned by specific practitioner
            if ((fromActor.isPresent() || toActor.isPresent()) && !patient.isPresent()) {
                iQuery.where(new ReferenceClientParam("actor").hasId(fromActor.get()));
            }

            //query the task and sub-task for the specific patient
            if (patient.isPresent() && !fromActor.isPresent() && !toActor.isPresent()) {
                iQuery.where(new ReferenceClientParam("patient").hasId(patient.get()));
            }

            //query the task and sub-task owned by specific practitioner and for the specific patient
            if ((fromActor.isPresent() || toActor.isPresent()) && patient.isPresent()) {
                iQuery.where(new ReferenceClientParam("actor").hasId(fromActor.get()))
                      .where(new ReferenceClientParam("patient").hasId(patient.get()));
            }
        }
       // iQuery.where(new DateClientParam("period").afterOrEquals().second(dateToday))
       //         .where(new DateClientParam("period").beforeOrEquals().second(dateToday));

        return iQuery;
    }

}
