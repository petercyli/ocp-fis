package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class CommunicationServiceImpl implements CommunicationService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public CommunicationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }

    public PageDto<CommunicationDto> getCommunications(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfCommunicationsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Communication.name());
        IQuery iQuery = fhirClient.search().forResource(Communication.class);

        //Check for Patient
        if (searchKey.equalsIgnoreCase("patientId"))
            iQuery.where(new ReferenceClientParam("patient").hasId(searchValue));

        //Check for Communication
        if (searchKey.equalsIgnoreCase("communicationId"))
            iQuery.where(new TokenClientParam("_id").exactly().code(searchValue));

        //Check for Status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            iQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
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

            CommunicationDto communicationDto = new CommunicationDto();


            communicationDto.setLogicalId(communication.getIdElement().getIdPart());

            if (communication.hasStatus()) {
                communicationDto.setStatusValue(communication.getStatus().getDisplay());
            }

           if (communication.hasCategory()) {
                ValueSetDto category = FhirDtoUtil.convertCodeableConceptListToValuesetDto(communication.getCategory());
                communicationDto.setCategoryValue(category.getDisplay());
            }

            if (communication.hasMedium()) {
                ValueSetDto medium = FhirDtoUtil.convertCodeableConceptListToValuesetDto(communication.getCategory());
                communicationDto.setMediumValue(medium.getDisplay());
            }


            if (communication.hasRecipient()) {
                communicationDto.setRecipient(communication.getRecipient().stream().map(recipient -> FhirDtoUtil.convertReferenceToReferenceDto(recipient)).collect(Collectors.toList()));
            }

            if (communication.hasSender()) {
                communicationDto.setSender(ReferenceDto.builder()
                            .reference((communication.getSender().getReference() != null && !communication.getSender().getReference().isEmpty()) ? communication.getSender().getReference() : null)
                            .display((communication.getSender().getDisplay()!= null && !communication.getSender().getDisplay().isEmpty()) ? communication.getSender().getDisplay() : null)
                            .build());
            }

            if (communication.hasSent()) {
                communicationDto.setSent(DateUtil.convertDateToString(communication.getSent()));
            }

            return communicationDto;

        }).collect(toList());

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

        if(bundle != null) {
            List<Bundle.BundleEntryComponent> components = bundle.getEntry();
            for(Bundle.BundleEntryComponent component : components) {
                Resource resource = component.getResource();

                if(resource instanceof Practitioner || resource instanceof Patient || resource instanceof Practitioner || resource instanceof Organization) {
                    recipientIds.add(resource.getIdElement().getIdPart());
                }
            }
        }

        return recipientIds;
    }

    @Override
    public void createCommunication(CommunicationDto communicationDto) {

        try{
            final Communication communication = convertCommunicationDtoToCommunication(communicationDto);
            //Validate
            FhirUtil.validateFhirResource(fhirValidator, communication, Optional.empty(), ResourceType.Communication.name(), "Create Communication");
            //Create
            FhirUtil.createFhirResource(fhirClient, communication, ResourceType.Communication.name());
        }
        catch ( ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while create a communication:" + e.getMessage());
        }
    }

    @Override
    public void updateCommunication(String communicationId, CommunicationDto communicationDto) {

        try {
            Communication communication = convertCommunicationDtoToCommunication(communicationDto);
            communication.setId(communicationId);
            //Validate
            FhirUtil.validateFhirResource(fhirValidator, communication, Optional.of(communicationId), ResourceType.Communication.name(), "Update Communication");
            //Update
            FhirUtil.updateFhirResource(fhirClient, communication, ResourceType.Communication.name());
        }
        catch ( ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while update a communication:" + e.getMessage());
        }
    }

    private Communication convertCommunicationDtoToCommunication(CommunicationDto communicationDto) throws ParseException {
        Communication communication = new Communication();

        communication.setNotDone(communicationDto.isNotDone());

        //Set Subject
        if(communicationDto.getSubject() !=null) {
            communication.setSubject(FhirDtoUtil.mapReferenceDtoToReference(communicationDto.getSubject()));
        }

        //Set Sender
        communication.setSender(FhirDtoUtil.mapReferenceDtoToReference(communicationDto.getSender()));

        //Set Status
        if (communicationDto.getStatusCode() != null) {
            communication.setStatus(Communication.CommunicationStatus.valueOf(communicationDto.getStatusCode().toUpperCase()));
        }

        //Set Category
        if (communicationDto.getCategoryCode() != null) {
            ValueSetDto category = FhirDtoUtil.convertCodeToValueSetDto(communicationDto.getCategoryCode(), lookUpService.getCommunicationCategory());
            List<CodeableConcept> categories = new ArrayList<>();
            categories.add(FhirDtoUtil.convertValuesetDtoToCodeableConcept(category));
            communication.setCategory(categories);
        }

        //Set Medium
        if (communicationDto.getMediumCode() != null) {
            ValueSetDto medium = FhirDtoUtil.convertCodeToValueSetDto(communicationDto.getMediumCode(), lookUpService.getCommunicationMedium());
            List<CodeableConcept> mediums = new ArrayList<>();
            mediums.add(FhirDtoUtil.convertValuesetDtoToCodeableConcept(medium));
            communication.setMedium(mediums);
        }

        //Set Not Done Reason
        if (communicationDto.getNotDoneReasonCode() != null) {
            ValueSetDto notDoneReason = FhirDtoUtil.convertCodeToValueSetDto(communicationDto.getNotDoneReasonCode(), lookUpService.getCommunicationNotDoneReason());
            communication.setNotDoneReason(FhirDtoUtil.convertValuesetDtoToCodeableConcept(notDoneReason));
        }

        //Set subject
        if (communicationDto.getSubject() != null) {
            communication.setSubject(FhirDtoUtil.mapReferenceDtoToReference(communicationDto.getSubject()));
        }

        //Set recipients
        if (communicationDto.getRecipient() != null) {
            communication.setRecipient(communicationDto.getRecipient().stream().map(recipient -> FhirDtoUtil.mapReferenceDtoToReference(recipient)).collect(Collectors.toList()));
        }

        //Set topic
        if (communicationDto.getTopic() != null) {
            List<Reference> topics = new ArrayList<>();
            topics.add(FhirDtoUtil.mapReferenceDtoToReference(communicationDto.getTopic()));
            communication.setTopic(topics);
        }

        //Set definitions
        if (communicationDto.getDefinition() != null) {
            List<Reference> definitions = new ArrayList<>();
            definitions.add(FhirDtoUtil.mapReferenceDtoToReference(communicationDto.getDefinition()));
            communication.setDefinition(definitions);
        }

        //Set context
        if (communicationDto.getContext() != null) {
            communication.setContext(FhirDtoUtil.mapReferenceDtoToReference(communicationDto.getContext()));
        }

        if(communicationDto.getSent() !=null) {
            communication.setSent(DateUtil.convertStringToDate(communicationDto.getSent()));
        }


        if(communicationDto.getReceived() !=null)
            communication.setReceived(DateUtil.convertStringToDate(communicationDto.getReceived()));

        //Set Note
        if (communicationDto.getNote() != null){
            Annotation note = new Annotation();
            note.setText(communicationDto.getNote());
            List<Annotation> notes = new ArrayList<>();
            notes.add(note);
            communication.setNote(notes);
        }

        //Set Message
        if (communicationDto.getPayloadContent() != null){
            StringType newType = new StringType(communicationDto.getPayloadContent());
            Communication.CommunicationPayloadComponent messagePayload = new Communication.CommunicationPayloadComponent(newType);
            List<Communication.CommunicationPayloadComponent> payloads = new ArrayList<>();
            payloads.add(messagePayload);
            communication.setPayload(payloads);
        }

        return communication;
    }


}
