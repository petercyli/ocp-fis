package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CommunicationToCommunicationDtoMap {

    public static CommunicationDto map(Communication communication) {
        CommunicationDto communicationDto = new CommunicationDto();

        if (communication.hasMeta() && communication.getMeta().hasLastUpdated()) {
            Date lastUpdated = communication.getMeta().getLastUpdated();
            communicationDto.setLastUpdated(DateUtil.convertDateTimeToString(lastUpdated));
        }

        communicationDto.setLogicalId(communication.getIdElement().getIdPart());

        communicationDto.setNotDone(communication.getNotDone());

        if (communication.hasStatus()) {
            communicationDto.setStatusValue(communication.getStatus().getDisplay());
            communicationDto.setStatusCode(communication.getStatus().toCode());
        }

        if (communication.hasCategory()) {
            ValueSetDto category = FhirDtoUtil.convertCodeableConceptListToValuesetDto(communication.getCategory());
            communicationDto.setCategoryValue(category.getDisplay());
            communicationDto.setCategoryCode(category.getCode());
        }

        if (communication.hasMedium()) {
            ValueSetDto medium = FhirDtoUtil.convertCodeableConceptListToValuesetDto(communication.getMedium());
            communicationDto.setMediumValue(medium.getDisplay());
            communicationDto.setMediumCode(medium.getCode());
        }

        if (communication.hasNotDoneReason()) {
            ValueSetDto notDoneReason = FhirDtoUtil.convertCodeableConceptToValueSetDto(communication.getNotDoneReason());
            communicationDto.setNotDoneReasonValue(notDoneReason.getDisplay());
            communicationDto.setNotDoneReasonCode(notDoneReason.getCode());
        }

        if (communication.hasRecipient()) {
            communicationDto.setRecipient(communication.getRecipient().stream().map(FhirDtoUtil::convertReferenceToReferenceDto).collect(Collectors.toList()));
        }

        if (communication.hasSender()) {
            communicationDto.setSender(ReferenceDto.builder()
                    .reference((communication.getSender().getReference() != null && !communication.getSender().getReference().isEmpty()) ? communication.getSender().getReference() : null)
                    .display((communication.getSender().getDisplay() != null && !communication.getSender().getDisplay().isEmpty()) ? communication.getSender().getDisplay() : null)
                    .build());
        }

        if (communication.hasSubject()) {
            communicationDto.setSubject(ReferenceDto.builder()
                    .reference((communication.getSubject().getReference() != null && !communication.getSubject().getReference().isEmpty()) ? communication.getSubject().getReference() : null)
                    .display((communication.getSubject().getDisplay() != null && !communication.getSubject().getDisplay().isEmpty()) ? communication.getSubject().getDisplay() : null)
                    .build());
        }


        if (communication.hasTopic()) {
            communicationDto.setTopic(ReferenceDto.builder()
                    .reference(FhirDtoUtil.convertReferenceToReferenceDto(communication.getTopic().stream().findAny().get()).getReference())
                    .display(FhirDtoUtil.convertReferenceToReferenceDto(communication.getTopic().stream().findAny().get()).getDisplay())
                    .build());
        }


        if (communication.hasDefinition()) {
            communicationDto.setDefinition(ReferenceDto.builder()
                    .reference(FhirDtoUtil.convertReferenceToReferenceDto(communication.getDefinition().stream().findAny().get()).getReference())
                    .display(FhirDtoUtil.convertReferenceToReferenceDto(communication.getDefinition().stream().findAny().get()).getDisplay())
                    .build());
        }

        //TODO: Remove this after data purge.
        if (communication.hasContext()) {
            communicationDto.setContext(ReferenceDto.builder()
                    .reference((communication.getContext().getReference() != null && !communication.getContext().getReference().isEmpty()) ? communication.getContext().getReference() : null)
                    .display((communication.getContext().getDisplay() != null && !communication.getContext().getDisplay().isEmpty()) ? communication.getContext().getDisplay() : null)
                    .build());
        }


        if (communication.hasNote()) {
             List<Annotation> annotations = communication.getNote();

            annotations.stream().forEach(annotation -> {
                StringType type = (StringType) annotation.getAuthor();
                if (type != null) {
                    String value = type.getValue();

                    if (value != null) {
                        if (value.equals("note")) {
                            communicationDto.setNote(annotation.getText());
                        } else {
                            communicationDto.setDuration(annotation.getText());
                        }
                    }
                }
            });
        }

        if (communication.hasPayload()) {
            try {
                communicationDto.setPayloadContent(communication.getPayload().stream().findAny().get().getContentStringType().getValue());
            } catch (FHIRException e) {
                throw new FHIRClientException("FHIR Client returned with an error while load a communication:" + e.getMessage());
            }
        }

        if (communication.hasSent()) {
            communicationDto.setSent(DateUtil.convertDateTimeToString(communication.getSent()));
        }

        if (communication.hasReceived()) {
            communicationDto.setReceived(DateUtil.convertDateTimeToString(communication.getReceived()));
        }

        return communicationDto;

    }
}
