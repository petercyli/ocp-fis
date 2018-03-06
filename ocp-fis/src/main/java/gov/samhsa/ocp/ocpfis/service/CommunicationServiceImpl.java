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
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                ValueSetDto category = convertCodeableConceptListToValuesetDto(communication.getCategory());
                communicationDto.setCategoryValue(category.getDisplay());
            }

            if (communication.hasMedium()) {
                ValueSetDto medium = convertCodeableConceptListToValuesetDto(communication.getCategory());
                communicationDto.setMediumVaule(medium.getDisplay());
            }


            if (communication.hasRecipient()) {
                communicationDto.setRecipient(communication.getRecipient().stream().map(recipient -> convertReferenceToReferenceDto(recipient)).collect(Collectors.toList()));
            }

            if (communication.hasSender()) {
                communicationDto.setSender(ReferenceDto.builder()
                            .reference((communication.getSender().getReference() != null && !communication.getSender().getReference().isEmpty()) ? communication.getSender().getReference() : null)
                            .display((communication.getSender().getDisplay()!= null && !communication.getSender().getDisplay().isEmpty()) ? communication.getSender().getDisplay() : null)
                            .build());
            }

            if (communication.hasSent()) {
                communicationDto.setSent(DateUtil.convertDateToLocalDate(communication.getSent()));
            }

            return communicationDto;

        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageCommunicationBundle.getTotal() / numberOfCommunicationsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(communicationDtos, numberOfCommunicationsPerPage, totalPages, currentPage, communicationDtos.size(), otherPageCommunicationBundle.getTotal());
    }

    @Override
    public void createCommunication(CommunicationDto communicationDto) {
            final Communication communication = convertCommunicationDtoToCommunication(communicationDto);
            //Validate
            FhirUtil.validateFhirResource(fhirValidator, communication, Optional.empty(), ResourceType.Communication.name(), "Create Communication");
            //Create
            FhirUtil.createFhirResource(fhirClient, communication, ResourceType.Communication.name());
    }

    @Override
    public void updateCommunication(String communicationId, CommunicationDto communicationDto) {

            Communication communication = convertCommunicationDtoToCommunication(communicationDto);
            communication.setId(communicationId);
            //Validate
            FhirUtil.validateFhirResource(fhirValidator, communication, Optional.of(communicationId), ResourceType.Communication.name(), "Update Communication");
            //Update
            FhirUtil.updateFhirResource(fhirClient, communication, ResourceType.Communication.name());
    }

    private Communication convertCommunicationDtoToCommunication(CommunicationDto communicationDto) {
        Communication communication = new Communication();

        communication.setNotDone(communicationDto.isNotDone());

        //Set Subject
        if(communicationDto.getSubject() !=null) {
            communication.setSubject(convertReferenceDtoToReference(communicationDto.getSubject()));
        }

        //Set Sender
        communication.setSender(convertReferenceDtoToReference(communicationDto.getSender()));

        //Set Status
        if (communicationDto.getStatusCode() != null) {
            communication.setStatus(Communication.CommunicationStatus.valueOf(communicationDto.getStatusCode().toUpperCase()));
        }

        //Set Category
        if (communicationDto.getCategoryCode() != null) {
            ValueSetDto category = convertCodeToValueSetDto(communicationDto.getCategoryCode(), lookUpService.getCommunicationCategory());
            List<CodeableConcept> categories = new ArrayList<>();
            categories.add(convertValuesetDtoToCodeableConcept(category));
            communication.setCategory(categories);
        }

        //Set Medium
        if (communicationDto.getMediumCode() != null) {
            ValueSetDto medium = convertCodeToValueSetDto(communicationDto.getMediumCode(), lookUpService.getCommunicationMedium());
            List<CodeableConcept> mediums = new ArrayList<>();
            mediums.add(convertValuesetDtoToCodeableConcept(medium));
            communication.setMedium(mediums);
        }

        //Set Not Done Reason
        if (communicationDto.getNotDoneReasonCode() != null) {
            ValueSetDto notDoneReason = convertCodeToValueSetDto(communicationDto.getNotDoneReasonCode(), lookUpService.getCommunicationNotDoneReason());
            communication.setNotDoneReason(convertValuesetDtoToCodeableConcept(notDoneReason));
        }

        //Set subject
        if (communicationDto.getSubject() != null) {
            communication.setSubject(convertReferenceDtoToReference(communicationDto.getSubject()));
        }

        //Set recipients
        if (communicationDto.getRecipient() != null) {
            communication.setRecipient(communicationDto.getRecipient().stream().map(recipient -> convertReferenceDtoToReference(recipient)).collect(Collectors.toList()));
        }

        //Set topic
        if (communicationDto.getTopic() != null) {
            List<Reference> topics = new ArrayList<>();
            topics.add(convertReferenceDtoToReference(communicationDto.getTopic()));
            communication.setTopic(topics);
        }

        //Set definitions
        if (communicationDto.getDefinition() != null) {
            List<Reference> definitions = new ArrayList<>();
            definitions.add(convertReferenceDtoToReference(communicationDto.getDefinition()));
            communication.setDefinition(definitions);
        }

        //Set context
        if (communicationDto.getContext() != null) {
            communication.setContext(convertReferenceDtoToReference(communicationDto.getContext()));
        }

        if(communicationDto.getSent() !=null)
            communication.setSent((java.sql.Date.valueOf(communicationDto.getSent())));

        if(communicationDto.getReceived() !=null)
            communication.setReceived((java.sql.Date.valueOf(communicationDto.getReceived())));

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

    private Reference convertReferenceDtoToReference(ReferenceDto referenceDto) {
        Reference reference = new Reference();
        reference.setDisplay(referenceDto.getDisplay());
        reference.setReference(referenceDto.getReference());
        return reference;
    }

    private ReferenceDto convertReferenceToReferenceDto(Reference reference) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setDisplay(reference.getDisplay());
        referenceDto.setReference(reference.getReference());
        return referenceDto;
    }

    private ValueSetDto convertCodeToValueSetDto(String code, List<ValueSetDto> valueSetDtos) {
        return valueSetDtos.stream().filter(lookup -> code.equalsIgnoreCase(lookup.getCode())).map(valueSet -> {
            ValueSetDto valueSetDto = new ValueSetDto();
            valueSetDto.setCode(valueSet.getCode());
            valueSetDto.setDisplay(valueSet.getDisplay());
            valueSetDto.setSystem(valueSet.getSystem());
            return valueSetDto;
        }).findFirst().orElse(null);
    }

    private CodeableConcept convertValuesetDtoToCodeableConcept (ValueSetDto valueSetDto) {
            CodeableConcept codeableConcept = new CodeableConcept();
            if (valueSetDto != null) {
                Coding coding = FhirUtil.getCoding(valueSetDto.getCode(),valueSetDto.getDisplay(),valueSetDto.getSystem());
                codeableConcept.addCoding(coding);
            }
            return codeableConcept;
    }

    private ValueSetDto convertCodeableConceptToValueSetDto(CodeableConcept  source) {
        ValueSetDto valueSetDto =new ValueSetDto();
        if(source !=null){
            if(source.getCodingFirstRep().getDisplay() !=null)
                valueSetDto.setDisplay(source.getCodingFirstRep().getDisplay());
            if(source.getCodingFirstRep().getSystem()!=null)
                valueSetDto.setSystem(source.getCodingFirstRep().getSystem());
            if(source.getCodingFirstRep().getCode()!=null)
                valueSetDto.setCode(source.getCodingFirstRep().getCode());
        }
        return valueSetDto;
    }

    private ValueSetDto convertCodeableConceptListToValuesetDto(List<CodeableConcept> source) {
        ValueSetDto valueSetDto = new ValueSetDto();

        if (!source.isEmpty()) {
            int sourceSize = source.get(0).getCoding().size();
            if (sourceSize > 0) {
                source.get(0).getCoding().stream().findAny().ifPresent(coding -> {
                    valueSetDto.setSystem(coding.getSystem());
                    valueSetDto.setDisplay(coding.getDisplay());
                    valueSetDto.setCode(coding.getCode());
                });
            }
        }
        return valueSetDto;

    }


}
