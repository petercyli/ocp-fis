package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.PreconditionFailedException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public final class AppointmentDtoToAppointmentConverter {
    private static final String ACCEPTED_PARTICIPATION_STATUS = "accepted";
    private static final String NEEDS_ACTION_PARTICIPATION_STATUS = "needs-action";
    private static final String PROPOSED_APPOINTMENT_STATUS = "proposed";
    private static final String REQUIRED = "required";
    private static final String INFORMATION_ONLY = "information-only";

    public static Appointment map(AppointmentDto appointmentDto, boolean isCreate, Optional<String> logicalId) {
        try {
            Appointment appointment = new Appointment();

            //id
            if (isStringNotNullAndNotEmpty(appointmentDto.getLogicalId())) {
                appointment.setId(appointmentDto.getLogicalId().trim());
            } else logicalId.ifPresent(s -> appointment.setId(s.trim()));

            //Status
            if (isCreate) {
                appointment.setStatus(Appointment.AppointmentStatus.fromCode(PROPOSED_APPOINTMENT_STATUS));
            } else if (isStringNotNullAndNotEmpty(appointmentDto.getStatusCode())) {
                Appointment.AppointmentStatus status = Appointment.AppointmentStatus.fromCode(appointmentDto.getStatusCode().trim());
                appointment.setStatus(status);
            }

            //Type
            if (isStringNotNullAndNotEmpty(appointmentDto.getTypeCode())) {
                CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding(appointmentDto.getTypeCode(), null, null));
                appointment.setAppointmentType(codeableConcept);
            }

            //Desc
            if (isStringNotNullAndNotEmpty(appointmentDto.getDescription())) {
                appointment.setDescription(appointmentDto.getDescription().trim());
            }

            //Start and End Dates
            appointment.setStart(DateUtil.convertLocalDateTimeToUTCDate(appointmentDto.getStart()));
            appointment.setEnd(DateUtil.convertLocalDateTimeToUTCDate(appointmentDto.getEnd()));

            //Caution: DO NOT set created time here
            //TBD: Slot and incoming reference

            //Participants
            if (appointmentDto.getParticipant() == null || appointmentDto.getParticipant().isEmpty()) {
                throw new PreconditionFailedException("An appointment cannot be without its participant(s).");
            } else {
                List<Appointment.AppointmentParticipantComponent> participantList = new ArrayList<>();
                for (AppointmentParticipantDto participant : appointmentDto.getParticipant()) {
                    Appointment.AppointmentParticipantComponent participantModel = new Appointment.AppointmentParticipantComponent();

                    //Participation Type
                    if (isStringNotNullAndNotEmpty(participant.getParticipationTypeCode())) {
                        CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding(participant.getParticipationTypeCode(), participant.getParticipationTypeDisplay(), participant.getParticipationTypeSystem()));
                        participantModel.setType(Collections.singletonList(codeableConcept));
                    } else if (isCreate) {
                        //By default, add participants as "attender"
                        CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding("ATND", "attender", "http://hl7.org/fhir/v3/ParticipationType"));
                        participantModel.setType(Collections.singletonList(codeableConcept));
                    }

                    //Participation Status
                    if(isCreate){
                        participantModel.setStatus(Appointment.ParticipationStatus.fromCode(NEEDS_ACTION_PARTICIPATION_STATUS));
                    } else if (isStringNotNullAndNotEmpty(participant.getParticipationStatusCode())) {
                        Appointment.ParticipationStatus status = Appointment.ParticipationStatus.fromCode(participant.getParticipationStatusCode().trim());
                        participantModel.setStatus(status);
                    } else {
                        participantModel.setStatus(Appointment.ParticipationStatus.fromCode(NEEDS_ACTION_PARTICIPATION_STATUS));
                    }

                    //Actor
                    if (isStringNotNullAndNotEmpty(participant.getActorReference())) {
                        Reference ref = new Reference(participant.getActorReference().trim());

                        if (isStringNotNullAndNotEmpty(participant.getActorName())) {
                            ref.setDisplay(participant.getActorName().trim());
                        }
                        participantModel.setActor(ref);

                        //Participant Required
                        if (isStringNotNullAndNotEmpty(participant.getParticipantRequiredCode())) {
                            Appointment.ParticipantRequired required = Appointment.ParticipantRequired.fromCode(participant.getParticipantRequiredCode().trim());
                            participantModel.setRequired(required);
                        } else {
                            participantModel.setRequired(getRequiredBasedOnParticipantReference(participant.getActorReference()));
                        }
                    }
                    participantList.add(participantModel);
                }

                //Add creator reference during appointment creation
                if (isCreate && isStringNotNullAndNotEmpty(appointmentDto.getCreatorReference())) {
                    Appointment.AppointmentParticipantComponent creatorParticipantModel = new Appointment.AppointmentParticipantComponent();

                    //Participation Type
                    CodeableConcept codeableConcept = new CodeableConcept().addCoding(FhirUtil.getCoding("AUT", "author (originator)", "http://hl7.org/fhir/v3/ParticipationType"));
                    creatorParticipantModel.setType(Collections.singletonList(codeableConcept));

                    //Participation Status
                    creatorParticipantModel.setStatus(Appointment.ParticipationStatus.fromCode(ACCEPTED_PARTICIPATION_STATUS));

                    //Participant Required
                    creatorParticipantModel.setRequired(getRequiredBasedOnParticipantReference(appointmentDto.getCreatorReference()));

                    //Actor
                    Reference creatorRef = new Reference(appointmentDto.getCreatorReference().trim());
                    if (isStringNotNullAndNotEmpty(appointmentDto.getCreatorName())) {
                        creatorRef.setDisplay(appointmentDto.getCreatorName().trim());
                    }
                    creatorParticipantModel.setActor(creatorRef);

                    participantList.add(creatorParticipantModel);
                }

                appointment.setParticipant(participantList);
            }
            return appointment;
        }
        catch (FHIRException e) {
            log.error("Unable to convert from the given code to valueSet");
            throw new BadRequestException("Invalid values in the request Dto ", e);
        }
    }

    private static boolean isStringNotNullAndNotEmpty(String givenString) {
        return givenString != null && !givenString.trim().isEmpty();
    }

    private static Appointment.ParticipantRequired getRequiredBasedOnParticipantReference(String reference) throws FHIRException {
        String resourceType = reference.trim().split("/")[0];
        List<String> requiredResources = Arrays.asList(ResourceType.Practitioner.name().toUpperCase(), ResourceType.Patient.name().toUpperCase(), ResourceType.RelatedPerson.name().toUpperCase());
        List<String> infoOnlyResources = Arrays.asList(ResourceType.HealthcareService.name().toUpperCase(), ResourceType.Location.name().toUpperCase());

        if (requiredResources.contains(resourceType.toUpperCase())) {
            return Appointment.ParticipantRequired.fromCode(REQUIRED);
        } else if (infoOnlyResources.contains(resourceType.toUpperCase())) {
            return Appointment.ParticipantRequired.fromCode(INFORMATION_ONLY);
        }
        return null;
    }

}
