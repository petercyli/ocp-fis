package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.ProvenanceActivityEnum;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.CommunicationDtoToCommunicationMap;
import gov.samhsa.ocp.ocpfis.service.mapping.CommunicationToCommunicationDtoMap;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.ProvenanceUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class CommunicationServiceImpl implements CommunicationService {

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    private final ProvenanceUtil provenanceUtil;

    @Autowired
    public CommunicationServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties, ProvenanceUtil provenanceUtil) {
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.provenanceUtil = provenanceUtil;
    }

    public PageDto<CommunicationDto> getCommunications(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<String> organization, Optional<String> topic, Optional<String> resourceType, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfCommunicationsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Communication.name());
        IQuery iQuery = fhirClient.search().forResource(Communication.class);

        //Set Sort order
        iQuery = FhirOperationUtil.setLastUpdatedTimeSortOrder(iQuery, true);

        //Check for Patient
        iQuery.where(new ReferenceClientParam("patient").hasId(searchValue));

        //Check for Communication
        if (searchKey.equalsIgnoreCase("communicationId"))
            iQuery.where(new TokenClientParam("_id").exactly().code(searchValue));

        //Check for Status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            iQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        }

        //Topic
        if(topic.isPresent() && resourceType.isPresent()) {
            //iQuery.where(new RichStringClientParam("topic").contains().value(topic.get()));
            iQuery.include(Communication.INCLUDE_ALL);
        }

        Bundle firstPageCommunicationBundle;
        Bundle otherPageCommunicationBundle;
        boolean firstPage = true;

        firstPageCommunicationBundle = (Bundle) iQuery
                .count(numberOfCommunicationsPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPageCommunicationBundle == null || firstPageCommunicationBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Communications were found in the FHIR server.");
        }

        otherPageCommunicationBundle = firstPageCommunicationBundle;

        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageCommunicationBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageCommunicationBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageCommunicationBundle, pageNumber.get(), numberOfCommunicationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedCommunications = otherPageCommunicationBundle.getEntry();

        List<CommunicationDto> communicationDtos = retrievedCommunications.stream().filter(retrivedBundle -> retrivedBundle.getResource().getResourceType().equals(ResourceType.Communication)).map(retrievedCommunication -> {
            Communication communication = (Communication) retrievedCommunication.getResource();
            return CommunicationToCommunicationDtoMap.map(communication);

        }).collect(toList());

        if (topic.isPresent() && resourceType.isPresent()) {
            communicationDtos = communicationDtos.stream().filter(dto -> {
                boolean result = false;
                ReferenceDto refDto = dto.getTopic();

                if (refDto != null && refDto.getReference() != null) {
                    result = refDto.getReference().equals(resourceType.get() + "/" + topic.get());
                }

                return result;
            }).collect(toList());
            otherPageCommunicationBundle.setTotal(communicationDtos.size());
        }

        double totalPages = Math.ceil((double) otherPageCommunicationBundle.getTotal() / numberOfCommunicationsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(communicationDtos, numberOfCommunicationsPerPage, totalPages, currentPage, communicationDtos.size(), otherPageCommunicationBundle.getTotal());
    }

    public List<String> getRecipientsByCommunicationId(String patient, String communicationId) {
        List<String> recipientIds = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(Communication.class)
                .where(new ReferenceClientParam("patient").hasId(patient))
                .where(new TokenClientParam("_id").exactly().code(communicationId))
                .include(Communication.INCLUDE_RECIPIENT)
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> components = bundle.getEntry();
            recipientIds = components.stream().map(Bundle.BundleEntryComponent::getResource).filter(resource -> resource instanceof Practitioner || resource instanceof Patient || resource instanceof RelatedPerson || resource instanceof Organization).map(resource -> resource.getIdElement().getIdPart()).collect(Collectors.toList());
        }

        return recipientIds;
    }

    @Override
    public void createCommunication(CommunicationDto communicationDto, Optional<String> loggedInUser) {
        try {
            List<String> idList = new ArrayList<>();

            final Communication communication = CommunicationDtoToCommunicationMap.map(communicationDto, lookUpService);
            communication.setSent(DateUtil.convertLocalDateTimeToUTCDate(LocalDateTime.now()));
            //Set Profile Meta Data
            FhirProfileUtil.setCommunicationProfileMetaData(fhirClient, communication);
            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, communication, Optional.empty(), ResourceType.Communication.name(), "Create Communication");
            //Create
            MethodOutcome methodOutcome = FhirOperationUtil.createFhirResource(fhirClient, communication, ResourceType.Communication.name());
            idList.add(ResourceType.Communication.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

            if(fisProperties.isProvenanceEnabled()) {
                provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.CREATE, loggedInUser);
            }

        } catch (ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while create a communication:" + e.getMessage());
        }
    }

    @Override
    public void updateCommunication(String communicationId, CommunicationDto communicationDto, Optional<String> loggedInUser) {

        try {
            List<String> idList = new ArrayList<>();

            Communication communication = CommunicationDtoToCommunicationMap.map(communicationDto, lookUpService);
            communication.setId(communicationId);
            //Set Profile Meta Data
            FhirProfileUtil.setCommunicationProfileMetaData(fhirClient, communication);
            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, communication, Optional.of(communicationId), ResourceType.Communication.name(), "Update Communication");
            //Update
            MethodOutcome methodOutcome = FhirOperationUtil.updateFhirResource(fhirClient, communication, ResourceType.Communication.name());
            idList.add(ResourceType.Communication.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

            if(fisProperties.isProvenanceEnabled()) {
                provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.UPDATE, loggedInUser);
            }

        } catch (ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while update a communication:" + e.getMessage());
        }
    }


}
