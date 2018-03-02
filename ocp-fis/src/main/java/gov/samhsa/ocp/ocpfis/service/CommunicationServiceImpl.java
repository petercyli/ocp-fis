package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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


    @Override
    public void createCommunication(CommunicationDto communicationDto) {
            Communication communication = convertCommunicationDtoToCommunication(communicationDto);
            fhirClient.create().resource(communication).execute();
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
       //     Reference context = new Reference();
       //     context = (convertReferenceDtoToReference(communicationDto.getContext()));
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
                Coding coding = new Coding();
                coding.setCode(valueSetDto.getCode());
                coding.setSystem(valueSetDto.getSystem());
                coding.setDisplay(valueSetDto.getDisplay());
                codeableConcept.addCoding(coding);
            }
            return codeableConcept;
    }

}
