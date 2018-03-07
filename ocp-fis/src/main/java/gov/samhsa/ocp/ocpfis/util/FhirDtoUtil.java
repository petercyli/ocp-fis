package gov.samhsa.ocp.ocpfis.util;

import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Task;

import java.util.List;
import java.util.Optional;

public class FhirDtoUtil {


    public static String getIdFromReferenceDto(ReferenceDto dto, ResourceType resourceType) {
        return dto.getReference().replace(resourceType + "/", "");
    }

    public static ReferenceDto mapActivityDefinitionToReferenceDto(ActivityDefinition activityDefintion) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference(ResourceType.ActivityDefinition + "/" + activityDefintion.getIdElement().getIdPart());
        referenceDto.setDisplay(activityDefintion.getName());
        return referenceDto;
    }

    public static ReferenceDto mapPractitionerDtoToReferenceDto(PractitionerDto practitionerDto) {
        ReferenceDto referenceDto = new ReferenceDto();

        referenceDto.setReference(ResourceType.Practitioner + "/" + practitionerDto.getLogicalId());
        List<NameDto> names = practitionerDto.getName();
        names.stream().findFirst().ifPresent(it -> {
            String name = it.getFirstName() + " " + it.getLastName();
            referenceDto.setDisplay(name);
        });

        return referenceDto;

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
                    //    participant.setParticipationStatusCode(member.getStatus().toCode());
                    //    participant.setParticipantRequiredCode(member.getRequired().toCode());
                    //    participant.setParticipationStatusCode(member.getStatus().toCode());
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
    public static ReferenceDto mapOrganizationToReferenceDto(Organization organization) {
        ReferenceDto referenceDto = new ReferenceDto();

        referenceDto.setReference(ResourceType.Organization + "/" + organization.getIdElement().getIdPart());
        referenceDto.setDisplay(organization.getName());

        return referenceDto;
    }

    public static Reference mapReferenceDtoToReference(ReferenceDto referenceDto) {
        Reference reference = new Reference();
        reference.setDisplay(referenceDto.getDisplay());
        reference.setReference(referenceDto.getReference());
        return reference;
    }

    public static ReferenceDto convertReferenceToReferenceDto(Reference reference) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setDisplay(reference.getDisplay());
        referenceDto.setReference(reference.getReference());
        return referenceDto;
    }


    public static ReferenceDto mapTaskToReferenceDto(Task task) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setReference(ResourceType.Task + "/" + task.getIdElement().getIdPart());
        referenceDto.setDisplay(task.getDescription() != null ? task.getDescription() : referenceDto.getReference());
        return referenceDto;
    }


    public static ValueSetDto convertCodeableConceptToValueSetDto(CodeableConcept source) {
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

    public static CodeableConcept convertValuesetDtoToCodeableConcept (ValueSetDto valueSetDto) {
            CodeableConcept codeableConcept = new CodeableConcept();
            if (valueSetDto != null) {
                Coding coding = FhirUtil.getCoding(valueSetDto.getCode(),valueSetDto.getDisplay(),valueSetDto.getSystem());
                codeableConcept.addCoding(coding);
            }
            return codeableConcept;
    }

    public static Optional<String> getDisplayForCode(String code, Optional<List<ValueSetDto>> lookupValueSets) {
        Optional<String> lookupDisplay = Optional.empty();
        if (lookupValueSets.isPresent()) {
            lookupDisplay = lookupValueSets.get().stream()
                    .filter(lookupValue -> code.equalsIgnoreCase(lookupValue.getCode()))
                    .map(ValueSetDto::getDisplay).findFirst();

        }
        return lookupDisplay;
    }


}
