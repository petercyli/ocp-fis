package gov.samhsa.ocp.ocpfis.util;


import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Reference;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FhirDtoUtil {

    public static ReferenceDto convertReferenceToReferenceDto(Reference reference) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setDisplay(reference.getDisplay());
        referenceDto.setReference(reference.getReference());
        return referenceDto;
    }

    public static List<AppointmentParticipantDto> convertAppointmentParticipantListToAppointmentParticipantDtoList(List<Appointment.AppointmentParticipantComponent> source) {
         List<AppointmentParticipantDto> participants = new ArrayList<>();
         AppointmentParticipantDto participant = new AppointmentParticipantDto();

            if (source != null && source.size() > 0) {
                int numberOfSource = source.size();
                if (numberOfSource > 0) {
                    source.forEach(member -> {
                        participant.setActorName(member.getActor().getDisplay());
                        participant.setActorReference(member.getActor().getReference());
                        participant.setParticipationStatusCode(member.getStatus().toCode());
                        participant.setParticipantRequiredCode(member.getRequired().toCode());
                        participant.setParticipationStatusCode(member.getStatus().toCode());
                        participants.add(participant);

                    });
                }
            }
            return participants;
    }

    public static ValueSetDto convertCodeToValueSetDto(String code, List<ValueSetDto> valueSetDtos) {
        return valueSetDtos.stream().filter(lookup -> code.equalsIgnoreCase(lookup.getCode())).map(valueSet -> {
            ValueSetDto valueSetDto = new ValueSetDto();
            valueSetDto.setCode(valueSet.getCode());
            valueSetDto.setDisplay(valueSet.getDisplay());
            valueSetDto.setSystem(valueSet.getSystem());
            return valueSetDto;
        }).findFirst().orElse(null);
    }

    public static ValueSetDto convertCodeableConceptToValueSetDto(CodeableConcept  source) {
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

    public static ValueSetDto convertCodeableConceptListToValuesetDto(List<CodeableConcept> source) {
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
